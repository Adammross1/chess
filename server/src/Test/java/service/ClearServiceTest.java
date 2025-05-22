package service;

import dataaccess.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.requests.ClearAppRequest;
import service.requests.CreateGameRequest;
import service.requests.RegisterRequest;
import service.results.CreateGameResult;
import service.results.RegisterResult;

import static org.junit.jupiter.api.Assertions.*;

public class ClearServiceTest {
    private ClearService clearService;
    private UserService userService;
    private GameService gameService;
    private AuthDAO authDAO;
    private UserDAO userDAO;
    private GameDAO gameDAO;

    @BeforeEach
    public void setUp() throws DataAccessException {
        userDAO = new MemoryUserDAO();
        authDAO = new MemoryAuthDAO();
        gameDAO = new MemoryGameDAO();

        userDAO.clear();
        authDAO.clear();
        gameDAO.clear();

        clearService = new ClearService(userDAO, gameDAO, authDAO);
        userService = new UserService(userDAO, authDAO);
        gameService = new GameService(gameDAO, authDAO);
    }

    @Test
    public void clearPositive() throws DataAccessException {
        RegisterRequest registerRequest = new RegisterRequest("testUser", "password", "email");
        RegisterResult registerResult = userService.register(registerRequest);

        CreateGameRequest createGameRequest = new CreateGameRequest(registerResult.authToken(), "Test Game");
        CreateGameResult createGameResult = gameService.createGame(createGameRequest);

        assertNotNull(userDAO.getUser("testUser"));
        assertNotNull(authDAO.getAuth(registerResult.authToken()));
        assertNotNull(gameDAO.getGame(createGameResult.gameID()));

        clearService.clearApplication(new ClearAppRequest());

        assertNull(userDAO.getUser("testUser"));
        assertNull(authDAO.getAuth(registerResult.authToken()));
        assertNull(gameDAO.getGame(createGameResult.gameID()));
    }

    @Test
    public void clearNegative() throws DataAccessException {
        clearService.clearApplication(new ClearAppRequest());

        clearService.clearApplication(new ClearAppRequest());

        assertNull(userDAO.getUser("testUser"));
        assertNull(authDAO.getAuth("any-token"));
        assertNull(gameDAO.getGame(1));
    }
}
