package sudoku;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Observable;
import java.util.Random;
import java.util.Set;

/**
 * Model for both the GUI and CLI Sudoku programs.
 *
 * Class invariants:
 * 1. puzzles.size() > 0
 * 2. board.length = 9 and fixedCells.length = 9
 * 3. forall r,c in [0,8]: 0 <= board[r][c] <= 9
 * 4. forall r,c in [0,8]: fixedCells[r][c] <-> currentPuzzle.puzzle[r][c] != 0
 * 5. forall r,c in [0,8]: fixedCells[r][c] => board[r][c] = currentPuzzle.puzzle[r][c]
 */
@SuppressWarnings("deprecation")
public class Model extends Observable {
    public static final int SIZE = 9;
    private static final String PUZZLE_RESOURCE = "/puzzles.txt";

    private final List<Puzzle> puzzles;
    private final Random random;

    private Puzzle currentPuzzle;
    private int[][] board;
    private boolean[][] fixedCells;
    private final List<BoardSnapshot> undoSnapshots = new ArrayList<>();

    private boolean validationFeedbackEnabled = true;
    private boolean hintEnabled = true;
    private boolean randomPuzzleSelectionEnabled = true;

    public Model() {
        this(new Random());
    }

    Model(Random random) {
        assert random != null : "random != null";
        this.random = random;
        this.puzzles = loadPuzzles();
        loadPuzzle(selectPuzzleIndex());
        assert invariant();
    }

    /**
     * Pre: true
     * Post: result = board[row][column]
     */
    public int getCellValue(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        int value = board[row][column];
        assert invariant();
        return value;
    }

    /**
     * Pre: true
     * Post: result <-> currentPuzzle.puzzle[row][column] = 0
     */
    public boolean isEditableCell(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        boolean editable = !fixedCells[row][column];
        assert invariant();
        return editable;
    }

    /**
     * Pre: true
     * Post: result <-> currentPuzzle.puzzle[row][column] != 0
     */
    public boolean isFixedCell(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        boolean fixed = fixedCells[row][column];
        assert invariant();
        return fixed;
    }

    /**
     * Pre: true
     * Post: result <-> board[row][column] = 0
     */
    public boolean isCellEmpty(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        boolean empty = board[row][column] == 0;
        assert invariant();
        return empty;
    }

