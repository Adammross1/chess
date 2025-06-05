package server;

import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {
    private static ServerFacade serverFacade;
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_PASSWORD = "testPass";
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeAll
    public static void init() {
        serverFacade = new ServerFacade("http://localhost:8080");
    }

    @Test
    public void testRegisterSuccess() throws ResponseException {
        // Register a new user
        AuthData authData = serverFacade.register(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        
        // Verify the response
        assertNotNull(authData);
        assertEquals(TEST_USERNAME, authData.username());
        assertNotNull(authData.authToken());
    }

    @Test
    public void testRegisterDuplicateUser() {
        // First registration
        try {
            serverFacade.register(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        } catch (ResponseException e) {
            fail("First registration should succeed");
        }

        // Second registration with same username
        ResponseException exception = assertThrows(ResponseException.class, () -> {
            serverFacade.register(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        });

        assertEquals(403, exception.getStatusCode());
    }

    @Test
    public void testRegisterInvalidInput() {
        // Test with empty username
        ResponseException exception = assertThrows(ResponseException.class, () -> {
            serverFacade.register("", TEST_PASSWORD, TEST_EMAIL);
        });
        assertTrue(exception.getMessage().contains("failure"));

        // Test with empty password
        exception = assertThrows(ResponseException.class, () -> {
            serverFacade.register(TEST_USERNAME, "", TEST_EMAIL);
        });
        assertTrue(exception.getMessage().contains("failure"));

        // Test with empty email
        exception = assertThrows(ResponseException.class, () -> {
            serverFacade.register(TEST_USERNAME, TEST_PASSWORD, "");
        });
        assertTrue(exception.getMessage().contains("failure"));
    }

    @Test
    public void testLoginSuccess() throws ResponseException {
        // First register a user
        serverFacade.register(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        
        // Then try to login
        AuthData authData = serverFacade.login(TEST_USERNAME, TEST_PASSWORD);
        
        // Verify the response
        assertNotNull(authData);
        assertEquals(TEST_USERNAME, authData.username());
        assertNotNull(authData.authToken());
    }

    @Test
    public void testLoginInvalidCredentials() {
        // First register a user
        try {
            serverFacade.register(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        } catch (ResponseException e) {
            fail("First registration should succeed");
        }

        // Try to login with wrong password
        ResponseException exception = assertThrows(ResponseException.class, () -> {
            serverFacade.login(TEST_USERNAME, "wrongPassword");
        });
        assertEquals(401, exception.getStatusCode());

        // Try to login with non-existent username
        exception = assertThrows(ResponseException.class, () -> {
            serverFacade.login("nonexistentUser", TEST_PASSWORD);
        });
        assertEquals(401, exception.getStatusCode());
    }

    @Test
    public void testLoginInvalidInput() {
        // Test with empty username
        ResponseException exception = assertThrows(ResponseException.class, () -> {
            serverFacade.login("", TEST_PASSWORD);
        });
        assertTrue(exception.getMessage().contains("failure"));

        // Test with empty password
        exception = assertThrows(ResponseException.class, () -> {
            serverFacade.login(TEST_USERNAME, "");
        });
        assertTrue(exception.getMessage().contains("failure"));
    }

    @Test
    public void testLogoutSuccess() throws ResponseException {
        // First register and get auth token
        AuthData authData = serverFacade.register(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        
        // Then logout
        serverFacade.logout(authData.authToken());
        
        // Try to logout again with the same token (should fail)
        ResponseException exception = assertThrows(ResponseException.class, () -> {
            serverFacade.logout(authData.authToken());
        });
        assertEquals(401, exception.getStatusCode());
    }

    @Test
    public void testLogoutInvalidToken() {
        ResponseException exception = assertThrows(ResponseException.class, () -> {
            serverFacade.logout("invalid-token");
        });
        assertEquals(401, exception.getStatusCode());
    }
} 