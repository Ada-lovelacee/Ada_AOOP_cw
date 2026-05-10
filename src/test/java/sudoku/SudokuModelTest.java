package sudoku;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Three readable Model scenarios: cell editing, validation, and completion.
 * These tests focus on the Model because it owns the coursework's important
 * behaviour: puzzle state, edit rules, validation, hinting, undo and completion.
 * The GUI and CLI are thin clients around the same ISudokuModel contract, so
 * testing the Model gives confidence that both entry points use the same rules.
 */
@SuppressWarnings("deprecation")
class SudokuModelTest {
    private static final ISudokuModel.CellPosition EDITABLE_CELL = new ISudokuModel.CellPosition(0, 0);
    private static final ISudokuModel.CellPosition PREFILLED_CELL = new ISudokuModel.CellPosition(0, 2);
    private static final ISudokuModel.CellPosition FINAL_COMPLETION_CELL = new ISudokuModel.CellPosition(8, 6);

    private ISudokuModel model;

    /**
     * Creates a fresh deterministic puzzle before every test.
     * Each scenario starts from a clean Model so edits, undo snapshots and
     * validation conflicts from one test cannot affect another.
     */
    @BeforeEach
    void setUp() {
        model = createFixedPuzzleModel();
    }

    /**
     * Verifies the core cell-editing contract.
     * The test first proves the chosen empty and pre-filled coordinates have the
     * expected starting state, then checks each mutating operation on an editable
     * cell, and finally checks that the same operations are rejected for a fixed
     * clue.
     */
    /*@
     @ normal_behavior
     @ requires model != null;
     @ requires model.isEditableCell(EDITABLE_CELL.row(), EDITABLE_CELL.column());
     @ requires model.getCellValue(EDITABLE_CELL.row(), EDITABLE_CELL.column()) == 0;
     @ requires !model.isEditableCell(PREFILLED_CELL.row(), PREFILLED_CELL.column());
     @ requires model.getCellValue(PREFILLED_CELL.row(), PREFILLED_CELL.column()) == 1;
     @ ensures model.getCellValue(EDITABLE_CELL.row(), EDITABLE_CELL.column()) == 0;
     @ ensures model.getCellValue(PREFILLED_CELL.row(), PREFILLED_CELL.column()) == 1;
     @ ensures !model.canUndo();
     @*/
    @Test
    void testCellEditingRulesForEditableAndPrefilledCells() {
        assertTrue(model.isEditableCell(EDITABLE_CELL.row(), EDITABLE_CELL.column()));
        assertEquals(0, getValueAt(EDITABLE_CELL));

        assertFalse(model.isEditableCell(PREFILLED_CELL.row(), PREFILLED_CELL.column()));
        assertEquals(1, getValueAt(PREFILLED_CELL));

        assertTrue(model.setCellValue(EDITABLE_CELL.row(), EDITABLE_CELL.column(), 2));
        assertEquals(2, getValueAt(EDITABLE_CELL));
        assertTrue(model.undo());
        assertEquals(0, getValueAt(EDITABLE_CELL));

        assertTrue(model.setCellValue(EDITABLE_CELL.row(), EDITABLE_CELL.column(), 2));
        assertTrue(model.clearCell(EDITABLE_CELL.row(), EDITABLE_CELL.column()));
        assertEquals(0, getValueAt(EDITABLE_CELL));
        assertTrue(model.undo());
        assertEquals(2, getValueAt(EDITABLE_CELL));
        assertTrue(model.clearCell(EDITABLE_CELL.row(), EDITABLE_CELL.column()));

        assertTrue(model.applyHint(EDITABLE_CELL.row(), EDITABLE_CELL.column()));
        assertTrue(getValueAt(EDITABLE_CELL) >= 1 && getValueAt(EDITABLE_CELL) <= 9);
        assertTrue(model.undo());
        assertEquals(0, getValueAt(EDITABLE_CELL));

        assertFalse(model.setCellValue(PREFILLED_CELL.row(), PREFILLED_CELL.column(), 2));
        assertFalse(model.clearCell(PREFILLED_CELL.row(), PREFILLED_CELL.column()));
        assertFalse(model.applyHint(PREFILLED_CELL.row(), PREFILLED_CELL.column()));
        assertFalse(model.undo());
        assertEquals(1, getValueAt(PREFILLED_CELL));
    }

