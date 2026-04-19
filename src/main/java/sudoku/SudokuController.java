package sudoku;

public class SudokuController {
    private final SudokuModel model;
    private SudokuView view;

    private int selectedRow;
    private int selectedColumn;

    public SudokuController(SudokuModel model) {
        assert model != null : "model != null";
        this.model = model;
        this.selectedRow = 0;
        this.selectedColumn = 0;
    }

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
        int newRow = Math.max(0, Math.min(SudokuModel.SIZE - 1, selectedRow + rowDelta));
        int newColumn = Math.max(0, Math.min(SudokuModel.SIZE - 1, selectedColumn + columnDelta));
        selectCell(newRow, newColumn);
    }

    public void handleNumberInput(int value) {
        if (value < 1 || value > 9 || !model.isEditableCell(selectedRow, selectedColumn)) {
            return;
        }
        if (model.setCellValue(selectedRow, selectedColumn, value)) {
            finishBoardAction();
        }
    }

    public void eraseSelectedCell() {
        if (!model.isEditableCell(selectedRow, selectedColumn)) {
            return;
        }
        if (model.clearCell(selectedRow, selectedColumn)) {
            finishBoardAction();
        }
    }

    public void undo() {
        if (model.undo()) {
            finishBoardAction();
        }
    }

    public void hintSelectedCell() {
        if (model.applyHint(selectedRow, selectedColumn)) {
            finishBoardAction();
        }
    }

    public void resetPuzzle() {
        model.resetPuzzle();
        refreshActionAvailability();
    }

    public void newGame() {
        model.newGame();
        refreshActionAvailability();
    }

    public void setValidationFeedbackEnabled(boolean enabled) {
        model.setValidationFeedbackEnabled(enabled);
        refreshActionAvailability();
    }

    public void setHintEnabled(boolean enabled) {
        model.setHintEnabled(enabled);
        refreshActionAvailability();
    }

    public void setRandomPuzzleSelectionEnabled(boolean enabled) {
        model.setRandomPuzzleSelectionEnabled(enabled);
        refreshActionAvailability();
    }

    public void refreshActionAvailability() {
        if (view == null) {
            return;
        }
        boolean editable = model.isEditableCell(selectedRow, selectedColumn);
        boolean empty = editable && model.isCellEmpty(selectedRow, selectedColumn);
        boolean eraseEnabled = editable && !empty;
        boolean hintEnabled = model.isHintEnabled() && editable && empty;
        view.setControlStates(editable, eraseEnabled, hintEnabled, model.canUndo());
    }

    private void finishBoardAction() {
        refreshActionAvailability();
        if (view != null && model.isSolved()) {
            view.showCompletionMessage();
        }
    }

    private boolean isInBounds(int row, int column) {
        return row >= 0 && row < SudokuModel.SIZE && column >= 0 && column < SudokuModel.SIZE;
    }
}
