package sudoku;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Observable;
import java.util.Random;
import java.util.Set;

/**
 * Concrete Model in the MVC design.
 * Owns all Sudoku state: original clues, current board, solved board, undo
 * snapshot and runtime flags. GUI and CLI call it through ISudokuModel, and the
 * Swing View observes it through Observable. No button handling or console
 * parsing is stored here.
 */
/*@
 @ invariant puzzles != null && puzzles.size() > 0;
 @ invariant initialPuzzle != null && board != null && solution != null;
 @ invariant board.length == SIZE && initialPuzzle.length == SIZE && solution.length == SIZE;
 @ invariant (\forall int row; 0 <= row && row < SIZE;
 @              board[row] != null && board[row].length == SIZE
 @           && initialPuzzle[row] != null && initialPuzzle[row].length == SIZE
 @           && solution[row] != null && solution[row].length == SIZE);
 @ invariant (\forall int row, column;
 @              0 <= row && row < SIZE && 0 <= column && column < SIZE;
 @              0 <= board[row][column] && board[row][column] <= 9
 @           && 0 <= initialPuzzle[row][column] && initialPuzzle[row][column] <= 9
 @           && 1 <= solution[row][column] && solution[row][column] <= 9);
 @ invariant (\forall int row, column;
 @              0 <= row && row < SIZE && 0 <= column && column < SIZE;
 @              initialPuzzle[row][column] != 0 ==> board[row][column] == initialPuzzle[row][column]);
 @*/
@SuppressWarnings("deprecation")
public class SudokuModel extends Observable implements ISudokuModel {
    private static final String PUZZLE_RESOURCE = "/puzzles.txt";

    private final List<int[][]> puzzles;
    private final Random random;

    private int[][] initialPuzzle;
    private int[][] solution;
    private int[][] board;

    private int[][] undoSnapshot;

    private boolean validationFeedbackEnabled = true;
    private boolean hintEnabled = true;
    private boolean randomPuzzleSelectionEnabled = true;

    /**
     * Builds a ready-to-use model by loading puzzle data and selecting the
     * first active puzzle. The constructor performs all setup needed by both
     * entry points, so the GUI and CLI can start with a valid board immediately.
     */
    /*@
     @ ensures invariant();
     @ ensures validationFeedbackEnabled && hintEnabled && randomPuzzleSelectionEnabled;
     @ ensures !canUndo();
     @*/
    public SudokuModel() {
        this.random = new Random();
        this.puzzles = loadPuzzles();
        loadPuzzle(selectPuzzleIndex());
        assert invariant();
    }

    /**
     * Reads one current board value from the Model.
     * The method does not expose the board array itself; this protects the MVC
     * boundary because Views can display values without being able to mutate the
     * board behind the Model's validation checks.
     */
    /*@
     @ requires 0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @ ensures \result == board[row][column];
     @*/
    public int getCellValue(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        int value = board[row][column];
        assert invariant();
        return value;
    }

    /**
     * Decides whether the player may edit a cell by comparing against the
     * original puzzle. A non-zero clue in initialPuzzle is fixed forever, even
     * after reset, undo, or attempted writes.
     */
    /*@
     @ requires 0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @ ensures \result <==> initialPuzzle[row][column] == 0;
     @*/
    public boolean isEditableCell(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        boolean editable = initialPuzzle[row][column] == 0;
        assert invariant();
        return editable;
    }

