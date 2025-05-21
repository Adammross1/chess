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

    @BeforeEach
    public void setUp() throws DataAccessException {
        // Create in-memory implementations of DAOs
        UserDAO userDAO = new MemoryUserDAO();
        AuthDAO authDAO = new MemoryAuthDAO();

        // Clear the data to start fresh
        userDAO.clear();
        authDAO.clear();

        // Create a new UserService with the DAOs
        userService = new UserService(userDAO, authDAO);
    }

    @Test
    public void registerPositive() throws DataAccessException {
        // Test successful registration
        RegisterRequest request = new RegisterRequest("testUser", "password123", "test@example.com");
        RegisterResult result = userService.register(request);

        // Verify the result contains a username and auth token
        assertEquals("testUser", result.username());
        assertNotNull(result.authToken());
        assertFalse(result.authToken().isEmpty());
    }

    @Test
    public void registerNegative() {
        // Test registration with duplicate username
        RegisterRequest request = new RegisterRequest("testUser", "password123", "test@example.com");

        try {
            // Register once successfully
            userService.register(request);

            // Try to register again with the same username (should throw exception)
            userService.register(request);
            fail("Should have thrown AlreadyTakenException");
        } catch (AlreadyTakenException e) {
            // Expected exception
            assertTrue(true);
        } catch (DataAccessException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void loginPositive() throws DataAccessException {
        // Register a user first
        RegisterRequest registerRequest = new RegisterRequest("testUser", "password123", "test@example.com");
        userService.register(registerRequest);

        // Test successful login
        LoginRequest loginRequest = new LoginRequest("testUser", "password123");
        LoginResult result = userService.login(loginRequest);

        // Verify the result contains a username and auth token
        assertEquals("testUser", result.username());
        assertNotNull(result.authToken());
        assertFalse(result.authToken().isEmpty());
    }

    @Test
    public void loginNegative() {
        // Register a user first
        RegisterRequest registerRequest = new RegisterRequest("testUser", "password123", "test@example.com");

        try {
            userService.register(registerRequest);

            // Test login with incorrect password
            LoginRequest loginRequest = new LoginRequest("testUser", "wrongPassword");
            userService.login(loginRequest);
            fail("Should have thrown UnauthorizedException");
        } catch (UnauthorizedException e) {
            // Expected exception
            assertTrue(true);
        } catch (DataAccessException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void logoutPositive() throws DataAccessException {
        // Register a user first to get an auth token
        RegisterRequest registerRequest = new RegisterRequest("testUser", "password123", "test@example.com");
        RegisterResult registerResult = userService.register(registerRequest);

        // Test successful logout
        LogoutRequest logoutRequest = new LogoutRequest(registerResult.authToken());
        userService.logout(logoutRequest);

        // Try to logout again with the same token (should throw exception since token is invalidated)
        try {
            userService.logout(logoutRequest);
            fail("Should have thrown UnauthorizedException (token already invalidated)");
        } catch (UnauthorizedException e) {
            // Expected exception after trying to use an invalidated token
            assertTrue(true);
        }
    }

    @Test
    public void logoutNegative() {
        // Test logout with invalid auth token
        LogoutRequest logoutRequest = new LogoutRequest("invalidAuthToken");

        try {
            userService.logout(logoutRequest);
            fail("Should have thrown UnauthorizedException");
        } catch (UnauthorizedException e) {
            // Expected exception
            assertTrue(true);
        } catch (DataAccessException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}
