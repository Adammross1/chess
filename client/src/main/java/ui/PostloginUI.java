package ui;

import chess.ChessBoard;
import server.ResponseException;
import server.ServerFacade;
import model.GameData;
import chess.ChessGame;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class PostloginUI {
    private final Scanner scanner;
    private final String username;
    private final String authToken;
    private final ServerFacade serverFacade;
    private final PreloginUI preloginUI;
    private boolean running;
    private final List<Integer> gameIds; // Store game IDs for later use

    public PostloginUI(PreloginUI preloginUI, String username, String authToken, Scanner scanner, ServerFacade serverFacade) {
        this.scanner = scanner;
        this.username = username;
        this.authToken = authToken;
        this.serverFacade = serverFacade;
        this.preloginUI = preloginUI;
        this.running = true;
        this.gameIds = new ArrayList<>();
    }

    public void run() {
        System.out.println("Welcome " + username + "!");
        
        while (running) {
            System.out.print("\n[LOGGED_IN] >>> ");
            String input = scanner.nextLine().trim().toLowerCase();
            
            switch (input) {
                case "help" -> showHelp();
                case "quit" -> quit();
                case "logout" -> logout();
                case "create" -> createGame();
                case "list" -> listGames();
                case "join" -> playGame();
                case "observe" -> observeGame();
                default -> System.out.println("Unknown command. Type 'help' to see available commands.");
            }
        }
    }

    private void showHelp() {
        System.out.println("  create <NAME>  - a game");
        System.out.println("  list           - games");
        System.out.println("  join <ID> [WHITE|BLACK] - a game");
        System.out.println("  observe <ID>   - a game");
        System.out.println("  logout         - when you are done");
        System.out.println("  quit           - playing chess");
        System.out.println("  help           - with possible commands");
    }

    private void quit() {
        System.out.println("Goodbye!");
        running = false;
        System.exit(0);
    }

    private void logout() {
        try {
            serverFacade.logout(authToken);
            System.out.println("Successfully logged out!");
            running = false;
        } catch (ResponseException e) {
            if (e.getStatusCode() != 401) {
                System.out.println("Error: " + e.getMessage());
            } else {
                System.out.println("Successfully logged out!");
            }
            running = false;
        }
    }

    private void createGame() {
        try {
            System.out.print("Enter game name: ");
            String gameName = scanner.nextLine().trim();

            if (gameName.isEmpty()) {
                System.out.println("Error: Game name cannot be empty.");
                return;
            }

            int gameId = serverFacade.createGame(authToken, gameName);
            System.out.println("Game '" + gameName);
        } catch (ResponseException e) {
            System.out.println("Error creating game: " + e.getMessage());
        }
    }

    private void listGames() {
        try {
            var games = serverFacade.listGames(authToken);
            gameIds.clear(); // Clear previous game IDs
            
            if (games.isEmpty()) {
                System.out.println("No games available.");
            } else {
                System.out.println("Available games:");
                for (int i = 0; i < games.size(); i++) {
                    GameData game = games.get(i);
                    gameIds.add(game.gameID()); // Store the game ID
                    
                    // Display game info with list number
                    System.out.printf("%d. %s%n", i + 1, game.gameName());
                    System.out.printf("   White: %s%n", game.whiteUsername() != null ? game.whiteUsername() : "none");
                    System.out.printf("   Black: %s%n", game.blackUsername() != null ? game.blackUsername() : "none");
                }
            }
        } catch (ResponseException e) {
            System.out.println("Error listing games: " + e.getMessage());
        }
    }

    private void playGame() {
        if (gameIds.isEmpty()) {
            System.out.println("No games available. Please use 'list' to see available games first.");
            return;
        }

        try {
            // Get game number
            System.out.print("Enter game number: ");
            String gameNumStr = scanner.nextLine().trim();
            int gameNum;
            try {
                gameNum = Integer.parseInt(gameNumStr);
                if (gameNum < 1 || gameNum > gameIds.size()) {
                    System.out.println("Invalid game number. Please enter a number between 1 and " + gameIds.size());
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                return;
            }

            // Get color choice
            System.out.print("Enter color (WHITE/BLACK): ");
            String colorStr = scanner.nextLine().trim().toUpperCase();
            ChessGame.TeamColor color;
            try {
                color = ChessGame.TeamColor.valueOf(colorStr);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid color. Please enter WHITE or BLACK.");
                return;
            }

            // Join the game
            int gameId = gameIds.get(gameNum - 1);
            serverFacade.joinGame(authToken, gameId, color);
            ChessBoard board = serverFacade.getGameBoard(authToken, gameId);
            ChessBoardRenderer.render(board, colorStr);
            System.out.println("Playing as " + colorStr);
            
            // TODO: Transition to gameplay UI
            
        } catch (ResponseException e) {
            System.out.println(e.getMessage());
        }
    }

    private void observeGame() {
        if (gameIds.isEmpty()) {
            System.out.println("No games available. Please use 'list' to see available games first.");
            return;
        }

        try {
            // Get game number
            System.out.print("Enter game number: ");
            String gameNumStr = scanner.nextLine().trim();
            int gameNum;
            try {
                gameNum = Integer.parseInt(gameNumStr);
                if (gameNum < 1 || gameNum > gameIds.size()) {
                    System.out.println("Invalid game number. Please enter a number between 1 and " + gameIds.size());
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                return;
            }

            // Observe the game
            int gameId = gameIds.get(gameNum - 1);
            serverFacade.observeGame(authToken, gameId);
            ChessBoard board = serverFacade.getGameBoard(authToken, gameId);
            ChessBoardRenderer.render(board, "observer");
            System.out.println("Observing the game from the WHITE team's perspective.");
            
            // TODO: Transition to gameplay UI
            
        } catch (ResponseException e) {
            System.out.println("Error observing game: " + e.getMessage());
        }
    }
} 