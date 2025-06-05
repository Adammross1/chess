package server;

import model.AuthData;
import model.UserData;
import model.GameData;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {
    private static ServerFacade serverFacade;
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_PASSWORD = "testPass";
    private static final String TEST_EMAIL = "test@example.com";
    private String authToken;

    @BeforeAll
    public static void init() {
        serverFacade = new ServerFacade("http://localhost:8080");
    }

    @BeforeEach
    public void setUp() throws ResponseException {
        // Register a test user and get auth token
        authToken = serverFacade.register(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL).authToken();
    }

    @AfterEach
    public void tearDown() throws ResponseException {
        // Clear the database after each test
        serverFacade.clear();
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

    @Test
    public void testCreateGameSuccess() throws ResponseException {
        // First register and login to get an auth token
        serverFacade.register(TEST_USERNAME, TEST_PASSWORD, TEST_EMAIL);
        AuthData authData = serverFacade.login(TEST_USERNAME, TEST_PASSWORD);

        // Create a game
        String gameName = "myNewGame";
        int gameId = serverFacade.createGame(authData.authToken(), gameName);

        // Verify the response
        assertTrue(gameId > 0, "Game ID should be a positive integer");
    }

    @Test
    public void testCreateGameUnauthorized() {
        // Attempt to create a game with an invalid auth token
        ResponseException exception = assertThrows(ResponseException.class, () -> {
            serverFacade.createGame("invalid-token", "someGame");
        });

        assertEquals(401, exception.getStatusCode(), "Should return 401 for unauthorized create game");
    }

    @Test
    public void listGamesPositive() throws ResponseException {
        // Create a test game
        String gameName = "Test Game";
        int gameId = serverFacade.createGame(authToken, gameName);
        assertTrue(gameId > 0, "Game ID should be positive");

        // List games
        var games = serverFacade.listGames(authToken);
        assertNotNull(games, "Games list should not be null");
        assertFalse(games.isEmpty(), "Games list should not be empty");
        assertEquals(1, games.size(), "Should have exactly one game");
        
        GameData game = games.get(0);
        assertEquals(gameName, game.gameName(), "Game name should match");
        assertEquals(gameId, game.gameID(), "Game ID should match");
        assertNull(game.whiteUsername(), "White username should be null for new game");
        assertNull(game.blackUsername(), "Black username should be null for new game");
    }

    @Test
    public void listGamesEmpty() throws ResponseException {
        // List games without creating any
        var games = serverFacade.listGames(authToken);
        assertNotNull(games, "Games list should not be null");
        assertTrue(games.isEmpty(), "Games list should be empty");
    }

    @Test
    public void listGamesUnauthorized() {
        // Try to list games with invalid auth token
        assertThrows(ResponseException.class, () -> {
            serverFacade.listGames("invalid-token");
        }, "Should throw ResponseException for invalid auth token");
    }

    @Test
    public void listGamesMultiple() throws ResponseException {
        // Create multiple games
        String[] gameNames = {"Game 1", "Game 2", "Game 3"};
        for (String name : gameNames) {
            serverFacade.createGame(authToken, name);
        }

        // List games
        var games = serverFacade.listGames(authToken);
        assertNotNull(games, "Games list should not be null");
        assertEquals(gameNames.length, games.size(), "Should have correct number of games");

        // Verify each game
        for (int i = 0; i < gameNames.length; i++) {
            GameData game = games.get(i);
            assertEquals(gameNames[i], game.gameName(), "Game name should match");
            assertNull(game.whiteUsername(), "White username should be null for new game");
            assertNull(game.blackUsername(), "Black username should be null for new game");
        }
    }
} 