    /**
     * Pre: 0 <= row,column < 9 and 1 <= value <= 9
     * Post:
     * - if the target cell is editable then board[row][column] = value
     * - if the target cell is fixed then the board is unchanged
     */
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
        assert invariant();
        return true;
    }

    /**
     * Pre: 0 <= row,column < 9
     * Post:
     * - if the target cell is editable then board[row][column] = 0
     * - if the target cell is fixed then the board is unchanged
     */
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
        assert invariant();
        return true;
    }

    /**
     * Pre: true
     * Post: result <-> an undo snapshot exists
     */
    public boolean canUndo() {
        assert invariant();
        boolean result = !undoSnapshots.isEmpty();
        assert invariant();
        return result;
    }

    /**
     * Pre: true
     * Post:
     * - if a snapshot exists then board = previous board state and result = true
     * - otherwise board is unchanged and result = false
     */
    public boolean undo() {
        assert invariant();
        if (undoSnapshots.isEmpty()) {
            assert invariant();
            return false;
        }
        BoardSnapshot snapshot = undoSnapshots.remove(undoSnapshots.size() - 1);
        board = copyOf(snapshot.boardState());
        publishChange();
        assert invariant();
        return true;
    }

    /**
     * Pre: 0 <= row,column < 9
     * Post:
     * result <-> hintEnabled and the target cell is editable and empty
     */
    public boolean canHintAt(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        boolean result = hintEnabled && isEditableCell(row, column) && isCellEmpty(row, column);
        assert invariant();
        return result;
    }

    /**
     * Pre: 0 <= row,column < 9
     * Post:
     * - if canHintAt(row,column) then board[row][column] = currentPuzzle.solution[row][column]
     * - otherwise the board is unchanged
     */
    public boolean applyHint(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        if (!canHintAt(row, column)) {
            assert invariant();
            return false;
        }
        saveUndoSnapshot();
        board[row][column] = currentPuzzle.solution()[row][column];
        publishChange();
        assert invariant();
        return true;
    }

    /**
     * Pre: true
     * Post: board = currentPuzzle.puzzle and completion is not forced
     */
    public void resetPuzzle() {
        assert invariant();
        if (boardsEqual(board, currentPuzzle.puzzle())) {
            assert invariant();
            return;
        }
        saveUndoSnapshot();
        board = copyOf(currentPuzzle.puzzle());
        publishChange();
        assert invariant();
    }

    /**
     * Pre: true
     * Post: a new puzzle is loaded and undo history is discarded
     */
    public void newGame() {
        assert invariant();
        loadPuzzle(selectPuzzleIndex());
        publishChange();
        assert invariant();
    }

    /**
     * Pre: true
     * Post: result <-> forall r,c in [0,8]: board[r][c] = currentPuzzle.solution[r][c]
     */
    public boolean isSolved() {
        assert invariant();
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (board[row][column] != currentPuzzle.solution()[row][column]) {
                    assert invariant();
                    return false;
                }
            }
        }
        assert invariant();
        return true;
    }

    /**
     * Pre: true
     * Post:
     * result contains exactly those occupied cells that duplicate a value in
     * a row, column, or 3x3 sub-grid
     */
    public Set<CellPosition> getInvalidCells() {
        assert invariant();
        Set<CellPosition> invalidCells = new LinkedHashSet<>();
        markInvalidRows(invalidCells);
        markInvalidColumns(invalidCells);
        markInvalidBoxes(invalidCells);
        assert invariant();
        return Collections.unmodifiableSet(invalidCells);
    }

    /**
     * Pre: true
     * Post: result = validationFeedbackEnabled
     */
    public boolean isValidationFeedbackEnabled() {
        assert invariant();
        boolean result = validationFeedbackEnabled;
        assert invariant();
        return result;
    }

    /**
     * Pre: true
     * Post: validationFeedbackEnabled = enabled
     */
    public void setValidationFeedbackEnabled(boolean enabled) {
        assert invariant();
        validationFeedbackEnabled = enabled;
        publishChange();
        assert invariant();
    }

    /**
     * Pre: true
     * Post: result = hintEnabled
     */
    public boolean isHintEnabled() {
        assert invariant();
        boolean result = hintEnabled;
        assert invariant();
        return result;
    }

    /**
     * Pre: true
     * Post: hintEnabled = enabled
     */
    public void setHintEnabled(boolean enabled) {
        assert invariant();
        hintEnabled = enabled;
        publishChange();
        assert invariant();
    }

    /**
     * Pre: true
     * Post: result = randomPuzzleSelectionEnabled
     */
    public boolean isRandomPuzzleSelectionEnabled() {
        assert invariant();
        boolean result = randomPuzzleSelectionEnabled;
        assert invariant();
        return result;
    }

    /**
     * Pre: true
     * Post: randomPuzzleSelectionEnabled = enabled
     */
    public void setRandomPuzzleSelectionEnabled(boolean enabled) {
        assert invariant();
        randomPuzzleSelectionEnabled = enabled;
        publishChange();
        assert invariant();
    }

    public static record CellPosition(int row, int column) { }

    private void publishChange() {
        setChanged();
        notifyObservers();
    }

    private int selectPuzzleIndex() {
        if (!randomPuzzleSelectionEnabled) {
            return 0;
        }
        return random.nextInt(puzzles.size());
    }

    private void loadPuzzle(int puzzleIndex) {
        currentPuzzle = puzzles.get(puzzleIndex);
        board = copyOf(currentPuzzle.puzzle());
        fixedCells = new boolean[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                fixedCells[row][column] = currentPuzzle.puzzle()[row][column] != 0;
            }
        }
        undoSnapshots.clear();
    }

    private void saveUndoSnapshot() {
        undoSnapshots.add(new BoardSnapshot(copyOf(board)));
    }

    private List<Puzzle> loadPuzzles() {
        List<Puzzle> loadedPuzzles = new ArrayList<>();
        try (InputStream inputStream = Model.class.getResourceAsStream(PUZZLE_RESOURCE)) {
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
                    loadedPuzzles.add(parsePuzzle(trimmedLine));
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

    private Puzzle parsePuzzle(String line) {
        int[][] puzzle = parseGrid(line, true);
        int[][] solution = solvePuzzle(copyOf(puzzle));
        validatePuzzleConsistency(puzzle, solution);
        return new Puzzle(puzzle, solution);
    }

    private int[][] parseGrid(String text, boolean allowZero) {
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
            if (!allowZero && value == 0) {
                throw new IllegalStateException("Solution grid must not contain zero");
            }
            grid[index / SIZE][index % SIZE] = value;
        }
        return grid;
    }

    private void validatePuzzleConsistency(int[][] puzzle, int[][] solution) {
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (puzzle[row][column] != 0 && puzzle[row][column] != solution[row][column]) {
                    throw new IllegalStateException("Puzzle clues must match the solution");
                }
            }
        }
        if (!isValidSolvedGrid(solution)) {
            throw new IllegalStateException("Solution grid is not a valid Sudoku solution");
        }
    }

    private int[][] solvePuzzle(int[][] puzzle) {
        if (solveCell(puzzle, 0, 0)) {
            return puzzle;
        }
        throw new IllegalStateException("Puzzle has no valid solution");
    }

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

    private boolean isValidPlacement(int[][] grid, int row, int column, int value) {
        return isRowValidForPlacement(grid, row, value)
                && isColumnValidForPlacement(grid, column, value)
                && isBoxValidForPlacement(grid, row, column, value);
    }

    private boolean isRowValidForPlacement(int[][] grid, int row, int value) {
        for (int column = 0; column < SIZE; column++) {
            if (grid[row][column] == value) {
                return false;
            }
        }
        return true;
    }

    private boolean isColumnValidForPlacement(int[][] grid, int column, int value) {
        for (int row = 0; row < SIZE; row++) {
            if (grid[row][column] == value) {
                return false;
            }
        }
        return true;
    }

    private boolean isBoxValidForPlacement(int[][] grid, int row, int column, int value) {
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

    private boolean isValidSolvedGrid(int[][] grid) {
        return noDuplicatesInRows(grid) && noDuplicatesInColumns(grid) && noDuplicatesInBoxes(grid);
    }

    private boolean noDuplicatesInRows(int[][] grid) {
        for (int row = 0; row < SIZE; row++) {
            boolean[] seen = new boolean[SIZE + 1];
            for (int column = 0; column < SIZE; column++) {
                int value = grid[row][column];
                if (value < 1 || value > 9 || seen[value]) {
                    return false;
                }
                seen[value] = true;
            }
        }
        return true;
    }

    private boolean noDuplicatesInColumns(int[][] grid) {
        for (int column = 0; column < SIZE; column++) {
            boolean[] seen = new boolean[SIZE + 1];
            for (int row = 0; row < SIZE; row++) {
                int value = grid[row][column];
                if (value < 1 || value > 9 || seen[value]) {
                    return false;
                }
                seen[value] = true;
            }
        }
        return true;
    }

    private boolean noDuplicatesInBoxes(int[][] grid) {
        for (int boxRow = 0; boxRow < SIZE; boxRow += 3) {
            for (int boxColumn = 0; boxColumn < SIZE; boxColumn += 3) {
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
            }
        }
        return true;
    }

    private void markInvalidRows(Set<CellPosition> invalidCells) {
        for (int row = 0; row < SIZE; row++) {
            int[] counts = new int[SIZE + 1];
            for (int column = 0; column < SIZE; column++) {
                int value = board[row][column];
                if (value != 0) {
                    counts[value]++;
                }
            }
            for (int column = 0; column < SIZE; column++) {
                int value = board[row][column];
                if (value != 0 && counts[value] > 1) {
                    invalidCells.add(new CellPosition(row, column));
                }
            }
        }
    }

    private void markInvalidColumns(Set<CellPosition> invalidCells) {
        for (int column = 0; column < SIZE; column++) {
            int[] counts = new int[SIZE + 1];
            for (int row = 0; row < SIZE; row++) {
                int value = board[row][column];
                if (value != 0) {
                    counts[value]++;
                }
            }
            for (int row = 0; row < SIZE; row++) {
                int value = board[row][column];
                if (value != 0 && counts[value] > 1) {
                    invalidCells.add(new CellPosition(row, column));
                }
            }
        }
    }

    private void markInvalidBoxes(Set<CellPosition> invalidCells) {
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
                            invalidCells.add(new CellPosition(row, column));
                        }
                    }
                }
            }
        }
    }

    private boolean boardsEqual(int[][] left, int[][] right) {
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (left[row][column] != right[row][column]) {
                    return false;
                }
            }
        }
        return true;
    }

    private int[][] copyOf(int[][] source) {
        int[][] copy = new int[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            System.arraycopy(source[row], 0, copy[row], 0, SIZE);
        }
        return copy;
    }

    private boolean isInBounds(int row, int column) {
        return row >= 0 && row < SIZE && column >= 0 && column < SIZE;
    }

    private boolean invariant() {
        if (puzzles == null || puzzles.isEmpty()) {
            return false;
        }
        if (board == null || fixedCells == null || currentPuzzle == null) {
            return false;
        }
        if (board.length != SIZE || fixedCells.length != SIZE || currentPuzzle.puzzle().length != SIZE) {
            return false;
        }
        for (int row = 0; row < SIZE; row++) {
            if (board[row].length != SIZE || fixedCells[row].length != SIZE || currentPuzzle.puzzle()[row].length != SIZE) {
                return false;
            }
            for (int column = 0; column < SIZE; column++) {
                int value = board[row][column];
                if (value < 0 || value > 9) {
                    return false;
                }
                boolean expectedFixed = currentPuzzle.puzzle()[row][column] != 0;
                if (fixedCells[row][column] != expectedFixed) {
                    return false;
                }
                if (fixedCells[row][column] && board[row][column] != currentPuzzle.puzzle()[row][column]) {
                    return false;
                }
            }
        }
        return true;
    }

    private record Puzzle(int[][] puzzle, int[][] solution) { }

    private record BoardSnapshot(int[][] boardState) { }
}
