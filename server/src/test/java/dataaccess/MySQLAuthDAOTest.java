package dataaccess;

import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class MySQLAuthDAOTest {
    private MySQLAuthDAO authDAO;
    private MySQLUserDAO userDAO;

    @BeforeEach
    void setUp() throws DataAccessException {
        userDAO = new MySQLUserDAO();
        userDAO.clear();
        authDAO = new MySQLAuthDAO();
        authDAO.clear();
    }

    @Test
    @DisplayName("Positive: Create auth token successfully")
    void createAuthSuccess() throws DataAccessException {
        userDAO.createUser(new UserData("testUser", "password", "email"));
        AuthData authData = new AuthData(null, "testUser");
        AuthData createdAuth = authDAO.createAuth(authData);
        
        assertNotNull(createdAuth);
        assertNotNull(createdAuth.authToken());
        assertEquals("testUser", createdAuth.username());
    }

    @Test
    @DisplayName("Negative: Create auth token with null username")
    void createAuthNullUsername() {
        AuthData authData = new AuthData(null, null);
        assertThrows(DataAccessException.class, () -> {
            authDAO.createAuth(authData);
        });
    }

    @Test
    @DisplayName("Positive: Get existing auth token")
    void getAuthSuccess() throws DataAccessException {
        userDAO.createUser(new UserData("testUser", "password", "email"));
        AuthData authData = new AuthData(null, "testUser");
        AuthData createdAuth = authDAO.createAuth(authData);
        
        AuthData retrievedAuth = authDAO.getAuth(createdAuth.authToken());
        assertNotNull(retrievedAuth);
        assertEquals(createdAuth.authToken(), retrievedAuth.authToken());
        assertEquals("testUser", retrievedAuth.username());
    }

    @Test
    @DisplayName("Negative: Get non-existent auth token")
    void getAuthNonExistent() throws DataAccessException {
        AuthData retrievedAuth = authDAO.getAuth("nonExistentToken");
        assertNull(retrievedAuth);
    }

    @Test
    @DisplayName("Positive: Delete existing auth token")
    void deleteAuthSuccess() throws DataAccessException {
        userDAO.createUser(new UserData("testUser", "password", "email"));
        AuthData authData = new AuthData(null, "testUser");
        AuthData createdAuth = authDAO.createAuth(authData);
        
        authDAO.deleteAuth(createdAuth.authToken());
        
        AuthData retrievedAuth = authDAO.getAuth(createdAuth.authToken());
        assertNull(retrievedAuth);
    }

    @Test
    @DisplayName("Negative: Delete non-existent auth token")
    void deleteAuthNonExistent() {
        assertThrows(DataAccessException.class, () -> {
            authDAO.deleteAuth("nonExistentToken");
        });
    }

    @Test
    @DisplayName("Positive: Clear auth tokens")
    void clearSuccess() throws DataAccessException {
        userDAO.createUser(new UserData("user1", "password1", "email1"));
        userDAO.createUser(new UserData("user2", "password2", "email2"));
        AuthData auth1 = new AuthData(null, "user1");
        AuthData auth2 = new AuthData(null, "user2");
        AuthData createdAuth1 = authDAO.createAuth(auth1);
        AuthData createdAuth2 = authDAO.createAuth(auth2);
        
        authDAO.clear();
        
        assertNull(authDAO.getAuth(createdAuth1.authToken()));
        assertNull(authDAO.getAuth(createdAuth2.authToken()));
    }
} 