    /**
     * Verifies duplicate detection for all three Sudoku constraint types.
     * The Model marks conflicts by returning the coordinates involved in the
     * duplicate, so this test checks row, column and 3 by 3 box conflicts
     * separately.
     */
    /*@
     @ normal_behavior
     @ requires model != null;
     @ requires model.getInvalidCells().isEmpty();
     @ ensures !model.isValidationFeedbackEnabled();
     @ ensures model.getInvalidCells().contains(new ISudokuModel.CellPosition(0, 0));
     @ ensures model.getInvalidCells().contains(new ISudokuModel.CellPosition(1, 2));
     @ ensures model.getInvalidCells().size() == 2;
     @ ensures !model.isSolved();
     @*/
    @Test
    void testCellValidationFindsRowColumnAndBoxDuplicates() {
        assertTrue(model.getInvalidCells().isEmpty());

        ISudokuModel.CellPosition rowTarget = new ISudokuModel.CellPosition(0, 1);
        ISudokuModel.CellPosition rowConflict = new ISudokuModel.CellPosition(0, 8);
        assertTrue(model.setCellValue(rowTarget.row(), rowTarget.column(), 9));
        assertInvalidPair(rowTarget, rowConflict);
        assertTrue(model.undo());

        ISudokuModel.CellPosition columnTarget = new ISudokuModel.CellPosition(0, 0);
        ISudokuModel.CellPosition columnConflict = new ISudokuModel.CellPosition(6, 0);
        assertTrue(model.setCellValue(columnTarget.row(), columnTarget.column(), 4));
        assertInvalidPair(columnTarget, columnConflict);
        assertTrue(model.undo());

        model.setValidationFeedbackEnabled(false);
        assertFalse(model.isValidationFeedbackEnabled());

        ISudokuModel.CellPosition boxTarget = new ISudokuModel.CellPosition(0, 0);
        ISudokuModel.CellPosition boxConflict = new ISudokuModel.CellPosition(1, 2);
        assertTrue(model.setCellValue(boxTarget.row(), boxTarget.column(), 3));
        assertInvalidPair(boxTarget, boxConflict);
        assertFalse(model.isSolved());
    }

    /**
     * Verifies that completion means "matches the solved grid", not just "all
     * cells contain values". The test fills almost the whole puzzle with hints,
     * completes it correctly, then deliberately enters a wrong value in the last
     * cell to prove isSolved() becomes false again.
     */
    /*@
     @ normal_behavior
     @ requires model != null;
     @ requires model.isEditableCell(FINAL_COMPLETION_CELL.row(), FINAL_COMPLETION_CELL.column());
     @ requires model.getCellValue(FINAL_COMPLETION_CELL.row(), FINAL_COMPLETION_CELL.column()) == 0;
     @ ensures model.isSolved();
     @ ensures model.getCellValue(FINAL_COMPLETION_CELL.row(), FINAL_COMPLETION_CELL.column()) != 0;
     @ ensures model.getInvalidCells().isEmpty();
     @*/
    @Test
    void testPuzzleCompletionRequiresEveryCellToMatchSolution() {
        fillEditableCellsExcept(FINAL_COMPLETION_CELL);
        assertEquals(0, getValueAt(FINAL_COMPLETION_CELL));
        assertFalse(model.isSolved());

        assertTrue(model.applyHint(FINAL_COMPLETION_CELL.row(), FINAL_COMPLETION_CELL.column()));
        assertTrue(model.isSolved());

        int correctValue = getValueAt(FINAL_COMPLETION_CELL);
        int wrongValue = model.getCellValue(FINAL_COMPLETION_CELL.row(), 0);
        assertFalse(wrongValue == correctValue);

        assertTrue(model.undo());
        assertEquals(0, getValueAt(FINAL_COMPLETION_CELL));

        assertTrue(model.setCellValue(FINAL_COMPLETION_CELL.row(), FINAL_COMPLETION_CELL.column(), wrongValue));
        assertTrue(model.getInvalidCells().contains(FINAL_COMPLETION_CELL));
        assertFalse(model.isSolved());

        assertTrue(model.undo());
        assertTrue(model.applyHint(FINAL_COMPLETION_CELL.row(), FINAL_COMPLETION_CELL.column()));
        assertTrue(model.isSolved());
    }

    /**
     * Creates a Model locked to the first puzzle.
     * Random puzzle selection is disabled here so every coordinate used in these
     * tests points to a known clue or known empty cell.
     */
    private ISudokuModel createFixedPuzzleModel() {
        ISudokuModel model = new SudokuModel();
        model.setRandomPuzzleSelectionEnabled(false);
        model.newGame();
        return model;
    }

    /**
     * Small readability helper for assertions that check one coordinate.
     */
    private int getValueAt(ISudokuModel.CellPosition cell) {
        return model.getCellValue(cell.row(), cell.column());
    }

    /**
     * Asserts that exactly two cells are currently marked invalid.
     * This keeps row, column and box tests precise: if validation misses one
     * side of the conflict or marks unrelated cells, the assertion fails.
     */
    private void assertInvalidPair(
            ISudokuModel.CellPosition target,
            ISudokuModel.CellPosition conflict
    ) {
        Set<ISudokuModel.CellPosition> invalidCells = model.getInvalidCells();
        assertEquals(2, invalidCells.size());
        assertTrue(invalidCells.contains(target));
        assertTrue(invalidCells.contains(conflict));
    }

    /**
     * Uses hints to fill every editable cell except one chosen coordinate.
     * Hints are used instead of hard-coded solution values so the test remains
     * tied to the Model's own solved grid and does not duplicate puzzle answers.
     */
    private void fillEditableCellsExcept(ISudokuModel.CellPosition excludedCell) {
        for (int row = 0; row < ISudokuModel.SIZE; row++) {
            for (int column = 0; column < ISudokuModel.SIZE; column++) {
                ISudokuModel.CellPosition currentCell = new ISudokuModel.CellPosition(row, column);
                if (currentCell.equals(excludedCell) || !model.isEditableCell(row, column)) {
                    continue;
                }
                assertTrue(model.applyHint(row, column));
            }
        }
    }
}
