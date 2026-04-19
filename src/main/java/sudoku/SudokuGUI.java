package sudoku;

import javax.swing.SwingUtilities;

public final class SudokuGUI {
    private SudokuGUI() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SudokuModel model = new Model();
            SudokuView view = new SudokuView(model);
            SudokuController controller = new SudokuController(model);
            view.attachController(controller);
            controller.attachView(view);
            view.setVisible(true);
        });
    }
}
