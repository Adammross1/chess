package dataaccess;

import model.UserData;

/**
 * Interface for user data access operations
 */
public interface UserDAO {
    /**
     * Clears all user data
     * @throws DataAccessException if an error occurs
     */
    void clear() throws DataAccessException;

    /**
     * Creates a new user
     * @param user the user to create
     * @throws DataAccessException if an error occurs or user already exists
     */
    void createUser(UserData user) throws DataAccessException;

    /**
     * Gets a user by username
     * @param username the username to look up
     * @return the user data, or null if not found
     * @throws DataAccessException if an error occurs
     */
    UserData getUser(String username) throws DataAccessException;

    /**
     * Verifies if the provided password matches the stored password
     * @param username the username to check
     * @param password the password to verify
     * @return true if the password matches, false otherwise
     * @throws DataAccessException if an error occurs
     */
    boolean verifyPassword(String username, String password) throws DataAccessException;
}
