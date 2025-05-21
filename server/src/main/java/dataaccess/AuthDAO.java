package dataaccess;

import model.AuthData;

/**
 * Interface for authentication data access operations
 */
public interface AuthDAO {
    /**
     * Clears all authentication data
     * @throws DataAccessException if an error occurs
     */
    void clear() throws DataAccessException;

    /**
     * Creates a new authentication token
     * @param authData the authentication data to create
     * @return the created authentication data
     * @throws DataAccessException if an error occurs
     */
    AuthData createAuth(AuthData authData) throws DataAccessException;

    /**
     * Gets authentication data by token
     * @param authToken the token to look up
     * @return the authentication data, or null if not found
     * @throws DataAccessException if an error occurs
     */
    AuthData getAuth(String authToken) throws DataAccessException;

    /**
     * Deletes authentication data by token
     * @param authToken the token to delete
     * @throws DataAccessException if an error occurs or token not found
     */
    void deleteAuth(String authToken) throws DataAccessException;
}
