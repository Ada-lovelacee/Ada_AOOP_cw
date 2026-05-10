package sudoku;

import java.util.Observer;
import java.util.Set;

/**
 * Public contract for the Sudoku Model in the MVC design.
 * SudokuModel implements this interface, while the View, Controller, CLI and
 * tests depend on this type instead of the concrete class. The interface keeps
 * board state and game operations accessible without exposing internal arrays.
 */
@SuppressWarnings("deprecation")
public interface ISudokuModel {
    int SIZE = 9;

    /**
     * Returns the current value stored in one board cell.
     * A value of 0 means the cell is empty; values 1 to 9 are visible Sudoku
     * entries. The Model remains the single source of truth for the board, so
     * callers should use this method instead of storing their own cell values.
     */
    int getCellValue(int row, int column);

    /**
     * Reports whether a cell can be changed by the player.
     * Pre-filled puzzle clues are not editable, while cells that were empty in
     * the original puzzle can be set, cleared, hinted and undone.
     */
    boolean isEditableCell(int row, int column);

    /**
     * Attempts to place a value in an editable cell.
     * The implementation rejects fixed cells, invalid values and no-op writes,
     * and records undo history only when the board actually changes.
     */
    boolean setCellValue(int row, int column, int value);

    /**
     * Attempts to clear an editable cell.
     * Fixed clues and already-empty cells are rejected so the View and CLI can
     * report a rejected move without duplicating board logic.
     */
    boolean clearCell(int row, int column);

    /**
     * Indicates whether one previous board state is available.
     * The coursework design uses single-level undo, so this becomes false after
     * a successful undo or when a new puzzle is loaded.
     */
    boolean canUndo();

    /**
     * Restores the most recent undo snapshot if one exists.
     * A successful undo consumes the snapshot, which prevents repeatedly
     * toggling between two board states through the same saved value.
     */
    boolean undo();

    /**
     * Checks whether a hint can be applied to a cell.
     * Hints require the global hint flag to be enabled, the cell to be editable,
     * and the cell to currently be empty.
     */
    boolean canHintAt(int row, int column);

    /**
     * Fills an empty editable cell with the solved value.
     * The solved board is owned by the Model, so both GUI and CLI hints follow
     * the same solution and the same undo behaviour.
     */
    boolean applyHint(int row, int column);

    /**
     * Restores the current puzzle back to its original clues.
     * This keeps the same puzzle but clears player-entered values, while still
     * allowing the reset to be undone when it changed the board.
     */
    void resetPuzzle();

    /**
     * Loads a fresh puzzle according to the puzzle selection flag.
     * New games intentionally discard undo history because the previous board
     * no longer belongs to the current puzzle.
     */
    void newGame();

    /**
     * Returns true only when every current board value matches the solved grid.
     * This is stricter than "no duplicates": an incomplete or valid-but-wrong
     * board is not considered solved.
     */
    boolean isSolved();

    /**
     * Returns all cells currently involved in row, column or box duplicates.
     * The returned set is used for validation feedback in the GUI and CLI; the
     * caller decides whether to display it based on the validation flag.
     */
    Set<CellPosition> getInvalidCells();

    /**
     * Controls whether invalid-cell feedback should be displayed by clients.
     * The Model still knows how to calculate invalid cells even when this flag
     * is false; the flag only controls presentation behaviour.
     */
    boolean isValidationFeedbackEnabled();

    /**
     * Updates the validation feedback preference and notifies observers so the
     * GUI can immediately recolour or clear highlighted cells.
     */
    void setValidationFeedbackEnabled(boolean enabled);

    /**
     * Reports whether hint actions are currently allowed.
     */
    boolean isHintEnabled();

    /**
     * Updates the hint preference and notifies observers so button state can be
     * refreshed without waiting for another board edit.
     */
    void setHintEnabled(boolean enabled);

    /**
     * Reports whether new games should choose randomly from the puzzle list.
     * When false, new games use the first puzzle, which makes tests repeatable.
     */
    boolean isRandomPuzzleSelectionEnabled();

    /**
     * Updates puzzle selection behaviour for future new games.
     */
    void setRandomPuzzleSelectionEnabled(boolean enabled);

    /**
     * Registers an Observer, normally SudokuView, to receive redraw requests
     * whenever board state or runtime flags change.
     */
    void addObserver(Observer observer);

    /**
     * Immutable row-column coordinate used in sets of invalid cells.
     * The record gives value-based equality, which makes test assertions and GUI
     * lookup code straightforward.
     */
    record CellPosition(int row, int column) { }
}

