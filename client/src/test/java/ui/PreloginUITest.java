package ui;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

public class PreloginUITest {
    
    @Test
    public void testHelpCommand() {
        // Prepare input and output streams
        String input = "help\nquit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        
        // Save original streams
        PrintStream originalOut = System.out;
        System.setOut(printStream);
        System.setIn(inputStream);
        
        // Run the UI
        PreloginUI ui = new PreloginUI();
        ui.run();
        
        // Restore original streams
        System.setOut(originalOut);
        
        // Get the output
        String output = outputStream.toString();
        
        // Verify help text is displayed correctly
        assertTrue(output.contains("Available commands:"));
        assertTrue(output.contains("help     - Display this help message"));
        assertTrue(output.contains("quit     - Exit the program"));
        assertTrue(output.contains("login    - Login to your account"));
        assertTrue(output.contains("register - Create a new account"));
    }
    
    @Test
    public void testQuitCommand() {
        // Prepare input and output streams
        String input = "quit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        
        // Save original streams
        PrintStream originalOut = System.out;
        System.setOut(printStream);
        System.setIn(inputStream);
        
        // Run the UI
        PreloginUI ui = new PreloginUI();
        ui.run();
        
        // Restore original streams
        System.setOut(originalOut);
        
        // Get the output
        String output = outputStream.toString();
        
        // Verify quit message is displayed
        assertTrue(output.contains("Goodbye!"));
    }
    
    @Test
    public void testInvalidCommand() {
        // Prepare input and output streams
        String input = "invalid\nquit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        
        // Save original streams
        PrintStream originalOut = System.out;
        System.setOut(printStream);
        System.setIn(inputStream);
        
        // Run the UI
        PreloginUI ui = new PreloginUI();
        ui.run();
        
        // Restore original streams
        System.setOut(originalOut);
        
        // Get the output
        String output = outputStream.toString();
        
        // Verify invalid command message is displayed
        assertTrue(output.contains("Unknown command"));
    }
} 