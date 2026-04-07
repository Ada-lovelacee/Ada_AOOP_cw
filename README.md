# AOOP Coursework: Sudoku

A Java Sudoku coursework project built with Maven. The project provides both a Swing GUI and a command-line interface, backed by a shared game model.

## Features

- 9x9 Sudoku gameplay
- Shared model used by both GUI and CLI
- Load puzzles from a bundled resource file
- Undo support
- Reset current puzzle
- Start a new puzzle
- Hint support
- Validation feedback for invalid moves
- JUnit 5 tests for model behaviour

## Project Structure

```text
src/
  main/
    java/sudoku/
      Model.java
      SudokuCLI.java
      SudokuController.java
      SudokuGUI.java
      SudokuView.java
    resources/
      puzzles.txt
  test/
    java/sudoku/
      ModelTest.java
```

## Requirements

- Java 17
- Maven 3.9+ recommended

## Build And Test

```bash
mvn test
```

## Run The GUI

```bash
mvn -DskipTests compile
java -cp target/classes sudoku.SudokuGUI
```

## Run The CLI

```bash
mvn -DskipTests compile
java -cp target/classes sudoku.SudokuCLI
```

## CLI Commands

- `set <row> <column> <value>`: place a value from 1 to 9
- `clear <row> <column>`: clear an editable cell
- `undo`: undo the previous move
- `hint <row> <column>`: fill an empty editable cell with the correct value
- `reset`: reset the board to the original puzzle
- `new`: load a new puzzle
- `exit`: quit the program

Rows and columns use values from `1` to `9`.

## Notes

- Puzzles are loaded from `src/main/resources/puzzles.txt`.
- The main game logic lives in `src/main/java/sudoku/Model.java`.
- The GUI entry point is `src/main/java/sudoku/SudokuGUI.java`.
- The CLI entry point is `src/main/java/sudoku/SudokuCLI.java`.
