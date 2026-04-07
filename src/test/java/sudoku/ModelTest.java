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
        fillAllEditableCellsExcept(model, 8, 8);

        assertFalse(model.isSolved());

        assertTrue(model.setCellValue(8, 8, 9));
        assertTrue(model.isSolved());
    }

    @Test
    void reportsDuplicatesAcrossRowsColumnsAndBoxes() {
        // Scenario: two editable moves create duplicates in a row, a column and the same 3x3 box.
        Model model = new Model(new Random(0L));
        model.setRandomPuzzleSelectionEnabled(false);
        model.newGame();
        model.setCellValue(0, 2, 5);
        model.setCellValue(1, 1, 3);

        Set<Model.CellPosition> invalidCells = model.getInvalidCells();

        assertTrue(invalidCells.contains(new Model.CellPosition(0, 0)));
        assertTrue(invalidCells.contains(new Model.CellPosition(0, 1)));
        assertTrue(invalidCells.contains(new Model.CellPosition(0, 2)));
        assertTrue(invalidCells.contains(new Model.CellPosition(1, 1)));
        assertFalse(model.isSolved());
    }

    @Test
    void hintUndoAndResetAffectOnlyEditableState() {
        // Scenario: a hint fills one editable cell, undo restores it, and reset clears user-entered values.
        Model model = new Model(new Random(0L));
        model.setRandomPuzzleSelectionEnabled(false);
        model.newGame();
        int fixedValue = model.getCellValue(0, 0);

        assertTrue(model.applyHint(0, 2));
        assertEquals(4, model.getCellValue(0, 2));

        assertTrue(model.undo());
        assertEquals(0, model.getCellValue(0, 2));

        assertTrue(model.setCellValue(0, 2, 4));
        model.resetPuzzle();

        assertEquals(0, model.getCellValue(0, 2));
        assertEquals(fixedValue, model.getCellValue(0, 0));
        assertFalse(model.clearCell(0, 0));
    }

    private void fillAllEditableCellsExcept(Model model, int excludedRow, int excludedColumn) {
        int[][] solution = {
                {5, 3, 4, 6, 7, 8, 9, 1, 2},
                {6, 7, 2, 1, 9, 5, 3, 4, 8},
                {1, 9, 8, 3, 4, 2, 5, 6, 7},
                {8, 5, 9, 7, 6, 1, 4, 2, 3},
                {4, 2, 6, 8, 5, 3, 7, 9, 1},
                {7, 1, 3, 9, 2, 4, 8, 5, 6},
                {9, 6, 1, 5, 3, 7, 2, 8, 4},
                {2, 8, 7, 4, 1, 9, 6, 3, 5},
                {3, 4, 5, 2, 8, 6, 1, 7, 9}
        };
        for (int row = 0; row < Model.SIZE; row++) {
            for (int column = 0; column < Model.SIZE; column++) {
                if (row == excludedRow && column == excludedColumn) {
                    continue;
                }
                if (model.isEditableCell(row, column)) {
                    model.setCellValue(row, column, solution[row][column]);
                }
            }
        }
    }
}
