package service;

import dataaccess.*;
import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.requests.CreateGameRequest;
import service.requests.JoinGameRequest;
import service.requests.ListGamesRequest;
import service.results.CreateGameResult;
import service.results.ListGamesResult;
import com.google.gson.Gson;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {
    private GameService gameService;
    private String validAuthToken;
    private GameDAO gameDAO;
    private AuthDAO authDAO;
    private UserDAO userDAO;
    private Gson gson;

    @BeforeEach
    public void setUp() throws DataAccessException {
        gson = new Gson();
        gameDAO = new MySQLGameDAO(gson);
        authDAO = new MySQLAuthDAO();
        userDAO = new MySQLUserDAO();

        gameDAO.clear();
        authDAO.clear();
        userDAO.clear();

        userDAO.createUser(new model.UserData("testUser", "password", "test@email.com"));

        gameService = new GameService(gameDAO, authDAO);

        validAuthToken = "valid-auth-token";
        authDAO.createAuth(new AuthData(validAuthToken, "testUser"));
    }

    @Test
    public void listGamesPositive() throws DataAccessException {
        CreateGameRequest createRequest = new CreateGameRequest(validAuthToken, "Test Game");
        gameService.createGame(createRequest);

        ListGamesRequest listRequest = new ListGamesRequest(validAuthToken);
        ListGamesResult result = gameService.listGames(listRequest);

        assertNotNull(result.games());
        assertFalse(result.games().isEmpty());
        assertEquals(1, result.games().size());
        assertEquals("Test Game", result.games().get(0).gameName());
    }

    @Test
    public void listGamesNegative() {
        ListGamesRequest listRequest = new ListGamesRequest("invalid-token");

        try {
            gameService.listGames(listRequest);
            fail("Should have thrown DataAccessException");
        } catch (DataAccessException e) {
            assertTrue(e.getMessage().contains("unauthorized"));
        }
    }

    @Test
    public void createGamePositive() throws DataAccessException {
        CreateGameRequest request = new CreateGameRequest(validAuthToken, "Test Game");
        CreateGameResult result = gameService.createGame(request);

        assertTrue(result.gameID() > 0);
        
        ListGamesRequest listRequest = new ListGamesRequest(validAuthToken);
        ListGamesResult listResult = gameService.listGames(listRequest);
        assertEquals(1, listResult.games().size());
        assertEquals("Test Game", listResult.games().get(0).gameName());
    }

    @Test
    public void createGameNegative() {
        CreateGameRequest request = new CreateGameRequest("invalid-token", "Test Game");

        try {
            gameService.createGame(request);
            fail("Should have thrown DataAccessException");
        } catch (DataAccessException e) {
            assertTrue(e.getMessage().contains("unauthorized"));
        }
    }

    @Test
    public void joinGamePositive() throws DataAccessException {
        CreateGameRequest createRequest = new CreateGameRequest(validAuthToken, "Test Game");
        CreateGameResult createResult = gameService.createGame(createRequest);

        JoinGameRequest joinRequest = new JoinGameRequest(validAuthToken, createResult.gameID(), "WHITE");
        gameService.joinGame(joinRequest);

        ListGamesRequest listRequest = new ListGamesRequest(validAuthToken);
        ListGamesResult listResult = gameService.listGames(listRequest);
        assertEquals(1, listResult.games().size());
        assertEquals("testUser", listResult.games().get(0).whiteUsername());
        assertNull(listResult.games().get(0).blackUsername());
    }

    @Test
    public void joinGameNegative() throws DataAccessException {
        CreateGameRequest createRequest = new CreateGameRequest(validAuthToken, "Test Game");
        CreateGameResult createResult = gameService.createGame(createRequest);

        JoinGameRequest joinRequest = new JoinGameRequest(validAuthToken, createResult.gameID(), "WHITE");
        gameService.joinGame(joinRequest);

        try {
            gameService.joinGame(joinRequest);
            fail("Should have thrown DataAccessException");
        } catch (DataAccessException e) {
            assertTrue(e.getMessage().contains("already taken"));
        }
    }
}
