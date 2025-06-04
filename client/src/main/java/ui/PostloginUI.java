package ui;

import java.util.Scanner;

public class PostloginUI {
    private final Scanner scanner;
    private final String username;
    private final String authToken;
    private boolean running;

    public PostloginUI(String username, String authToken) {
        this.scanner = new Scanner(System.in);
        this.username = username;
        this.authToken = authToken;
        this.running = true;
    }

    public void run() {
        System.out.println("Welcome " + username + "!");
        // TODO: Implement post-login menu
    }
} 