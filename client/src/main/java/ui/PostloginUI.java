package ui;

import server.ResponseException;
import server.ServerFacade;
import java.util.Scanner;

public class PostloginUI {
    private final Scanner scanner;
    private final String username;
    private final String authToken;
    private final ServerFacade serverFacade;
    private boolean running;

    public PostloginUI(String username, String authToken) {
        this.scanner = new Scanner(System.in);
        this.username = username;
        this.authToken = authToken;
        this.serverFacade = new ServerFacade("http://localhost:8080");
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
                case "create game" -> createGame();
                case "list games" -> listGames();
                case "play game" -> playGame();
                case "observe game" -> observeGame();
                default -> System.out.println("Unknown command. Type 'help' to see available commands.");
            }
        }
    }

    private void showHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  help         - Display this help message");
        System.out.println("  quit         - Exit the program");
        System.out.println("  logout       - Log out of your account");
        System.out.println("  create game  - Create a new game");
        System.out.println("  list games   - List all available games");
        System.out.println("  play game    - Join a game as a player");
        System.out.println("  observe game - Join a game as an observer");
    }

    private void quit() {
        System.out.println("Goodbye!");
        running = false;
        scanner.close();
    }

    private void logout() {
        try {
            serverFacade.logout(authToken);
            System.out.println("Successfully logged out!");
            running = false;
            scanner.close();
            var preloginUI = new PreloginUI();
            preloginUI.run();
        } catch (ResponseException e) {
            System.out.println("Error: " + e.getMessage());
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