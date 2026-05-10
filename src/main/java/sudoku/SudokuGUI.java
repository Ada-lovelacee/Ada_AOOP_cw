package sudoku;

import javax.swing.SwingUtilities;

/**
 * Entry point for the Swing MVC version of the game.
 * Creates one SudokuModel, then wires it to SudokuView and SudokuController.
 * This keeps startup code separate from the Model, View and Controller classes.
 */
public final class SudokuGUI {
    /**
     * Utility class constructor kept private because this class only launches
     * the GUI and does not represent application state.
     */
    private SudokuGUI() {

    }

    /**
     * Wires the Swing MVC objects together on the Event Dispatch Thread.
     * The order matters: the View observes the Model, the Controller receives
     * user events from the View, and both View and Controller share the same
     * ISudokuModel instance.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ISudokuModel model = new SudokuModel();
            SudokuView view = new SudokuView(model);
            SudokuController controller = new SudokuController(model);
            view.attachController(controller);
            controller.attachView(view);
            view.setVisible(true);
        });
    }
}
