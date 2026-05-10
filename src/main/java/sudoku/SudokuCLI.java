package sudoku;

import java.util.Scanner;
import java.util.Set;

/**
 * Command-line version of the Sudoku game.
 * It is a separate runnable entry point from the Swing GUI, but it reuses the
 * same ISudokuModel contract so puzzle rules, hints and validation stay
 * consistent. Console input is handled here instead of through SudokuController.
 */
public class SudokuCLI {
    private final ISudokuModel model = new SudokuModel();

    public static void main(String[] args) {
        new SudokuCLI().run();
    }

    /**
     * Main console loop.
     * The board and command format are printed before input so the user can see
     * both the current state and the exact syntax expected by the parser.
     */
    private void run() {
        Scanner scanner = new Scanner(System.in);
        printBoard();
        while (true) {
            printCommandUsage();
            System.out.print("Enter command: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (processCommand(line)) {
                return;
            }
        }
    }

    /**
     * Parses the first word as a command and dispatches to a small handler.
     * The handlers perform argument validation and call the shared Model, while
     * this method centralises invalid command feedback.
     */
    private boolean processCommand(String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();
        try {
            switch (command) {
                case "set" -> handleSet(parts);
                case "clear" -> handleClear(parts);
                case "undo" -> handleUndo(parts);
                case "hint" -> handleHint(parts);
                case "reset" -> handleReset(parts);
                case "new" -> handleNew(parts);
                case "exit" -> {
                    return true;
                }
                default -> System.out.println("Invalid command.");
            }
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }
        return false;
    }

    private void handleSet(String[] parts) {
        requireLength(parts, 4);
        int row = parseIndex(parts[1]);
        int column = parseIndex(parts[2]);
        int value = parseValue(parts[3]);
        if (!model.setCellValue(row, column, value)) {
            System.out.println("Move rejected.");
        }
        afterBoardAction();
    }

    private void handleClear(String[] parts) {
        requireLength(parts, 3);
        int row = parseIndex(parts[1]);
        int column = parseIndex(parts[2]);
        if (!model.clearCell(row, column)) {
            System.out.println("Move rejected.");
        }
        afterBoardAction();
    }

    /**
     * Handles "undo".
     */
    private void handleUndo(String[] parts) {
        requireLength(parts, 1);
        if (!model.undo()) {
            System.out.println("Nothing to undo.");
        }
        afterBoardAction();
    }

    /**
     * Handles "hint row column".
     */
    private void handleHint(String[] parts) {
        requireLength(parts, 3);
        int row = parseIndex(parts[1]);
        int column = parseIndex(parts[2]);
        if (!model.applyHint(row, column)) {
            System.out.println("Hint rejected.");
        }
        afterBoardAction();
    }

    private void handleReset(String[] parts) {
        requireLength(parts, 1);
        model.resetPuzzle();
        afterBoardAction();
    }

    private void handleNew(String[] parts) {
        requireLength(parts, 1);
        model.newGame();
        afterBoardAction();
    }

    /**
     * Prints feedback after any command that may affect the board.
     * Validation and completion are read from the Model so the CLI does not
     * duplicate Sudoku rule checks.
     */
    private void afterBoardAction() {
        printBoard();
        if (model.isValidationFeedbackEnabled()) {
            Set<ISudokuModel.CellPosition> invalidCells = model.getInvalidCells();
            if (!invalidCells.isEmpty()) {
                System.out.println("Invalid move detected.");
            }
        }
        if (model.isSolved()) {
            System.out.println("Puzzle completed correctly.");
        }
    }

    /**
     * Prints the current board in a readable Sudoku grid.
     * Empty cells are shown as "." so players can distinguish blanks from digit
     * values at a glance.
     */
    private void printBoard() {
        for (int row = 0; row < ISudokuModel.SIZE; row++) {
            if (row % 3 == 0) {
                System.out.println("+-------+-------+-------+");
            }
            for (int column = 0; column < ISudokuModel.SIZE; column++) {
                if (column % 3 == 0) {
                    System.out.print("| ");
                }
                int value = model.getCellValue(row, column);
                System.out.print(value == 0 ? ". " : value + " ");
            }
            System.out.println("|");
        }
        System.out.println("+-------+-------+-------+");
    }

    private void printCommandUsage() {
        System.out.println("Input format:");
        System.out.println("1. set <row> <column> <value>  - place value 1-9 in a cell");
        System.out.println("2. clear <row> <column>        - clear an editable cell");
        System.out.println("3. undo                        - undo the previous move");
        System.out.println("4. hint <row> <column>         - fill an empty editable cell with a hint");
        System.out.println("5. reset                       - reset the current puzzle");
        System.out.println("6. new                         - start a new puzzle");
        System.out.println("7. exit                        - quit the CLI");
        System.out.println("Rows and columns must be numbers from 1 to 9.");
    }

    private void requireLength(String[] parts, int expectedLength) {
        if (parts.length != expectedLength) {
            throw new IllegalArgumentException("Invalid command.");
        }
    }

    private int parseIndex(String text) {
        int value = Integer.parseInt(text);
        if (value < 1 || value > ISudokuModel.SIZE) {
            throw new IllegalArgumentException("Row and column must be between 1 and 9.");
        }
        return value - 1;
    }

    private int parseValue(String text) {
        int value = Integer.parseInt(text);
        if (value < 1 || value > 9) {
            throw new IllegalArgumentException("Cell values must be between 1 and 9.");
        }
        return value;
    }
}
