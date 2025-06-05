package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.UserDAO;
import dataaccess.MySQLUserDAO;
import model.AuthData;
import model.UserData;
import service.requests.LoginRequest;
import service.requests.LogoutRequest;
import service.requests.RegisterRequest;
import service.results.LoginResult;
import service.results.LogoutResult;
import service.results.RegisterResult;

import java.util.UUID;
import java.util.logging.Logger;

public class UserService {
    private final UserDAO userDAO;
    private final AuthDAO authDAO;
    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

    public UserService(UserDAO userDAO, AuthDAO authDAO) {
        this.userDAO = userDAO;
        this.authDAO = authDAO;
    }

    public RegisterResult register(RegisterRequest request) throws DataAccessException {
        if (userDAO.getUser(request.username()) != null) {
            throw new DataAccessException("Error: username already taken");
        }

        UserData user = new UserData(request.username(), request.password(), request.email());
        userDAO.createUser(user);

        String authToken = UUID.randomUUID().toString();
        AuthData authData = new AuthData(authToken, request.username());
        authDAO.createAuth(authData);

        return new RegisterResult(request.username(), authToken);
    }

    public LoginResult login(LoginRequest request) throws DataAccessException {
        LOGGER.info("Attempting login for user: " + request.username());
        UserData user = userDAO.getUser(request.username());
        if (user == null) {
            LOGGER.warning("Login failed: User not found: " + request.username());
            throw new DataAccessException("Error: unauthorized");
        }

        // Always use MySQL password verification
        if (!((MySQLUserDAO) userDAO).verifyPassword(request.username(), request.password())) {
            LOGGER.warning("Login failed: Invalid password for user: " + request.username());
            throw new DataAccessException("Error: unauthorized");
        }

        String authToken = UUID.randomUUID().toString();
        LOGGER.info("Generated new auth token for user " + request.username() + ": [" + authToken + "]");
        AuthData authData = new AuthData(authToken, request.username());
        authDAO.createAuth(authData);
        LOGGER.info("Auth token created successfully for user: " + request.username());

        return new LoginResult(request.username(), authToken);
    }

    public LogoutResult logout(LogoutRequest request) throws DataAccessException {
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
            throw new DataAccessException("Error: unauthorized");
        }

        authDAO.deleteAuth(request.authToken());

        return new LogoutResult();
    }
}
