package sudoku;

import java.util.Scanner;
import java.util.Set;

public class SudokuCLI {
    private final SudokuModel model = new Model();

    public static void main(String[] args) {
        new SudokuCLI().run();
    }

    private void run() {
        Scanner scanner = new Scanner(System.in);
        printBoard();
        while (true) {
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

    private void handleUndo(String[] parts) {
        requireLength(parts, 1);
        if (!model.undo()) {
            System.out.println("Nothing to undo.");
        }
        afterBoardAction();
    }

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

    private void afterBoardAction() {
        printBoard();
        if (model.isValidationFeedbackEnabled()) {
            Set<SudokuModel.CellPosition> invalidCells = model.getInvalidCells();
            if (!invalidCells.isEmpty()) {
                System.out.println("Invalid move detected.");
            }
        }
        if (model.isSolved()) {
            System.out.println("Puzzle completed correctly.");
        }
    }

    private void printBoard() {
        for (int row = 0; row < SudokuModel.SIZE; row++) {
            if (row % 3 == 0) {
                System.out.println("+-------+-------+-------+");
            }
            for (int column = 0; column < SudokuModel.SIZE; column++) {
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

    private void requireLength(String[] parts, int expectedLength) {
        if (parts.length != expectedLength) {
            throw new IllegalArgumentException("Invalid command.");
        }
    }

    private int parseIndex(String text) {
        int value = Integer.parseInt(text);
        if (value < 1 || value > SudokuModel.SIZE) {
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
