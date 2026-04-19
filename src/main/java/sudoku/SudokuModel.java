package sudoku;

import java.util.Observer;
import java.util.Set;

public interface SudokuModel {
    int SIZE = 9;

    int getCellValue(int row, int column);

    boolean isEditableCell(int row, int column);

    boolean isFixedCell(int row, int column);

    boolean isCellEmpty(int row, int column);

    boolean setCellValue(int row, int column, int value);

    boolean clearCell(int row, int column);

    boolean canUndo();

    boolean undo();

    boolean canHintAt(int row, int column);

    boolean applyHint(int row, int column);

    void resetPuzzle();

    void newGame();

    boolean isSolved();

    Set<CellPosition> getInvalidCells();

    boolean isValidationFeedbackEnabled();

    void setValidationFeedbackEnabled(boolean enabled);

    boolean isHintEnabled();

    void setHintEnabled(boolean enabled);

    boolean isRandomPuzzleSelectionEnabled();

    void setRandomPuzzleSelectionEnabled(boolean enabled);

    void addObserver(Observer observer);

    record CellPosition(int row, int column) { }
}
