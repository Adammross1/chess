package service;

import dataaccess.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.requests.ClearAppRequest;
import service.requests.CreateGameRequest;
import service.requests.RegisterRequest;
import service.results.CreateGameResult;
import service.results.RegisterResult;
import com.google.gson.Gson;

import static org.junit.jupiter.api.Assertions.*;

public class ClearServiceTest {
    private ClearService clearService;
    private UserService userService;
    private GameService gameService;
    private AuthDAO authDAO;
    private UserDAO userDAO;
    private GameDAO gameDAO;
    private Gson gson;

    @BeforeEach
    public void setUp() throws DataAccessException {
        gson = new Gson();
        userDAO = new MySQLUserDAO();
        authDAO = new MySQLAuthDAO();
        gameDAO = new MySQLGameDAO(gson);

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
}
