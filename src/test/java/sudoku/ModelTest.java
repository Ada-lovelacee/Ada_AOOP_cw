package sudoku;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelTest {
    @Test
    void completesOnlyWhenAllEditableCellsMatchTheSolution() {
        // Scenario: the board is almost complete, then the final correct move should trigger completion.
        Model model = new Model(new Random(0L));
        model.setRandomPuzzleSelectionEnabled(false);
        model.newGame();
        SudokuModel.CellPosition finalCell = findLastEditableCell(model);
        fillAllEditableCellsExceptWithHints(model, finalCell.row(), finalCell.column());

        assertFalse(model.isSolved());

        assertTrue(model.applyHint(finalCell.row(), finalCell.column()));
        assertTrue(model.isSolved());
    }

    @Test
    void reportsDuplicatesAcrossRowsColumnsAndBoxes() {
        // Scenario: two editable moves trigger duplicate detection across a row, a column and 3x3 boxes.
        Model model = new Model(new Random(0L));
        model.setRandomPuzzleSelectionEnabled(false);
        model.newGame();
        PlacementScenario rowScenario = findRowBoxDuplicateScenario(model);
        PlacementScenario columnScenario = findColumnBoxDuplicateScenario(model, rowScenario.target());

        assertTrue(model.setCellValue(rowScenario.target().row(), rowScenario.target().column(), rowScenario.duplicateValue()));
        assertTrue(model.setCellValue(columnScenario.target().row(), columnScenario.target().column(), columnScenario.duplicateValue()));

        Set<SudokuModel.CellPosition> invalidCells = model.getInvalidCells();

        assertTrue(invalidCells.contains(rowScenario.target()));
        assertTrue(invalidCells.contains(rowScenario.conflict()));
        assertTrue(invalidCells.contains(columnScenario.target()));
        assertTrue(invalidCells.contains(columnScenario.conflict()));
        assertFalse(model.isSolved());
    }

    @Test
    void hintUndoAndResetAffectOnlyEditableState() {
        // Scenario: a hint fills one editable cell, undo restores it, and reset clears user-entered values.
        Model model = new Model(new Random(0L));
        model.setRandomPuzzleSelectionEnabled(false);
        model.newGame();
        SudokuModel.CellPosition hintedCell = findFirstEditableCell(model);
        SudokuModel.CellPosition fixedCell = findFirstFixedCell(model);
        int fixedValue = model.getCellValue(fixedCell.row(), fixedCell.column());

        assertTrue(model.applyHint(hintedCell.row(), hintedCell.column()));
        int hintedValue = model.getCellValue(hintedCell.row(), hintedCell.column());
        assertTrue(hintedValue >= 1 && hintedValue <= 9);

        assertTrue(model.undo());
        assertEquals(0, model.getCellValue(hintedCell.row(), hintedCell.column()));

        assertTrue(model.setCellValue(hintedCell.row(), hintedCell.column(), hintedValue));
        model.resetPuzzle();

        assertEquals(0, model.getCellValue(hintedCell.row(), hintedCell.column()));
        assertEquals(fixedValue, model.getCellValue(fixedCell.row(), fixedCell.column()));
        assertFalse(model.clearCell(fixedCell.row(), fixedCell.column()));
    }

    private void fillAllEditableCellsExceptWithHints(SudokuModel model, int excludedRow, int excludedColumn) {
        for (int row = 0; row < SudokuModel.SIZE; row++) {
            for (int column = 0; column < SudokuModel.SIZE; column++) {
                if (row == excludedRow && column == excludedColumn) {
                    continue;
                }
                if (model.isEditableCell(row, column)) {
                    assertTrue(model.applyHint(row, column));
                }
            }
        }
    }

    private SudokuModel.CellPosition findLastEditableCell(SudokuModel model) {
        SudokuModel.CellPosition result = null;
        for (int row = 0; row < SudokuModel.SIZE; row++) {
            for (int column = 0; column < SudokuModel.SIZE; column++) {
                if (model.isEditableCell(row, column)) {
                    result = new SudokuModel.CellPosition(row, column);
                }
            }
        }
        assertTrue(result != null);
        return result;
    }

    private SudokuModel.CellPosition findFirstEditableCell(SudokuModel model) {
        for (int row = 0; row < SudokuModel.SIZE; row++) {
            for (int column = 0; column < SudokuModel.SIZE; column++) {
                if (model.isEditableCell(row, column)) {
                    return new SudokuModel.CellPosition(row, column);
                }
            }
        }
        throw new AssertionError("Expected at least one editable cell");
    }

    private SudokuModel.CellPosition findFirstFixedCell(SudokuModel model) {
        for (int row = 0; row < SudokuModel.SIZE; row++) {
            for (int column = 0; column < SudokuModel.SIZE; column++) {
                if (model.isFixedCell(row, column)) {
                    return new SudokuModel.CellPosition(row, column);
                }
            }
        }
        throw new AssertionError("Expected at least one fixed cell");
    }

    private PlacementScenario findRowBoxDuplicateScenario(SudokuModel model) {
        for (int row = 0; row < SudokuModel.SIZE; row++) {
            for (int column = 0; column < SudokuModel.SIZE; column++) {
                if (!model.isEditableCell(row, column) || !model.isCellEmpty(row, column)) {
                    continue;
                }
                SudokuModel.CellPosition conflict = findFixedWithSameBoxDifferentColumn(model, row, column, row);
                if (conflict != null) {
                    return new PlacementScenario(
                            new SudokuModel.CellPosition(row, column),
                            model.getCellValue(conflict.row(), conflict.column()),
                            conflict
                    );
                }
            }
        }
        throw new AssertionError("Expected a cell that can duplicate a row and box value");
    }

    private PlacementScenario findColumnBoxDuplicateScenario(
            SudokuModel model,
            SudokuModel.CellPosition excludedTarget
    ) {
        for (int row = 0; row < SudokuModel.SIZE; row++) {
            for (int column = 0; column < SudokuModel.SIZE; column++) {
                SudokuModel.CellPosition target = new SudokuModel.CellPosition(row, column);
                if (target.equals(excludedTarget) || !model.isEditableCell(row, column) || !model.isCellEmpty(row, column)) {
                    continue;
                }
                SudokuModel.CellPosition conflict = findFixedWithSameBoxDifferentRow(model, row, column, column);
                if (conflict != null) {
                    return new PlacementScenario(
                            target,
                            model.getCellValue(conflict.row(), conflict.column()),
                            conflict
                    );
                }
            }
        }
        throw new AssertionError("Expected a cell that can duplicate a column and box value");
    }

    private SudokuModel.CellPosition findFixedWithSameBoxDifferentColumn(
            SudokuModel model,
            int targetRow,
            int targetColumn,
            int requiredRow
    ) {
        int boxColumn = (targetColumn / 3) * 3;
        for (int column = boxColumn; column < boxColumn + 3; column++) {
            if (column == targetColumn) {
                continue;
            }
            if (model.isFixedCell(requiredRow, column)) {
                return new SudokuModel.CellPosition(requiredRow, column);
            }
        }
        return null;
    }

    private SudokuModel.CellPosition findFixedWithSameBoxDifferentRow(
            SudokuModel model,
            int targetRow,
            int targetColumn,
            int requiredColumn
    ) {
        int boxRow = (targetRow / 3) * 3;
        int boxColumn = (targetColumn / 3) * 3;
        for (int row = boxRow; row < boxRow + 3; row++) {
            if (row == targetRow) {
                continue;
            }
            if (model.isFixedCell(row, requiredColumn)) {
                return new SudokuModel.CellPosition(row, requiredColumn);
            }
        }
        return null;
    }

    private record PlacementScenario(
            SudokuModel.CellPosition target,
            int duplicateValue,
            SudokuModel.CellPosition conflict
    ) { }
}
