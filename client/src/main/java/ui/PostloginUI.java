package ui;

import server.ResponseException;
import server.ServerFacade;
import java.util.Scanner;

public class PostloginUI {
    private final Scanner scanner;
    private final String username;
    private final String authToken;
    private final ServerFacade serverFacade;
    private final PreloginUI preloginUI;
    private boolean running;

    public PostloginUI(PreloginUI preloginUI, String username, String authToken, Scanner scanner) {
        this.scanner = scanner;
        this.username = username;
        this.authToken = authToken;
        this.serverFacade = new ServerFacade("http://localhost:8080");
        this.preloginUI = preloginUI;
        this.running = true;
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
        System.out.println("Create game functionality coming soon!");
    }

    private void listGames() {
        System.out.println("List games functionality coming soon!");
    }

    private void playGame() {
        System.out.println("Play game functionality coming soon!");
    }

    private void observeGame() {
        System.out.println("Observe game functionality coming soon!");
    }
} 