package ui;

import java.util.Scanner;

public class GameplayUI {
    private final Scanner scanner;
    private chess.ChessBoard currentBoard;
    private String perspective = "observer";

    public GameplayUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public void setBoard(chess.ChessBoard board) {
        this.currentBoard = board;
    }

    public void setPerspective(String perspective) {
        this.perspective = perspective;
    }

    public void redrawBoard() {
        if (currentBoard == null) {
            System.out.println("No board to display.");
        } else {
            System.out.println("\nRedrawing chess board:");
            ChessBoardRenderer.render(currentBoard, perspective);
        }
    }

    public void handleCommand(String command) {
        switch (command.trim().toLowerCase()) {
            case "help" -> showHelp();
            case "redraw" -> redrawBoard();
            // Add other commands here
            default -> System.out.println("Unknown command. Type 'help' for a list of commands.");
        }
    }

    public void showHelp() {
        System.out.println("\nAvailable Gameplay Commands:");
        System.out.println("--------------------------------------------------------------");
        System.out.printf("%-25s | %s%n", "Command", "Description");
        System.out.println("--------------------------------------------------------------");
        System.out.printf("%-25s | %s%n", "Help", "Displays text informing the user what actions they can take.");
        System.out.printf("%-25s | %s%n", "Redraw Chess Board", "Redraws the chess board upon the user's request.");
        System.out.printf("%-25s | %s%n", "Leave", "Removes the user from the game (playing or observing). Returns to Post-Login UI.");
        System.out.printf("%-25s | %s%n", "Make Move", "Allow the user to input what move they want to make. Board updates for all clients.");
        System.out.printf("%-25s | %s%n", "Resign", "Prompts the user to confirm resignation. Forfeits the game but does not leave.");
        System.out.printf("%-25s | %s%n", "Highlight Legal Moves", "Input a piece to highlight its legal moves. Local operation only.");
        System.out.println("--------------------------------------------------------------\n");
    }

} 