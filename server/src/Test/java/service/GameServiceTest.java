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

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {
    private GameService gameService;
    private String validAuthToken;

    @BeforeEach
    public void setUp() throws DataAccessException {
        GameDAO gameDAO = new MemoryGameDAO();
        AuthDAO authDAO = new MemoryAuthDAO();
        UserDAO userDAO = new MemoryUserDAO();

        gameDAO.clear();
        authDAO.clear();
        userDAO.clear();

        gameService = new GameService(gameDAO, authDAO);

        validAuthToken = "valid-auth-token";
        authDAO.createAuth(new AuthData(validAuthToken, "testUser"));
    }

    @Test
    public void listGamesPositive() throws DataAccessException {
        CreateGameRequest createRequest = new CreateGameRequest("Test Game", validAuthToken);
        gameService.createGame(createRequest);

        ListGamesRequest listRequest = new ListGamesRequest(validAuthToken);
        ListGamesResult result = gameService.listGames(listRequest);

        assertNotNull(result.games());
        assertFalse(result.games().isEmpty());
        assertEquals(1, result.games().size());
        assertEquals("Test Game", result.games().getFirst().gameName());
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
        CreateGameRequest request = new CreateGameRequest("Test Game", validAuthToken);
        CreateGameResult result = gameService.createGame(request);

        assertTrue(result.gameID() > 0);
    }

    @Test
    public void createGameNegative() {
        CreateGameRequest request = new CreateGameRequest("Test Game", "invalid-token");

        try {
            gameService.createGame(request);
            fail("Should have thrown DataAccessException");
        } catch (DataAccessException e) {
            assertTrue(e.getMessage().contains("unauthorized"));
        }
    }

    @Test
    public void joinGamePositive() throws DataAccessException {
        CreateGameRequest createRequest = new CreateGameRequest("Test Game", validAuthToken);
        CreateGameResult createResult = gameService.createGame(createRequest);

        JoinGameRequest joinRequest = new JoinGameRequest("WHITE", createResult.gameID(), validAuthToken);
        gameService.joinGame(joinRequest);

        ListGamesRequest listRequest = new ListGamesRequest(validAuthToken);
        ListGamesResult listResult = gameService.listGames(listRequest);

        assertEquals("testUser", listResult.games().getFirst().whiteUsername());
        assertNull(listResult.games().getFirst().blackUsername());
    }

    @Test
    public void joinGameNegative() throws DataAccessException {
        CreateGameRequest createRequest = new CreateGameRequest("Test Game", validAuthToken);
        CreateGameResult createResult = gameService.createGame(createRequest);

        JoinGameRequest joinRequest = new JoinGameRequest("WHITE", createResult.gameID(), validAuthToken);
        gameService.joinGame(joinRequest);

        try {
            gameService.joinGame(joinRequest);
            fail("Should have thrown DataAccessException");
        } catch (DataAccessException e) {
            assertTrue(e.getMessage().contains("already taken"));
        }
    }
}
