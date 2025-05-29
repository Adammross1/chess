package dataaccess;

import model.UserData;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class MySQLUserDAOTest {
    private MySQLUserDAO userDAO;

    @BeforeEach
    void setUp() throws DataAccessException {
        userDAO = new MySQLUserDAO();
        userDAO.clear();
    }

    @Test
    @DisplayName("Positive: Create user successfully")
    void createUserSuccess() throws DataAccessException {
        UserData user = new UserData("testUser", "password123", "test@email.com");
        userDAO.createUser(user);
        
        UserData retrievedUser = userDAO.getUser("testUser");
        assertNotNull(retrievedUser);
        assertEquals("testUser", retrievedUser.username());
    }

    @Test
    @DisplayName("Negative: Create duplicate user")
    void createUserDuplicate() throws DataAccessException {
        UserData user = new UserData("testUser", "password123", "test@email.com");
        userDAO.createUser(user);
        
        assertThrows(DataAccessException.class, () -> {
            userDAO.createUser(user);
        });
    }

    @Test
    @DisplayName("Positive: Get existing user")
    void getUserSuccess() throws DataAccessException {
        UserData user = new UserData("testUser", "password123", "test@email.com");
        userDAO.createUser(user);
        
        UserData retrievedUser = userDAO.getUser("testUser");
        assertNotNull(retrievedUser);
        assertEquals("testUser", retrievedUser.username());
    }

    @Test
    @DisplayName("Negative: Get non-existent user")
    void getUserNonExistent() throws DataAccessException {
        UserData retrievedUser = userDAO.getUser("nonExistentUser");
        assertNull(retrievedUser);
    }

    @Test
    @DisplayName("Positive: Verify correct password")
    void verifyPasswordSuccess() throws DataAccessException {
        UserData user = new UserData("testUser", "password123", "test@email.com");
        userDAO.createUser(user);
        
        assertTrue(userDAO.verifyPassword("testUser", "password123"));
    }

    @Test
    @DisplayName("Negative: Verify incorrect password")
    void verifyPasswordIncorrect() throws DataAccessException {
        UserData user = new UserData("testUser", "password123", "test@email.com");
        userDAO.createUser(user);
        
        assertFalse(userDAO.verifyPassword("testUser", "wrongPassword"));
    }

    @Test
    @DisplayName("Positive: Clear users")
    void clearSuccess() throws DataAccessException {
        UserData user1 = new UserData("user1", "pass1", "email1");
        UserData user2 = new UserData("user2", "pass2", "email2");
        userDAO.createUser(user1);
        userDAO.createUser(user2);
        
        userDAO.clear();
        
        assertNull(userDAO.getUser("user1"));
        assertNull(userDAO.getUser("user2"));
    }
} 