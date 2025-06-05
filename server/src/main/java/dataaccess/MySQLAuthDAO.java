package dataaccess;

import model.AuthData;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

public class MySQLAuthDAO implements AuthDAO {
    private static final Logger LOGGER = Logger.getLogger(MySQLAuthDAO.class.getName());

    public MySQLAuthDAO() throws DataAccessException {
        LOGGER.info("Initializing MySQLAuthDAO");
        DatabaseManager.initializeDatabase();
        LOGGER.info("MySQLAuthDAO initialized");
    }

    @Override
    public void clear() throws DataAccessException {
        LOGGER.info("Clearing auth table");
        var statement = "DELETE FROM auth";
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction
            LOGGER.info("Transaction started for clearing auth table");

            try (var preparedStatement = conn.prepareStatement(statement)) {
                LOGGER.info("Executing DELETE FROM auth statement");
                preparedStatement.executeUpdate();
                LOGGER.info("DELETE FROM auth statement executed successfully");
            }

            conn.commit(); // Commit transaction
            LOGGER.info("Transaction committed for clearing auth table");
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error clearing auth table: " + ex.getMessage());
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction
                    LOGGER.warning("Transaction rolled back for clearing auth table");
                } catch (SQLException rollbackEx) {
                    LOGGER.severe("Error during rollback: " + rollbackEx.getMessage());
                }
            }
            throw new DataAccessException("failed to clear auth table", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close(); // Close connection
                    LOGGER.info("Database connection closed after clearing auth table");
                } catch (SQLException closeEx) {
                    LOGGER.severe("Error closing connection after clearing auth table: " + closeEx.getMessage());
                }
            }
        }
        LOGGER.info("Auth table cleared");
    }

    @Override
    public AuthData createAuth(AuthData authData) throws DataAccessException {
        LOGGER.info("Creating auth token for user: " + authData.username());
        var statement = "INSERT INTO auth (auth_token, username) VALUES (?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction
            LOGGER.info("Transaction started for creating auth token for user: " + authData.username());

            String authToken = authData.authToken();
            if (authToken == null || authToken.isEmpty()) {
                authToken = UUID.randomUUID().toString();
                LOGGER.info("Generated new auth token: [" + authToken + "]");
            } else {
                LOGGER.info("Using provided auth token: [" + authToken + "]");
            }
            
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.setString(1, authToken);
                preparedStatement.setString(2, authData.username());
                LOGGER.info("Executing INSERT statement for auth token: [" + authToken + "]");
                preparedStatement.executeUpdate();
                LOGGER.info("INSERT statement executed successfully for auth token: [" + authToken + "]");
            }

            conn.commit(); // Commit transaction
            LOGGER.info("Transaction committed for creating auth token for user: " + authData.username());
            return new AuthData(authToken, authData.username());
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error creating auth token for user " + authData.username() + ": " + ex.getMessage());
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction
                    LOGGER.warning("Transaction rolled back for creating auth token for user: " + authData.username());
                } catch (SQLException rollbackEx) {
                    LOGGER.severe("Error during rollback: " + rollbackEx.getMessage());
                }
            }
            throw new DataAccessException("failed to create auth token", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close(); // Close connection
                    LOGGER.info("Database connection closed after creating auth token for user: " + authData.username());
                } catch (SQLException closeEx) {
                    LOGGER.severe("Error closing connection after creating auth token for user: " + closeEx.getMessage());
                }
            }
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        LOGGER.info("Getting auth token: [" + authToken + "]");
        var statement = "SELECT username FROM auth WHERE auth_token = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setString(1, authToken);
            LOGGER.info("Executing SELECT statement for auth token: [" + authToken + "]");
            try (var rs = preparedStatement.executeQuery()) {
                LOGGER.info("SELECT statement executed for auth token: [" + authToken + "]");
                if (rs.next()) {
                    String username = rs.getString("username");
                    LOGGER.info("Auth token found for user: " + username);
                    return new AuthData(authToken, username);
                }
                LOGGER.warning("Auth token not found in database: [" + authToken + "]");
                return null;
            }
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error getting auth token [" + authToken + "]: " + ex.getMessage());
            throw new DataAccessException("failed to get auth token", ex);
        } finally {
            LOGGER.info("Finished getAuth for token: [" + authToken + "]");
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        LOGGER.info("Deleting auth token: " + authToken);
        var statement = "DELETE FROM auth WHERE auth_token = ?";
         Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction
            LOGGER.info("Transaction started for deleting auth token: " + authToken);

            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.setString(1, authToken);
                LOGGER.info("Executing DELETE statement for auth token: " + authToken);
                int rowsAffected = preparedStatement.executeUpdate();
                LOGGER.info("DELETE statement executed for auth token: " + authToken + ", rows affected: " + rowsAffected);
                if (rowsAffected == 0) {
                    LOGGER.warning("Auth token not found for deletion: " + authToken);
                    if (conn != null) {
                        try {
                            conn.rollback(); // Rollback transaction
                             LOGGER.warning("Transaction rolled back for deleting auth token (not found): " + authToken);
                        } catch (SQLException rollbackEx) {
                             LOGGER.severe("Error during rollback (not found): " + rollbackEx.getMessage());
                        }
                    }
                    throw new DataAccessException("Error: unauthorized");
                }
            }
            conn.commit(); // Commit transaction
            LOGGER.info("Transaction committed for deleting auth token: " + authToken);
        } catch (SQLException ex) {
             LOGGER.severe("SQL Error deleting auth token " + authToken + ": " + ex.getMessage());
             if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction
                     LOGGER.warning("Transaction rolled back for deleting auth token: " + authToken);
                } catch (SQLException rollbackEx) {
                    LOGGER.severe("Error during rollback: " + rollbackEx.getMessage());
                }
            }
            throw new DataAccessException("failed to delete auth token", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close(); // Close connection
                    LOGGER.info("Database connection closed after deleting auth token: " + authToken);
                } catch (SQLException closeEx) {
                    LOGGER.severe("Error closing connection after deleting auth token: " + closeEx.getMessage());
                }
            }
        }
        LOGGER.info("Finished deleteAuth for token: " + authToken);
    }
} 