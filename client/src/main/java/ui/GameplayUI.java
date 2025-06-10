package ui;

import java.util.Scanner;

public class GameplayUI {
    private final Scanner scanner;
    private chess.ChessBoard currentBoard;
    private String perspective = "observer";
    private server.WebsocketCommunicator communicator;
    private String authToken;
    private int gameID;
    private PostloginUI postloginUI;

    public GameplayUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public void setBoard(chess.ChessBoard board) {
        this.currentBoard = board;
    }

    public void setPerspective(String perspective) {
        this.perspective = perspective;
    }

    public void setCommunicator(server.WebsocketCommunicator communicator) {
        this.communicator = communicator;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public void setGameID(int gameID) {
        this.gameID = gameID;
    }

    public void setPostloginUI(PostloginUI postloginUI) {
        this.postloginUI = postloginUI;
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
            case "leave" -> handleLeave();
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

    /**
     * Handles the leave command: sends LEAVE, closes WebSocket, and transitions to PostloginUI.
     */
    private void handleLeave() {
        try {
            if (communicator != null) {
                communicator.sendLeaveCommand(authToken, gameID);
                communicator.closeAndCleanup();
            }
        } catch (Exception e) {
            System.out.println("Error sending leave command: " + e.getMessage());
        }
        System.out.println("You have left the game. Returning to main menu...");
        if (postloginUI != null) {
            postloginUI.run();
        }
    }
} 