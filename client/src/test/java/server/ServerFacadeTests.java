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
} 