package sudoku;

/**
 * Controller for the Swing MVC version.
 * Receives cell, keyboard and button events from SudokuView, asks ISudokuModel
 * to change game state, and then updates View controls such as Undo and Hint.
 * It does not store Sudoku rules; rule decisions stay inside the Model.
 */
public class SudokuController {
    private final ISudokuModel model;
    private SudokuView view;

    private int selectedRow;
    private int selectedColumn;

    /**
     * Creates a controller for one model instance.
     * The selected cell starts at the top-left corner so keyboard and button
     * actions always have a defined target once a View is attached.
     */
    public SudokuController(ISudokuModel model) {
        assert model != null : "model != null";
        this.model = model;
        this.selectedRow = 0;
        this.selectedColumn = 0;
    }

    /**
     * Connects the Controller back to the View after both objects exist.
     * This avoids constructing either side inside the other and keeps MVC wiring
     * inside the GUI entry point.
     */
    public void attachView(SudokuView view) {
        assert view != null : "view != null";
        this.view = view;
        view.setSelectedCell(selectedRow, selectedColumn);
        refreshActionAvailability();
    }

    public void selectCell(int row, int column) {
        if (!isInBounds(row, column)) {
            return;
        }
        selectedRow = row;
        selectedColumn = column;
        if (view != null) {
            view.setSelectedCell(row, column);
        }
        refreshActionAvailability();
    }

    public void moveSelection(int rowDelta, int columnDelta) {
        int newRow = Math.max(0, Math.min(ISudokuModel.SIZE - 1, selectedRow + rowDelta));
        int newColumn = Math.max(0, Math.min(ISudokuModel.SIZE - 1, selectedColumn + columnDelta));
        selectCell(newRow, newColumn);
    }

    /**
     * Handles number entry from buttons or keyboard.
     * The Model still makes the final rule decision.
     */
    public void handleNumberInput(int value) {
        if (value < 1 || value > 9 || !model.isEditableCell(selectedRow, selectedColumn)
                || model.getCellValue(selectedRow, selectedColumn) == value) {
            return;
        }
        model.setCellValue(selectedRow, selectedColumn, value);
        finishBoardAction();
    }

    public void eraseSelectedCell() {
        if (!model.isEditableCell(selectedRow, selectedColumn)
                || model.getCellValue(selectedRow, selectedColumn) == 0) {
            return;
        }
        model.clearCell(selectedRow, selectedColumn);
        finishBoardAction();
    }

    public void undo() {
        if (!model.canUndo()) {
            return;
        }
        model.undo();
        finishBoardAction();
    }

    public void hintSelectedCell() {
        if (!model.canHintAt(selectedRow, selectedColumn)) {
            return;
        }
        model.applyHint(selectedRow, selectedColumn);
        finishBoardAction();
    }

    public void resetPuzzle() {
        model.resetPuzzle();
    }

    public void newGame() {
        model.newGame();
    }

    public void setValidationFeedbackEnabled(boolean enabled) {
        model.setValidationFeedbackEnabled(enabled);
    }

    public void setHintEnabled(boolean enabled) {
        model.setHintEnabled(enabled);
    }

    public void setRandomPuzzleSelectionEnabled(boolean enabled) {
        model.setRandomPuzzleSelectionEnabled(enabled);
    }

    /**
     * Recomputes enabled/disabled states for GUI controls.
     * The values are derived from the selected cell and Model flags, so the View
     * only receives final presentation booleans and does not duplicate game
     * logic.
     */
    public void refreshActionAvailability() {
        if (view == null) {
            return;
        }
        boolean editable = model.isEditableCell(selectedRow, selectedColumn);
        boolean eraseEnabled = editable && model.getCellValue(selectedRow, selectedColumn) != 0;
        boolean hintEnabled = model.canHintAt(selectedRow, selectedColumn);
        view.setControlStates(editable, eraseEnabled, hintEnabled, model.canUndo());
    }

    /**
     * Runs after a successful board action.
     * Observer updates redraw the board; this method handles completion feedback.
     */
    private void finishBoardAction() {
        if (view != null && model.isSolved()) {
            view.showCompletionMessage();
        }
    }

    private boolean isInBounds(int row, int column) {
        return row >= 0 && row < ISudokuModel.SIZE && column >= 0 && column < ISudokuModel.SIZE;
    }
}
