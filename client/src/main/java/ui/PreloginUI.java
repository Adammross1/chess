package ui;

import model.AuthData;
import server.ResponseException;
import server.ServerFacade;

import java.util.Scanner;

public class PreloginUI {
    private final Scanner scanner;
    private final ServerFacade serverFacade;
    private boolean running;

    public PreloginUI() {
        this.scanner = new Scanner(System.in);
        this.serverFacade = new ServerFacade("http://localhost:8080");
        this.running = true;
    }

    public void run() {
        System.out.println("Welcome to Chess! Type 'help' to see available commands.");
        
        while (running) {
            System.out.print("\n[LOGGED_OUT] >>> ");
            String input = scanner.nextLine().trim().toLowerCase();
            
            switch (input) {
                case "help" -> showHelp();
                case "quit" -> quit();
                case "login" -> login();
                case "register" -> register();
                default -> System.out.println("Unknown command. Type 'help' to see available commands.");
            }
        }
    }

    private void showHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  help     - Display this help message");
        System.out.println("  quit     - Exit the program");
        System.out.println("  login    - Login to your account");
        System.out.println("  register - Create a new account");
    }

    private void quit() {
        System.out.println("Goodbye!");
        running = false;
        scanner.close();
    }

    private void login() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            if (username.isEmpty() || password.isEmpty()) {
                System.out.println("Error: Username and password are required");
                return;
            }

            AuthData authData = serverFacade.login(username, password);
            System.out.println("Login successful!");
            
            // Transition to post-login UI
            var postloginUI = new PostloginUI(authData.username(), authData.authToken());
            postloginUI.run();
        } catch (ResponseException e) {
            if (e.getStatusCode() == 401) {
                System.out.println("Error: Invalid username or password");
            } else {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void register() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                System.out.println("Error: All fields are required");
                return;
            }

            AuthData authData = serverFacade.register(username, password, email);
            System.out.println("Registration successful!");
            
            // Transition to post-login UI
            var postloginUI = new PostloginUI(authData.username(), authData.authToken());
            postloginUI.run();
        } catch (ResponseException e) {
            if (e.getStatusCode() == 403) {
                System.out.println("Error: Username already taken");
            } else {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
} 