    /**
     * Applies a user-entered digit to the current board.
     * This method deliberately accepts values that may conflict with Sudoku
     * rules because validation feedback is shown separately; the only hard
     * constraints here are board bounds, editability, value range and avoiding a
     * duplicate no-op write.
     */
    /*@
     @ requires 0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @ requires 1 <= value && value <= 9;
     @ ensures \result ==> board[row][column] == value;
     @ ensures !\result ==> board[row][column] == \old(board[row][column]);
     @ ensures (\forall int r, c;
     @              0 <= r && r < SIZE && 0 <= c && c < SIZE
     @           && !(r == row && c == column);
     @              board[r][c] == \old(board[r][c]));
     @*/
    public boolean setCellValue(int row, int column, int value) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        assert value >= 1 && value <= 9 : "1 <= value <= 9";
        if (!isEditableCell(row, column) || board[row][column] == value) {
            assert invariant();
            return false;
        }
        saveUndoSnapshot();
        board[row][column] = value;
        publishChange();
        assert board[row][column] == value : "successful setCellValue must write the requested value";
        assert canUndo() : "successful setCellValue must store one undo snapshot";
        assert invariant();
        return true;
    }

    /**
     * Clears a player-editable cell back to empty.
     * Clearing follows the same undo pattern as entering a number: save the
     * previous board, change one cell, then notify observers.
     */
    /*@
     @ requires 0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @ ensures \result ==> board[row][column] == 0;
     @ ensures !\result ==> board[row][column] == \old(board[row][column]);
     @ ensures (\forall int r, c;
     @              0 <= r && r < SIZE && 0 <= c && c < SIZE
     @           && !(r == row && c == column);
     @              board[r][c] == \old(board[r][c]));
     @*/
    public boolean clearCell(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        if (!isEditableCell(row, column) || board[row][column] == 0) {
            assert invariant();
            return false;
        }
        saveUndoSnapshot();
        board[row][column] = 0;
        publishChange();
        assert board[row][column] == 0 : "successful clearCell must clear the target cell";
        assert canUndo() : "successful clearCell must store one undo snapshot";
        assert invariant();
        return true;
    }

    /**
     * Exposes whether the single undo slot currently contains a saved board.
     * The Controller uses this to enable or disable the Undo button, and tests
     * use it to check that successful edits create undo history.
     */
    /*@
     @ requires true;
     @ ensures \result <==> undoSnapshot != null;
     @*/
    public boolean canUndo() {
        assert invariant();
        boolean result = undoSnapshot != null;
        assert invariant();
        return result;
    }

    /**
     * Restores the one saved board snapshot.
     * The snapshot is cleared before publishing the change so that any observer
     * refresh sees the new board and the correct disabled undo state together.
     */
    /*@
     @ requires true;
     @ ensures !\result ==> (\forall int row, column;
     @              0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @              board[row][column] == \old(board[row][column]));
     @ ensures \result <==> \old(undoSnapshot != null);
     @ ensures \result ==> undoSnapshot == null;
     @*/
    public boolean undo() {
        assert invariant();
        if (undoSnapshot == null) {
            assert invariant();
            return false;
        }
        int[][] snapshot = undoSnapshot;
        undoSnapshot = null;
        board = snapshot;
        publishChange();
        assert !canUndo() : "successful single-level undo must consume the snapshot";
        assert invariant();
        return true;
    }

    /**
     * Checks whether the selected cell is eligible for a hint.
     * Hints are limited to empty editable cells, which prevents overwriting
     * player choices or fixed clues and keeps hint behaviour predictable.
     */
    /*@
     @ requires 0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @ ensures \result <==> (hintEnabled && isEditableCell(row, column) && board[row][column] == 0);
     @*/
    public boolean canHintAt(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        boolean result = hintEnabled && isEditableCell(row, column) && board[row][column] == 0;
        assert invariant();
        return result;
    }

    /**
     * Writes the solved value into one empty editable cell.
     * This uses the internally solved board rather than recalculating a hint on
     * every request, so the value is consistent with completion checking.
     */
    /*@
     @ requires 0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @ ensures \result ==> board[row][column] == solution[row][column];
     @ ensures !\result ==> board[row][column] == \old(board[row][column]);
     @ ensures (\forall int r, c;
     @              0 <= r && r < SIZE && 0 <= c && c < SIZE
     @           && !(r == row && c == column);
     @              board[r][c] == \old(board[r][c]));
     @*/
    public boolean applyHint(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        if (!canHintAt(row, column)) {
            assert invariant();
            return false;
        }
        saveUndoSnapshot();
        board[row][column] = solution[row][column];
        publishChange();
        assert board[row][column] == solution[row][column] : "successful applyHint must write the solution value";
        assert canUndo() : "successful applyHint must store one undo snapshot";
        assert invariant();
        return true;
    }

    /**
     * Restores the board to the original clues for the current puzzle.
     * The method returns early if the board is already at the starting state,
     * avoiding a misleading undo snapshot that would undo to the same board.
     */
    /*@
     @ requires true;
     @ ensures (\forall int r, c;
     @              0 <= r && r < SIZE && 0 <= c && c < SIZE;
     @              board[r][c] == initialPuzzle[r][c]);
     @*/
    public void resetPuzzle() {
        assert invariant();
        if (Arrays.deepEquals(board, initialPuzzle)) {
            assert invariant();
            return;
        }
        saveUndoSnapshot();
        board = copyOf(initialPuzzle);
        publishChange();
        assert Arrays.deepEquals(board, initialPuzzle) : "resetPuzzle must restore the original puzzle state";
        assert canUndo() : "successful resetPuzzle must store one undo snapshot";
        assert invariant();
    }

    /**
     * Loads another puzzle and resets all board-specific state.
     * Runtime flags remain unchanged because they are user preferences, but the
     * undo snapshot is discarded because it belongs to the previous puzzle.
     */
    /*@
     @ requires true;
     @ ensures !canUndo();
     @*/
    public void newGame() {
        assert invariant();
        loadPuzzle(selectPuzzleIndex());
        publishChange();
        assert Arrays.deepEquals(board, initialPuzzle) : "newGame must load the puzzle clues into the board";
        assert !canUndo() : "newGame must discard undo history";
        assert invariant();
    }

    /**
     * Checks completion by comparing every cell with the solved grid.
     * This avoids treating a full but incorrect board as complete; validation
     * feedback and completion are related but intentionally separate checks.
     */
    /*@
     @ requires true;
     @ ensures \result <==> (\forall int row, column;
     @              0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @              board[row][column] == solution[row][column]);
     @*/
    public boolean isSolved() {
        assert invariant();
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (board[row][column] != solution[row][column]) {
                    assert invariant();
                    return false;
                }
            }
        }
        assert invariant();
        return true;
    }

    /**
     * Collects every currently duplicated cell.
     * The method scans rows, columns and boxes independently because one cell
     * can be invalid for more than one reason; a set avoids duplicate entries.
     */
    /*@
     @ requires true;
     @ ensures \result != null;
     @*/
    public Set<ISudokuModel.CellPosition> getInvalidCells() {
        assert invariant();
        Set<ISudokuModel.CellPosition> invalidCells = new LinkedHashSet<>();
        markInvalidRows(invalidCells);
        markInvalidColumns(invalidCells);
        markInvalidBoxes(invalidCells);
        assert invariant();
        return Collections.unmodifiableSet(invalidCells);
    }

    /**
     * Returns the current validation feedback preference.
     */
    /*@
     @ ensures \result == validationFeedbackEnabled;
     @*/
    public boolean isValidationFeedbackEnabled() {
        return validationFeedbackEnabled;
    }

    /**
     * Stores the validation feedback preference and publishes it immediately.
     * The board itself does not change, but the View still needs to redraw
     * because invalid-cell highlighting may appear or disappear.
     */
    /*@
     @ ensures validationFeedbackEnabled == enabled;
     @*/
    public void setValidationFeedbackEnabled(boolean enabled) {
        validationFeedbackEnabled = enabled;
        publishChange();
    }

    /**
     * Returns the current hint preference.
     */
    /*@
     @ ensures \result == hintEnabled;
     @*/
    public boolean isHintEnabled() {
        return hintEnabled;
    }

    /**
     * Stores the hint preference and notifies observers so hint controls can
     * update as soon as the checkbox changes.
     */
    /*@
     @ ensures hintEnabled == enabled;
     @*/
    public void setHintEnabled(boolean enabled) {
        hintEnabled = enabled;
        publishChange();
    }

    /**
     * Returns the new-game puzzle selection preference.
     */
    /*@
     @ ensures \result == randomPuzzleSelectionEnabled;
     @*/
    public boolean isRandomPuzzleSelectionEnabled() {
        return randomPuzzleSelectionEnabled;
    }

    /**
     * Stores whether future new games choose a random puzzle or the first one.
     * Tests turn this off to make their expected coordinates stable.
     */
    /*@
     @ ensures randomPuzzleSelectionEnabled == enabled;
     @*/
    public void setRandomPuzzleSelectionEnabled(boolean enabled) {
        randomPuzzleSelectionEnabled = enabled;
        publishChange();
    }

    /**
     * Sends a change notification to registered observers.
     * Only this helper touches Observable's setChanged/notifyObservers calls, so
     * every successful state change follows one consistent notification path.
     */
    private void publishChange() {
        setChanged();
        notifyObservers();
    }

    /**
     * Chooses which puzzle to load for a new game.
     * A deterministic first puzzle is useful for testing, while random selection
     * gives the GUI and CLI normal replay variety.
     */
    private int selectPuzzleIndex() {
        if (!randomPuzzleSelectionEnabled) {
            return 0;
        }
        return random.nextInt(puzzles.size());
    }

    /**
     * Copies one puzzle into active model state and solves it for reference.
     * The Model keeps three separate boards: original clues, current player
     * board, and solved board. Keeping them separate makes reset, hints and
     * completion checks simple and avoids accidental mutation of puzzle data.
     */
    private void loadPuzzle(int puzzleIndex) {
        initialPuzzle = copyOf(puzzles.get(puzzleIndex));
        solution = solvePuzzle(copyOf(initialPuzzle));
        if (!isValidSolvedGrid(solution)) {
            throw new IllegalStateException("Solution grid is not a valid Sudoku solution");
        }
        board = copyOf(initialPuzzle);
        undoSnapshot = null;
    }

    /**
     * Saves the current board before a successful mutating operation.
     * A deep copy is required because int[][] is mutable; assigning the array
     * reference directly would make undo point at the changing live board.
     */
    private void saveUndoSnapshot() {
        undoSnapshot = copyOf(board);
    }

    /**
     * Reads puzzle definitions from the packaged resource file.
     * Keeping file loading in the Model satisfies the coursework requirement and
     * keeps both GUI and CLI backed by the same puzzle source.
     */
    private List<int[][]> loadPuzzles() {
        List<int[][]> loadedPuzzles = new ArrayList<>();
        try (InputStream inputStream = SudokuModel.class.getResourceAsStream(PUZZLE_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing resource: " + PUZZLE_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                        continue;
                    }
                    loadedPuzzles.add(parseGrid(trimmedLine));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read puzzles", exception);
        }
        if (loadedPuzzles.isEmpty()) {
            throw new IllegalStateException("At least one puzzle is required");
        }
        return loadedPuzzles;
    }

    /**
     * Converts one 81-character puzzle line into a 9 by 9 grid.
     * Digits 1 to 9 are starting clues and 0 represents an empty editable cell.
     */
    private int[][] parseGrid(String text) {
        if (text.length() != SIZE * SIZE) {
            throw new IllegalStateException("Each grid must have 81 digits");
        }
        int[][] grid = new int[SIZE][SIZE];
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (!Character.isDigit(character)) {
                throw new IllegalStateException("Grid contains a non-digit character");
            }
            int value = character - '0';
            grid[index / SIZE][index % SIZE] = value;
        }
        return grid;
    }

    /**
     * Produces a solved board for one puzzle using backtracking.
     * The solver is private because the rest of the application only needs the
     * final solution for hints and completion, not the solving process itself.
     */
    private int[][] solvePuzzle(int[][] puzzle) {
        if (solveCell(puzzle, 0, 0)) {
            return puzzle;
        }
        throw new IllegalStateException("Puzzle has no valid solution");
    }

    /**
     * Recursive backtracking step for the solver.
     * It scans cells from left to right and top to bottom, skips original clues,
     * tries values 1 to 9 in empty cells, and backtracks when a later cell cannot
     * be solved.
     */
    private boolean solveCell(int[][] grid, int row, int column) {
        if (row == SIZE) {
            return true;
        }

        int nextRow = column == SIZE - 1 ? row + 1 : row;
        int nextColumn = column == SIZE - 1 ? 0 : column + 1;

        if (grid[row][column] != 0) {
            return solveCell(grid, nextRow, nextColumn);
        }

        for (int value = 1; value <= 9; value++) {
            if (isValidPlacement(grid, row, column, value)) {
                grid[row][column] = value;
                if (solveCell(grid, nextRow, nextColumn)) {
                    return true;
                }
                grid[row][column] = 0;
            }
        }
        return false;
    }

    /**
     * Checks whether a trial value can be placed during solving.
     * This is stricter than user input: the solver must only write values that
     * keep rows, columns and the 3 by 3 box valid.
     */
    private boolean isValidPlacement(int[][] grid, int row, int column, int value) {
        for (int index = 0; index < SIZE; index++) {
            if (grid[row][index] == value || grid[index][column] == value) {
                return false;
            }
        }
        int boxRow = (row / 3) * 3;
        int boxColumn = (column / 3) * 3;
        for (int currentRow = boxRow; currentRow < boxRow + 3; currentRow++) {
            for (int currentColumn = boxColumn; currentColumn < boxColumn + 3; currentColumn++) {
                if (grid[currentRow][currentColumn] == value) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Verifies that a solved grid really satisfies Sudoku rules.
     * This protects against a bad puzzle file or solver mistake before hints and
     * completion checks depend on the generated solution.
     */
    private boolean isValidSolvedGrid(int[][] grid) {
        for (int index = 0; index < SIZE; index++) {
            if (!isCompleteLine(grid, index, 0, 0, 1)
                    || !isCompleteLine(grid, 0, index, 1, 0)) {
                return false;
            }
        }
        for (int boxRow = 0; boxRow < SIZE; boxRow += 3) {
            for (int boxColumn = 0; boxColumn < SIZE; boxColumn += 3) {
                if (!isCompleteBox(grid, boxRow, boxColumn)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks one completed row or column.
     * The starting cell and step values let the same code validate both
     * horizontal and vertical lines without duplication.
     */
    private boolean isCompleteLine(int[][] grid, int startRow, int startColumn, int rowStep, int columnStep) {
        boolean[] seen = new boolean[SIZE + 1];
        for (int offset = 0; offset < SIZE; offset++) {
            int value = grid[startRow + offset * rowStep][startColumn + offset * columnStep];
            if (value < 1 || value > 9 || seen[value]) {
                return false;
            }
            seen[value] = true;
        }
        return true;
    }

    /**
     * Checks one completed 3 by 3 box.
     * Each value must be 1 to 9 and appear exactly once for the box to pass.
     */
    private boolean isCompleteBox(int[][] grid, int boxRow, int boxColumn) {
        boolean[] seen = new boolean[SIZE + 1];
        for (int row = boxRow; row < boxRow + 3; row++) {
            for (int column = boxColumn; column < boxColumn + 3; column++) {
                int value = grid[row][column];
                if (value < 1 || value > 9 || seen[value]) {
                    return false;
                }
                seen[value] = true;
            }
        }
        return true;
    }

    /**
     * Adds row conflicts to the invalid-cell set.
     */
    private void markInvalidRows(Set<ISudokuModel.CellPosition> invalidCells) {
        for (int row = 0; row < SIZE; row++) {
            markInvalidLine(invalidCells, row, 0, 0, 1);
        }
    }

    /**
     * Adds column conflicts to the invalid-cell set.
     */
    private void markInvalidColumns(Set<ISudokuModel.CellPosition> invalidCells) {
        for (int column = 0; column < SIZE; column++) {
            markInvalidLine(invalidCells, 0, column, 1, 0);
        }
    }

    /**
     * Marks every duplicate value in one row or column.
     * Counting first is important: when a value appears three times, all three
     * cells should be highlighted, not only the second and third occurrences.
     */
    private void markInvalidLine(
            Set<ISudokuModel.CellPosition> invalidCells,
            int startRow,
            int startColumn,
            int rowStep,
            int columnStep
    ) {
        int[] counts = new int[SIZE + 1];
        for (int offset = 0; offset < SIZE; offset++) {
            int row = startRow + offset * rowStep;
            int column = startColumn + offset * columnStep;
            int value = board[row][column];
            if (value != 0) {
                counts[value]++;
            }
        }
        for (int offset = 0; offset < SIZE; offset++) {
            int row = startRow + offset * rowStep;
            int column = startColumn + offset * columnStep;
            int value = board[row][column];
            if (value != 0 && counts[value] > 1) {
                invalidCells.add(new ISudokuModel.CellPosition(row, column));
            }
        }
    }

    /**
     * Marks every duplicate value inside each 3 by 3 box.
     * The logic mirrors markInvalidLine but each box has its own local count
     * array because duplicate checks do not cross box boundaries.
     */
    private void markInvalidBoxes(Set<ISudokuModel.CellPosition> invalidCells) {
        for (int boxRow = 0; boxRow < SIZE; boxRow += 3) {
            for (int boxColumn = 0; boxColumn < SIZE; boxColumn += 3) {
                int[] counts = new int[SIZE + 1];
                for (int row = boxRow; row < boxRow + 3; row++) {
                    for (int column = boxColumn; column < boxColumn + 3; column++) {
                        int value = board[row][column];
                        if (value != 0) {
                            counts[value]++;
                        }
                    }
                }
                for (int row = boxRow; row < boxRow + 3; row++) {
                    for (int column = boxColumn; column < boxColumn + 3; column++) {
                        int value = board[row][column];
                        if (value != 0 && counts[value] > 1) {
                            invalidCells.add(new ISudokuModel.CellPosition(row, column));
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a deep copy of a Sudoku grid.
     * This helper protects snapshots and puzzle templates from being changed
     * accidentally through another reference to the same row arrays.
     */
    private int[][] copyOf(int[][] source) {
        int[][] copy = new int[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            System.arraycopy(source[row], 0, copy[row], 0, SIZE);
        }
        return copy;
    }

    /**
     * Centralises row and column bounds checks.
     */
    private boolean isInBounds(int row, int column) {
        return row >= 0 && row < SIZE && column >= 0 && column < SIZE;
    }

    /**
     * Runtime invariant used by assertions to document core Model assumptions.
     * It checks that all boards exist, have the correct shape, contain legal
     * values, and preserve fixed clues. These checks make the data ownership
     * rules explicit for the coursework report and for debugging.
     */
    private boolean invariant() {
        if (puzzles == null || puzzles.isEmpty()) {
            return false;
        }
        if (board == null || initialPuzzle == null || solution == null) {
            return false;
        }
        if (board.length != SIZE || initialPuzzle.length != SIZE || solution.length != SIZE) {
            return false;
        }
        for (int row = 0; row < SIZE; row++) {
            if (board[row] == null || initialPuzzle[row] == null || solution[row] == null) {
                return false;
            }
            if (board[row].length != SIZE || initialPuzzle[row].length != SIZE
                    || solution[row].length != SIZE) {
                return false;
            }
            for (int column = 0; column < SIZE; column++) {
                int value = board[row][column];
                int clue = initialPuzzle[row][column];
                int solutionValue = solution[row][column];
                if (value < 0 || value > 9) {
                    return false;
                }
                if (clue < 0 || clue > 9 || solutionValue < 1 || solutionValue > 9) {
                    return false;
                }
                if (clue != 0 && board[row][column] != clue) {
                    return false;
                }
                if (clue != 0 && clue != solutionValue) {
                    return false;
                }
            }
        }
        return true;
    }
}
