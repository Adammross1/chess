package ui;

import java.util.Scanner;
import chess.ChessMove;
import chess.ChessPosition;
import chess.ChessPiece;
import chess.ChessGame;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

public class GameplayUI {
    private final Scanner scanner;
    private chess.ChessBoard currentBoard;
    private chess.ChessGame currentGame;
    private String perspective = "observer";
    private server.WebsocketCommunicator communicator;
    private String authToken;
    private int gameID;
    private PostloginUI postloginUI;
    private ChessGame.TeamColor playerColor = null;

    public GameplayUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public void setBoard(chess.ChessBoard board) {
        this.currentBoard = board;
        if (currentBoard != null) {
            ChessBoardRenderer.render(currentBoard, perspective);
        } else {
            System.out.println("Warning: Attempted to set null board");
        }
    }

    public void setGame(chess.ChessGame game) {
        System.out.println("[DEBUG] ====== setGame START =====");
        System.out.println("[DEBUG] Input game: " + (game != null ? "not null" : "null"));
        
        this.currentGame = game;
        if (currentGame != null) {
            System.out.println("[DEBUG] Getting team turn in setGame...");
            ChessGame.TeamColor teamTurn = currentGame.getTeamTurn();
            System.out.println("[DEBUG] Team turn in setGame: " + teamTurn);
            
            if (currentGame.getGameState() == ChessGame.GameState.ACTIVE) {
                System.out.println("[DEBUG] Printing team turn in setGame");
                System.out.println("Team turn: " + teamTurn);
            } else {
                System.out.println("[DEBUG] Skipping team turn print in setGame - gameOver=" + currentGame.getGameState());
            }
            
            // Check for game over conditions
            System.out.println("[DEBUG] Checking game over conditions...");
            if (currentGame.getGameState() == ChessGame.GameState.CHECKMATE) {
                System.out.println("Game over: Checkmate");
            } else if (currentGame.getGameState() == ChessGame.GameState.STALEMATE) {
                System.out.println("Game over: Stalemate");
            } else if (currentGame.getGameState() == ChessGame.GameState.RESIGNED) {
                System.out.println("Game over: Resigned");
            }
        }
        System.out.println("[DEBUG] ====== setGame END =====");
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

    public void setPlayerColor(ChessGame.TeamColor color) {
        this.playerColor = color;
    }

    public void redrawBoard() {
        if (currentBoard == null) {
            System.out.println("No board to display.");
        } else {
            ChessBoardRenderer.render(currentBoard, perspective);
        }
    }

    private boolean isGameOver() {
        return currentGame != null && currentGame.getGameState() != ChessGame.GameState.ACTIVE;
    }

    private boolean hasPlayerResigned() {
        return currentGame != null && currentGame.getGameState() == ChessGame.GameState.RESIGNED;
    }

    public void handleCommand(String command) {
        if (isGameOver()) {
            switch (command.trim().toLowerCase()) {
                case "help" -> showHelp();
                case "redraw" -> redrawBoard();
                case "leave" -> handleLeave();
                default -> System.out.println("Game is over. You can only use 'help', 'redraw', or 'leave' commands.");
            }
            return;
        }

        switch (command.trim().toLowerCase()) {
            case "help" -> showHelp();
            case "redraw" -> redrawBoard();
            case "leave" -> handleLeave();
            case "move" -> {
                if (hasPlayerResigned()) {
                    System.out.println("You have resigned and cannot make moves.");
                } else if (isGameOver()) {
                    System.out.println("Game is over. No more moves can be made.");
                } else if (playerColor == null) {
                    System.out.println("Observers cannot make moves.");
                } else if (currentGame != null && currentGame.getTeamTurn() != playerColor) {
                    System.out.println("Not your turn.");
                } else {
                    handleMakeMove();
                }
            }
            case "resign" -> {
                if (hasPlayerResigned()) {
                    System.out.println("You have already resigned.");
                } else if (isGameOver()) {
                    System.out.println("Game is already over.");
                } else if (playerColor == null) {
                    System.out.println("Observers cannot resign.");
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
        System.out.printf("%-25s | %s%n", "help", "Displays text informing the user what actions they can take.");
        System.out.printf("%-25s | %s%n", "redraw", "Redraws the chess board upon the user's request.");
        System.out.printf("%-25s | %s%n", "leave", "Leaves the game and returns to the main menu.");
        if (!isGameOver() && playerColor != null) {
            System.out.printf("%-25s | %s%n", "move", "Allow the user to input what move they want to make. Board updates for all clients.");
            System.out.printf("%-25s | %s%n", "resign", "Prompts the user to confirm resignation. Forfeits the game but does not leave.");
        }
        System.out.printf("%-25s | %s%n", "highlight", "Input a piece to highlight its legal moves. Local operation only.");
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

            // Check if there's a piece at the start position
            ChessPiece piece = currentBoard.getPiece(start);
            if (piece == null) {
                System.out.println("No piece at the selected position.");
                return;
            }

            // Check if it's the player's piece
            if (piece.getTeamColor() != playerColor) {
                System.out.println("You can only move your own pieces.");
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

            // Check if the move is valid before sending to server
            Collection<ChessMove> validMoves = currentGame.validMoves(start);
            ChessMove attemptedMove = new ChessMove(start, end, null);
            
            if (!validMoves.contains(attemptedMove)) {
                System.out.println("Invalid move. Valid moves for this piece are:");
                for (ChessMove move : validMoves) {
                    System.out.println("- " + move.getStartPosition().toString() + " to " + move.getEndPosition().toString());
                }
                return;
            }

            // Create and send the move
            communicator.sendMakeMoveCommand(authToken, gameID, attemptedMove);
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

        // Check if game is already over
        if (isGameOver()) {
            System.out.println("Game is already over. Cannot resign.");
            return;
        }

        // Prompt for confirmation
        System.out.println("Are you sure you want to resign? This will forfeit the game. (yes/no)");
        String response = scanner.nextLine().trim().toLowerCase();
        
        if (!response.equals("yes")) {
            System.out.println("Resignation cancelled by user.");
            return;
        }

        try {
            communicator.sendResignCommand(authToken, gameID);
        } catch (IOException e) {
            System.out.println("Error sending resign command: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error during resignation: " + e.getMessage());
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
        
        // Clear the highlights after displaying them
        ChessBoardRenderer.clearHighlights();
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

    public void updateGameState(chess.ChessBoard board, chess.ChessGame game, String perspective) {
        this.currentBoard = board;
        if (currentBoard != null) {
            ChessBoardRenderer.render(currentBoard, perspective);
        } else {
            System.out.println("Warning: Attempted to set null board");
        }

        this.currentGame = game;
        if (currentGame != null) {
            ChessGame.TeamColor teamTurn = currentGame.getTeamTurn();
            
            // Check game state
            if (currentGame.getGameState() != ChessGame.GameState.ACTIVE) {
                if (currentGame.getGameState() == ChessGame.GameState.CHECKMATE) {
                    System.out.println("Game Over: Checkmate!");
                } else if (currentGame.getGameState() == ChessGame.GameState.STALEMATE) {
                    System.out.println("Game Over: Stalemate!");
                } else if (currentGame.getGameState() == ChessGame.GameState.RESIGNED) {
                    System.out.println("Game Over: A player has resigned!");
                }
            }
        } else {
            System.out.println("Warning: Attempted to set null game");
        }
    }
} 