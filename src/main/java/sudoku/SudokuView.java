package sudoku;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

@SuppressWarnings("deprecation")
public class SudokuView extends JFrame implements Observer {
    private static final Color FIXED_BACKGROUND = new Color(225, 225, 225);
    private static final Color EDITABLE_BACKGROUND = Color.WHITE;
    private static final Color INVALID_BACKGROUND = new Color(255, 219, 219);
    private static final Color SELECTED_BACKGROUND = new Color(214, 234, 248);

    private final SudokuModel model;
    private SudokuController controller;

    private final JTextField[][] cells = new JTextField[SudokuModel.SIZE][SudokuModel.SIZE];
    private final JButton[] digitButtons = new JButton[9];
    private final JButton eraseButton = new JButton("Erase");
    private final JButton undoButton = new JButton("Undo");
    private final JButton hintButton = new JButton("Hint");
    private final JButton resetButton = new JButton("Reset");
    private final JButton newGameButton = new JButton("New Game");
    private final JCheckBox validationFeedbackBox = new JCheckBox("Validation Feedback", true);
    private final JCheckBox hintBox = new JCheckBox("Hint", true);
    private final JCheckBox randomPuzzleBox = new JCheckBox("Random Puzzle Selection", true);

    private int selectedRow;
    private int selectedColumn;

    public SudokuView(SudokuModel model) {
        super("Sudoku");
        assert model != null : "model != null";
        this.model = model;
        this.model.addObserver(this);
        buildInterface();
        update(null, null);
    }

    public void attachController(SudokuController controller) {
        assert controller != null : "controller != null";
        this.controller = controller;
        wireActions();
    }

    public void setSelectedCell(int row, int column) {
        selectedRow = row;
        selectedColumn = column;
        refreshBoard();
        cells[row][column].requestFocusInWindow();
    }

    public void setControlStates(boolean digitInputEnabled, boolean eraseEnabled, boolean hintEnabled, boolean undoEnabled) {
        for (JButton digitButton : digitButtons) {
            digitButton.setEnabled(digitInputEnabled);
        }
        eraseButton.setEnabled(eraseEnabled);
        hintButton.setEnabled(hintEnabled);
        undoButton.setEnabled(undoEnabled);
    }

    public void showCompletionMessage() {
        JOptionPane.showMessageDialog(this, "Puzzle completed correctly.");
    }

    @Override
    public void update(Observable observable, Object argument) {
        validationFeedbackBox.setSelected(model.isValidationFeedbackEnabled());
        hintBox.setSelected(model.isHintEnabled());
        randomPuzzleBox.setSelected(model.isRandomPuzzleSelectionEnabled());
        refreshBoard();
        if (controller != null) {
            controller.refreshActionAvailability();
        }
    }

    private void buildInterface() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        JPanel contentPanel = new JPanel(new BorderLayout(12, 12));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contentPanel.add(buildGridPanel(), BorderLayout.CENTER);
        contentPanel.add(buildRightPanel(), BorderLayout.EAST);
        add(contentPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private JPanel buildGridPanel() {
        JPanel gridPanel = new JPanel(new GridLayout(SudokuModel.SIZE, SudokuModel.SIZE));
        gridPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        for (int row = 0; row < SudokuModel.SIZE; row++) {
            for (int column = 0; column < SudokuModel.SIZE; column++) {
                JTextField cell = new JTextField();
                cell.setHorizontalAlignment(SwingConstants.CENTER);
                cell.setFont(new Font("SansSerif", Font.BOLD, 24));
                cell.setPreferredSize(new Dimension(52, 52));
                cell.setEditable(false);
                cell.setFocusTraversalKeysEnabled(false);
                installCellHandlers(cell, row, column);
                cells[row][column] = cell;
                gridPanel.add(cell);
            }
        }
        return gridPanel;
    }

    private JPanel buildRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        JPanel keyboardPanel = new JPanel(new GridLayout(3, 3, 6, 6));
        keyboardPanel.setBorder(BorderFactory.createTitledBorder("Keyboard"));
        for (int index = 0; index < digitButtons.length; index++) {
            JButton button = new JButton(String.valueOf(index + 1));
            digitButtons[index] = button;
            keyboardPanel.add(button);
        }

        JPanel actionsPanel = new JPanel(new GridLayout(5, 1, 6, 6));
        actionsPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        actionsPanel.add(eraseButton);
        actionsPanel.add(undoButton);
        actionsPanel.add(hintButton);
        actionsPanel.add(resetButton);
        actionsPanel.add(newGameButton);

        JPanel flagsPanel = new JPanel(new GridLayout(4, 1, 6, 6));
        flagsPanel.setBorder(BorderFactory.createTitledBorder("Flags"));
        flagsPanel.add(new JLabel("Runtime options:"));
        flagsPanel.add(validationFeedbackBox);
        flagsPanel.add(hintBox);
        flagsPanel.add(randomPuzzleBox);

        rightPanel.add(keyboardPanel);
        rightPanel.add(actionsPanel);
        rightPanel.add(flagsPanel);
        return rightPanel;
    }

