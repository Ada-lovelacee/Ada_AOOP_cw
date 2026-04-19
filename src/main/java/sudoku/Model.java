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
import java.util.Observer;
import java.util.Random;
import java.util.Set;

/**
 * Model for both the GUI and CLI Sudoku programs.
 */
/*@
 @ invariant puzzles != null && puzzles.size() > 0;
 @ invariant currentPuzzle != null;
 @ invariant board != null && fixedCells != null;
 @ invariant currentPuzzle.puzzle() != null && currentPuzzle.solution() != null;
 @ invariant board.length == SIZE && fixedCells.length == SIZE;
 @ invariant currentPuzzle.puzzle().length == SIZE && currentPuzzle.solution().length == SIZE;
 @ invariant (\forall int row; 0 <= row && row < SIZE;
 @              board[row] != null && board[row].length == SIZE
 @           && fixedCells[row] != null && fixedCells[row].length == SIZE
 @           && currentPuzzle.puzzle()[row] != null && currentPuzzle.puzzle()[row].length == SIZE
 @           && currentPuzzle.solution()[row] != null && currentPuzzle.solution()[row].length == SIZE);
 @ invariant (\forall int row, column;
 @              0 <= row && row < SIZE && 0 <= column && column < SIZE;
 @              0 <= board[row][column] && board[row][column] <= 9);
 @ invariant (\forall int row, column;
 @              0 <= row && row < SIZE && 0 <= column && column < SIZE;
 @              fixedCells[row][column] <==> currentPuzzle.puzzle()[row][column] != 0);
 @ invariant (\forall int row, column;
 @              0 <= row && row < SIZE && 0 <= column && column < SIZE;
 @              fixedCells[row][column] ==> board[row][column] == currentPuzzle.puzzle()[row][column]);
 @*/
@SuppressWarnings("deprecation")
public class Model extends Observable implements SudokuModel {
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

    /*@
     @ ensures invariant();
     @ ensures validationFeedbackEnabled;
     @ ensures hintEnabled;
     @ ensures randomPuzzleSelectionEnabled;
     @ ensures !canUndo();
     @*/
    public Model() {
        this(new Random());
    }

    /*@
     @ requires random != null;
     @ ensures invariant();
     @ ensures validationFeedbackEnabled;
     @ ensures hintEnabled;
     @ ensures randomPuzzleSelectionEnabled;
     @ ensures !canUndo();
     @*/
    Model(Random random) {
        assert random != null : "random != null";
        this.random = random;
        this.puzzles = loadPuzzles();
        loadPuzzle(selectPuzzleIndex());
        assert invariant();
    }

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

    /*@
     @ requires 0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @ ensures \result <==> currentPuzzle.puzzle()[row][column] == 0;
     @*/
    public boolean isEditableCell(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        boolean editable = !fixedCells[row][column];
        assert invariant();
        return editable;
    }

    /*@
     @ requires 0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @ ensures \result <==> currentPuzzle.puzzle()[row][column] != 0;
     @*/
    public boolean isFixedCell(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        boolean fixed = fixedCells[row][column];
        assert invariant();
        return fixed;
    }

