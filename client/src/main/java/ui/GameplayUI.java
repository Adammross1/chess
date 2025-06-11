package ui;

import java.util.Scanner;
import chess.ChessMove;
import chess.ChessPosition;
import chess.ChessPiece;
import chess.ChessGame;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

public class GameplayUI {
    private final Scanner scanner;
    private chess.ChessBoard currentBoard;
    private String perspective = "observer";
    private server.WebsocketCommunicator communicator;
    private String authToken;
    private int gameID;
    private PostloginUI postloginUI;
    private boolean hasResigned = false;

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
            case "move" -> {
                if (hasResigned) {
                    System.out.println("You have resigned and cannot make moves.");
                } else {
                    handleMakeMove();
                }
            }
            case "resign" -> {
                if (hasResigned) {
                    System.out.println("You have already resigned.");
                } else {
                    handleResign();
                }
            }
            case "highlight" -> handleHighlightLegalMoves();
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

    /**
     * Handles the make move command by prompting for start and end positions,
     * validating the input, and sending the move to the server.
     */
    private void handleMakeMove() {
        if (communicator == null) {
            System.out.println("Error: Not connected to game server");
            return;
        }

        try {
            // Get start position
            System.out.print("Enter start position (e.g., 'e2'): ");
            String startPos = scanner.nextLine().trim().toLowerCase();
            ChessPosition start = parseChessSquare(startPos);
            if (start == null) {
                System.out.println("Invalid start position. Use format 'e2' (a-h, 1-8)");
                return;
            }

            // Get end position
            System.out.print("Enter end position (e.g., 'e4'): ");
            String endPos = scanner.nextLine().trim().toLowerCase();
            ChessPosition end = parseChessSquare(endPos);
            if (end == null) {
                System.out.println("Invalid end position. Use format 'e4' (a-h, 1-8)");
                return;
            }

            // Create and send the move
            ChessMove move = new ChessMove(start, end, null); // No promotion piece for now
            communicator.sendMakeMoveCommand(authToken, gameID, move);
            System.out.println("Move sent to server. Waiting for update...");

        } catch (IOException e) {
            System.out.println("Error sending move: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error processing move: " + e.getMessage());
        }
    }

    /**
     * Handles the resign command by prompting for confirmation and sending the resign command.
     * After resigning, the player remains in the game as an observer.
     */
    private void handleResign() {
        if (communicator == null) {
            System.out.println("Error: Not connected to game server");
            return;
        }

        // Prompt for confirmation
        System.out.println("Are you sure you want to resign? This will forfeit the game. (yes/no)");
        String response = scanner.nextLine().trim().toLowerCase();
        
        if (!response.equals("yes")) {
            System.out.println("Resignation cancelled.");
            return;
        }

        try {
            communicator.sendResignCommand(authToken, gameID);
            hasResigned = true;
            System.out.println("You have resigned the game. You will remain as an observer.");
        } catch (IOException e) {
            System.out.println("Error sending resign command: " + e.getMessage());
        }
    }

    /**
     * Handles the highlight legal moves command by prompting for a piece position
     * and highlighting all legal moves for that piece.
     */
    private void handleHighlightLegalMoves() {
        if (currentBoard == null) {
            System.out.println("No board to display.");
            return;
        }

        // Get piece position
        System.out.print("Enter piece position to highlight (e.g., 'e2'): ");
        String posStr = scanner.nextLine().trim().toLowerCase();
        ChessPosition pos = parseChessSquare(posStr);
        if (pos == null) {
            System.out.println("Invalid position. Use format 'e2' (a-h, 1-8)");
            return;
        }

        // Get the piece at the position
        ChessPiece piece = currentBoard.getPiece(pos);
        if (piece == null) {
            System.out.println("No piece at that position.");
            ChessBoardRenderer.clearHighlights();
            redrawBoard();
            return;
        }

        // Get all legal moves for the piece
        Set<ChessPosition> legalMoves = new HashSet<>();
        legalMoves.add(pos); // Add the piece's current position

        // Add all possible moves for this piece type
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition endPos = new ChessPosition(row, col);
                ChessMove move = new ChessMove(pos, endPos, null);
                try {
                    if (currentBoard.getPiece(pos).pieceMoves(currentBoard, pos).contains(move)) {
                        legalMoves.add(endPos);
                    }
                } catch (Exception e) {
                    // Skip invalid moves
                }
            }
        }

        // Highlight the moves and redraw the board
        ChessBoardRenderer.setHighlightedSquares(legalMoves);
        redrawBoard();
    }

    /**
     * Parses a chess square string (e.g., "e2") into a ChessPosition.
     * @param square The square string to parse (e.g., "e2")
     * @return The ChessPosition, or null if invalid
     */
    private ChessPosition parseChessSquare(String square) {
        if (square == null || square.length() != 2) {
            return null;
        }

        char file = square.charAt(0);
        char rank = square.charAt(1);

        // Convert file (a-h) to column (1-8)
        int column = file - 'a' + 1;
        // Convert rank (1-8) to row (1-8)
        int row = rank - '0';

        // Validate the position is on the board
        if (column < 1 || column > 8 || row < 1 || row > 8) {
            return null;
        }

        return new ChessPosition(row, column);
    }
} 