    private void wireActions() {
        for (int index = 0; index < digitButtons.length; index++) {
            int value = index + 1;
            digitButtons[index].addActionListener(event -> controller.handleNumberInput(value));
        }
        eraseButton.addActionListener(event -> controller.eraseSelectedCell());
        undoButton.addActionListener(event -> controller.undo());
        hintButton.addActionListener(event -> controller.hintSelectedCell());
        resetButton.addActionListener(event -> controller.resetPuzzle());
        newGameButton.addActionListener(event -> controller.newGame());
        validationFeedbackBox.addActionListener(event -> controller.setValidationFeedbackEnabled(validationFeedbackBox.isSelected()));
        hintBox.addActionListener(event -> controller.setHintEnabled(hintBox.isSelected()));
        randomPuzzleBox.addActionListener(event -> controller.setRandomPuzzleSelectionEnabled(randomPuzzleBox.isSelected()));
    }

    private void installCellHandlers(JTextField cell, int row, int column) {
        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (controller != null) {
                    controller.selectCell(row, column);
                }
            }
        });
        cell.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (controller == null) {
                    return;
                }
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_UP -> controller.moveSelection(-1, 0);
                    case KeyEvent.VK_DOWN -> controller.moveSelection(1, 0);
                    case KeyEvent.VK_LEFT -> controller.moveSelection(0, -1);
                    case KeyEvent.VK_RIGHT -> controller.moveSelection(0, 1);
                    default -> {
                        int value = extractDigit(event);
                        if (value != -1) {
                            controller.handleNumberInput(value);
                        }
                    }
                }
            }
        });
    }

    private int extractDigit(KeyEvent event) {
        if (event.getKeyCode() >= KeyEvent.VK_1 && event.getKeyCode() <= KeyEvent.VK_9) {
            return event.getKeyCode() - KeyEvent.VK_0;
        }
        if (event.getKeyCode() >= KeyEvent.VK_NUMPAD1 && event.getKeyCode() <= KeyEvent.VK_NUMPAD9) {
            return event.getKeyCode() - KeyEvent.VK_NUMPAD0;
        }
        return -1;
    }

    private void refreshBoard() {
        Set<SudokuModel.CellPosition> invalidCells = model.isValidationFeedbackEnabled()
                ? model.getInvalidCells()
                : Collections.emptySet();
        for (int row = 0; row < SudokuModel.SIZE; row++) {
            for (int column = 0; column < SudokuModel.SIZE; column++) {
                JTextField cell = cells[row][column];
                int value = model.getCellValue(row, column);
                cell.setText(value == 0 ? "" : String.valueOf(value));
                cell.setBorder(createCellBorder(row, column));
                cell.setForeground(model.isFixedCell(row, column) ? Color.BLACK : new Color(0, 70, 140));
                cell.setBackground(resolveCellBackground(row, column, invalidCells));
            }
        }
    }

    private Color resolveCellBackground(int row, int column, Set<SudokuModel.CellPosition> invalidCells) {
        Color background = model.isFixedCell(row, column) ? FIXED_BACKGROUND : EDITABLE_BACKGROUND;
        boolean invalid = invalidCells.contains(new SudokuModel.CellPosition(row, column));
        if (invalid) {
            background = INVALID_BACKGROUND;
        } else if (row == selectedRow && column == selectedColumn) {
            background = SELECTED_BACKGROUND;
        }
        return background;
    }

    private Border createCellBorder(int row, int column) {
        int top = row % 3 == 0 ? 2 : 1;
        int left = column % 3 == 0 ? 2 : 1;
        int bottom = row == SudokuModel.SIZE - 1 ? 2 : (row % 3 == 2 ? 2 : 1);
        int right = column == SudokuModel.SIZE - 1 ? 2 : (column % 3 == 2 ? 2 : 1);
        Color borderColor = row == selectedRow && column == selectedColumn ? new Color(41, 128, 185) : Color.BLACK;
        return BorderFactory.createMatteBorder(top, left, bottom, right, borderColor);
    }
}