    /*@
     @ requires 0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @ ensures \result <==> board[row][column] == 0;
     @*/
    public boolean isCellEmpty(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        boolean empty = board[row][column] == 0;
        assert invariant();
        return empty;
    }

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
        int[][] oldBoard = copyOf(board);
        int oldUndoCount = undoSnapshots.size();
        if (!isEditableCell(row, column) || board[row][column] == value) {
            assert boardsEqual(board, oldBoard) : "board must be unchanged when setCellValue returns false";
            assert undoSnapshots.size() == oldUndoCount : "undo history must be unchanged when setCellValue returns false";
            assert invariant();
            return false;
        }
        saveUndoSnapshot();
        board[row][column] = value;
        publishChange();
        assert board[row][column] == value : "successful setCellValue must write the requested value";
        assert boardsEqualExceptAt(board, oldBoard, row, column) : "only the requested cell may change during setCellValue";
        assert undoSnapshots.size() == oldUndoCount + 1 : "successful setCellValue must add one undo snapshot";
        assert invariant();
        return true;
    }

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
        int[][] oldBoard = copyOf(board);
        int oldUndoCount = undoSnapshots.size();
        if (!isEditableCell(row, column) || board[row][column] == 0) {
            assert boardsEqual(board, oldBoard) : "board must be unchanged when clearCell returns false";
            assert undoSnapshots.size() == oldUndoCount : "undo history must be unchanged when clearCell returns false";
            assert invariant();
            return false;
        }
        saveUndoSnapshot();
        board[row][column] = 0;
        publishChange();
        assert board[row][column] == 0 : "successful clearCell must clear the target cell";
        assert boardsEqualExceptAt(board, oldBoard, row, column) : "only the requested cell may change during clearCell";
        assert undoSnapshots.size() == oldUndoCount + 1 : "successful clearCell must add one undo snapshot";
        assert invariant();
        return true;
    }

    /*@
     @ requires true;
     @ ensures \result <==> undoSnapshots.size() > 0;
     @*/
    public boolean canUndo() {
        assert invariant();
        boolean result = !undoSnapshots.isEmpty();
        assert invariant();
        return result;
    }

    /*@
     @ requires true;
     @ ensures !\result ==> board == \old(board);
     @ ensures \result <==> \old(undoSnapshots.size()) > 0;
     @ ensures \result ==> board equals the board stored in the most recent undo snapshot before the call;
     @*/
    public boolean undo() {
        assert invariant();
        int[][] oldBoard = copyOf(board);
        int oldUndoCount = undoSnapshots.size();
        if (undoSnapshots.isEmpty()) {
            assert boardsEqual(board, oldBoard) : "board must be unchanged when undo returns false";
            assert invariant();
            return false;
        }
        BoardSnapshot snapshot = undoSnapshots.remove(undoSnapshots.size() - 1);
        board = copyOf(snapshot.boardState());
        publishChange();
        assert boardsEqual(board, snapshot.boardState()) : "undo must restore the most recent snapshot";
        assert undoSnapshots.size() == oldUndoCount - 1 : "successful undo must consume one undo snapshot";
        assert invariant();
        return true;
    }

    /*@
     @ requires 0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @ ensures \result <==> (hintEnabled && isEditableCell(row, column) && isCellEmpty(row, column));
     @*/
    public boolean canHintAt(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        boolean result = hintEnabled && isEditableCell(row, column) && isCellEmpty(row, column);
        assert invariant();
        return result;
    }

    /*@
     @ requires 0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @ ensures \result ==> board[row][column] == currentPuzzle.solution()[row][column];
     @ ensures !\result ==> board[row][column] == \old(board[row][column]);
     @ ensures (\forall int r, c;
     @              0 <= r && r < SIZE && 0 <= c && c < SIZE
     @           && !(r == row && c == column);
     @              board[r][c] == \old(board[r][c]));
     @*/
    public boolean applyHint(int row, int column) {
        assert invariant();
        assert isInBounds(row, column) : "0 <= row,column < 9";
        int[][] oldBoard = copyOf(board);
        int oldUndoCount = undoSnapshots.size();
        if (!canHintAt(row, column)) {
            assert boardsEqual(board, oldBoard) : "board must be unchanged when applyHint returns false";
            assert undoSnapshots.size() == oldUndoCount : "undo history must be unchanged when applyHint returns false";
            assert invariant();
            return false;
        }
        saveUndoSnapshot();
        board[row][column] = currentPuzzle.solution()[row][column];
        publishChange();
        assert board[row][column] == currentPuzzle.solution()[row][column] : "successful applyHint must write the solution value";
        assert boardsEqualExceptAt(board, oldBoard, row, column) : "only the requested cell may change during applyHint";
        assert undoSnapshots.size() == oldUndoCount + 1 : "successful applyHint must add one undo snapshot";
        assert invariant();
        return true;
    }

    /*@
     @ requires true;
     @ ensures (\forall int r, c;
     @              0 <= r && r < SIZE && 0 <= c && c < SIZE;
     @              board[r][c] == currentPuzzle.puzzle()[r][c]);
     @*/
    public void resetPuzzle() {
        assert invariant();
        int[][] oldBoard = copyOf(board);
        int oldUndoCount = undoSnapshots.size();
        if (boardsEqual(board, currentPuzzle.puzzle())) {
            assert boardsEqual(board, oldBoard) : "resetPuzzle must leave an unchanged puzzle unchanged";
            assert undoSnapshots.size() == oldUndoCount : "undo history must be unchanged when resetPuzzle makes no change";
            assert invariant();
            return;
        }
        saveUndoSnapshot();
        board = copyOf(currentPuzzle.puzzle());
        publishChange();
        assert boardsEqual(board, currentPuzzle.puzzle()) : "resetPuzzle must restore the original puzzle state";
        assert undoSnapshots.size() == oldUndoCount + 1 : "successful resetPuzzle must add one undo snapshot";
        assert invariant();
    }

    /*@
     @ requires true;
     @ ensures boardsEqual(board, currentPuzzle.puzzle());
     @ ensures !canUndo();
     @*/
    public void newGame() {
        assert invariant();
        boolean oldValidationFeedbackEnabled = validationFeedbackEnabled;
        boolean oldHintEnabled = hintEnabled;
        boolean oldRandomPuzzleSelectionEnabled = randomPuzzleSelectionEnabled;
        loadPuzzle(selectPuzzleIndex());
        publishChange();
        assert boardsEqual(board, currentPuzzle.puzzle()) : "newGame must load the puzzle clues into the board";
        assert !canUndo() : "newGame must discard undo history";
        assert validationFeedbackEnabled == oldValidationFeedbackEnabled : "newGame must not change validation feedback flag";
        assert hintEnabled == oldHintEnabled : "newGame must not change hint flag";
        assert randomPuzzleSelectionEnabled == oldRandomPuzzleSelectionEnabled : "newGame must not change puzzle selection flag";
        assert invariant();
    }

    /*@
     @ requires true;
     @ ensures \result <==> (\forall int row, column;
     @              0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @              board[row][column] == currentPuzzle.solution()[row][column]);
     @*/
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

    /*@
     @ requires true;
     @ ensures (\forall int row, column;
     @              0 <= row && row < SIZE && 0 <= column && column < SIZE;
     @              \result.contains(new SudokuModel.CellPosition(row, column))
     @              <==> isConflictingOccupiedCell(row, column));
     @*/
    public Set<SudokuModel.CellPosition> getInvalidCells() {
        assert invariant();
        Set<SudokuModel.CellPosition> invalidCells = new LinkedHashSet<>();
        markInvalidRows(invalidCells);
        markInvalidColumns(invalidCells);
        markInvalidBoxes(invalidCells);
        assert invalidCellSetMatchesBoard(invalidCells) : "getInvalidCells must return exactly the conflicting occupied cells";
        assert invariant();
        return Collections.unmodifiableSet(invalidCells);
    }

    /*@
     @ requires true;
     @ ensures \result == validationFeedbackEnabled;
     @*/
    public boolean isValidationFeedbackEnabled() {
        assert invariant();
        boolean result = validationFeedbackEnabled;
        assert invariant();
        return result;
    }

    /*@
     @ requires true;
     @ ensures validationFeedbackEnabled == enabled;
     @*/
    public void setValidationFeedbackEnabled(boolean enabled) {
        assert invariant();
        int[][] oldBoard = copyOf(board);
        validationFeedbackEnabled = enabled;
        publishChange();
        assert validationFeedbackEnabled == enabled : "setValidationFeedbackEnabled must store the requested flag";
        assert boardsEqual(board, oldBoard) : "setValidationFeedbackEnabled must not modify the board";
        assert invariant();
    }

    /*@
     @ requires true;
     @ ensures \result == hintEnabled;
     @*/
    public boolean isHintEnabled() {
        assert invariant();
        boolean result = hintEnabled;
        assert invariant();
        return result;
    }

    /*@
     @ requires true;
     @ ensures hintEnabled == enabled;
     @*/
    public void setHintEnabled(boolean enabled) {
        assert invariant();
        int[][] oldBoard = copyOf(board);
        hintEnabled = enabled;
        publishChange();
        assert hintEnabled == enabled : "setHintEnabled must store the requested flag";
        assert boardsEqual(board, oldBoard) : "setHintEnabled must not modify the board";
        assert invariant();
    }

    /*@
     @ requires true;
     @ ensures \result == randomPuzzleSelectionEnabled;
     @*/
    public boolean isRandomPuzzleSelectionEnabled() {
        assert invariant();
        boolean result = randomPuzzleSelectionEnabled;
        assert invariant();
        return result;
    }

    /*@
     @ requires true;
     @ ensures randomPuzzleSelectionEnabled == enabled;
     @*/
    public void setRandomPuzzleSelectionEnabled(boolean enabled) {
        assert invariant();
        int[][] oldBoard = copyOf(board);
        randomPuzzleSelectionEnabled = enabled;
        publishChange();
        assert randomPuzzleSelectionEnabled == enabled : "setRandomPuzzleSelectionEnabled must store the requested flag";
        assert boardsEqual(board, oldBoard) : "setRandomPuzzleSelectionEnabled must not modify the board";
        assert invariant();
    }

    /*@
     @ requires observer != null;
     @ ensures invariant();
     @*/
    @Override
    public synchronized void addObserver(Observer observer) {
        assert invariant();
        assert observer != null : "observer != null";
        super.addObserver(observer);
        assert invariant();
    }

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

    private void markInvalidRows(Set<SudokuModel.CellPosition> invalidCells) {
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
                    invalidCells.add(new SudokuModel.CellPosition(row, column));
                }
            }
        }
    }

    private void markInvalidColumns(Set<SudokuModel.CellPosition> invalidCells) {
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
                    invalidCells.add(new SudokuModel.CellPosition(row, column));
                }
            }
        }
    }

    private void markInvalidBoxes(Set<SudokuModel.CellPosition> invalidCells) {
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
                            invalidCells.add(new SudokuModel.CellPosition(row, column));
                        }
                    }
                }
            }
        }
    }

    private boolean invalidCellSetMatchesBoard(Set<SudokuModel.CellPosition> invalidCells) {
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                boolean listed = invalidCells.contains(new SudokuModel.CellPosition(row, column));
                if (listed != isConflictingOccupiedCell(row, column)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isConflictingOccupiedCell(int row, int column) {
        if (board[row][column] == 0) {
            return false;
        }
        return appearsMoreThanOnceInRow(row, column)
                || appearsMoreThanOnceInColumn(row, column)
                || appearsMoreThanOnceInBox(row, column);
    }

    private boolean appearsMoreThanOnceInRow(int row, int column) {
        int value = board[row][column];
        int occurrences = 0;
        for (int currentColumn = 0; currentColumn < SIZE; currentColumn++) {
            if (board[row][currentColumn] == value) {
                occurrences++;
            }
        }
        return occurrences > 1;
    }

    private boolean appearsMoreThanOnceInColumn(int row, int column) {
        int value = board[row][column];
        int occurrences = 0;
        for (int currentRow = 0; currentRow < SIZE; currentRow++) {
            if (board[currentRow][column] == value) {
                occurrences++;
            }
        }
        return occurrences > 1;
    }

    private boolean appearsMoreThanOnceInBox(int row, int column) {
        int value = board[row][column];
        int boxRow = (row / 3) * 3;
        int boxColumn = (column / 3) * 3;
        int occurrences = 0;
        for (int currentRow = boxRow; currentRow < boxRow + 3; currentRow++) {
            for (int currentColumn = boxColumn; currentColumn < boxColumn + 3; currentColumn++) {
                if (board[currentRow][currentColumn] == value) {
                    occurrences++;
                }
            }
        }
        return occurrences > 1;
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

    private boolean boardsEqualExceptAt(int[][] left, int[][] right, int excludedRow, int excludedColumn) {
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (row == excludedRow && column == excludedColumn) {
                    continue;
                }
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
        if (board == null || fixedCells == null || undoSnapshots == null || currentPuzzle == null) {
            return false;
        }
        if (currentPuzzle.puzzle() == null || currentPuzzle.solution() == null) {
            return false;
        }
        if (board.length != SIZE || fixedCells.length != SIZE
                || currentPuzzle.puzzle().length != SIZE || currentPuzzle.solution().length != SIZE) {
            return false;
        }
        for (int row = 0; row < SIZE; row++) {
            if (board[row] == null || fixedCells[row] == null
                    || currentPuzzle.puzzle()[row] == null || currentPuzzle.solution()[row] == null) {
                return false;
            }
            if (board[row].length != SIZE || fixedCells[row].length != SIZE
                    || currentPuzzle.puzzle()[row].length != SIZE || currentPuzzle.solution()[row].length != SIZE) {
                return false;
            }
            for (int column = 0; column < SIZE; column++) {
                int value = board[row][column];
                int clue = currentPuzzle.puzzle()[row][column];
                int solutionValue = currentPuzzle.solution()[row][column];
                if (value < 0 || value > 9) {
                    return false;
                }
                if (clue < 0 || clue > 9 || solutionValue < 1 || solutionValue > 9) {
                    return false;
                }
                boolean expectedFixed = clue != 0;
                if (fixedCells[row][column] != expectedFixed) {
                    return false;
                }
                if (fixedCells[row][column] && board[row][column] != clue) {
                    return false;
                }
                if (clue != 0 && clue != solutionValue) {
                    return false;
                }
            }
        }
        return true;
    }

    private record Puzzle(int[][] puzzle, int[][] solution) { }

    private record BoardSnapshot(int[][] boardState) { }
}
