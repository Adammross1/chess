package service;

import dataaccess.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.requests.LoginRequest;
import service.requests.LogoutRequest;
import service.requests.RegisterRequest;
import service.results.LoginResult;
import service.results.RegisterResult;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
    private UserService userService;
    private UserDAO userDAO;
    private AuthDAO authDAO;

    @BeforeEach
    public void setUp() throws DataAccessException {
        userDAO = new MySQLUserDAO();
        authDAO = new MySQLAuthDAO();

        userDAO.clear();
        authDAO.clear();

        userService = new UserService(userDAO, authDAO);
    }

    @Test
    public void registerPositive() throws DataAccessException {
        RegisterRequest request = new RegisterRequest("testUser", "password123", "test@example.com");
        RegisterResult result = userService.register(request);

        assertEquals("testUser", result.username());
        assertNotNull(result.authToken());
        assertFalse(result.authToken().isEmpty());
    }

    @Test
    public void registerNegative() {
        RegisterRequest request = new RegisterRequest("testUser", "password123", "test@example.com");

        try {
            userService.register(request);
            userService.register(request);
            fail("Should have thrown DataAccessException");
        } catch (DataAccessException e) {
            assertTrue(e.getMessage().contains("already taken"));
        }
    }

    @Test
    public void loginPositive() throws DataAccessException {
        RegisterRequest registerRequest = new RegisterRequest("testUser", "password123", "test@example.com");
        userService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest("testUser", "password123");
        LoginResult result = userService.login(loginRequest);

        assertEquals("testUser", result.username());
        assertNotNull(result.authToken());
        assertFalse(result.authToken().isEmpty());
    }

    @Test
    public void loginNegative() {
        RegisterRequest registerRequest = new RegisterRequest("testUser", "password123", "test@example.com");

        try {
            userService.register(registerRequest);

            LoginRequest loginRequest = new LoginRequest("testUser", "wrongPassword");
            userService.login(loginRequest);
            fail("Should have thrown DataAccessException");
        } catch (DataAccessException e) {
            assertTrue(e.getMessage().contains("unauthorized"));
        }
    }

    @Test
    public void logoutPositive() throws DataAccessException {
        RegisterRequest registerRequest = new RegisterRequest("testUser", "password123", "test@example.com");
        RegisterResult registerResult = userService.register(registerRequest);

        LogoutRequest logoutRequest = new LogoutRequest(registerResult.authToken());
        userService.logout(logoutRequest);

        try {
            userService.logout(logoutRequest);
            fail("Should have thrown DataAccessException (token already invalidated)");
        } catch (DataAccessException e) {
            assertTrue(e.getMessage().contains("unauthorized"));
        }
    }

    @Test
    public void logoutNegative() {
        LogoutRequest logoutRequest = new LogoutRequest("invalidAuthToken");

        try {
            userService.logout(logoutRequest);
            fail("Should have thrown DataAccessException");
        } catch (DataAccessException e) {
            assertTrue(e.getMessage().contains("unauthorized"));
        }
    }
}
