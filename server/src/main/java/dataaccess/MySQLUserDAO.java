package dataaccess;

import model.UserData;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;
import java.util.logging.Logger;

public class MySQLUserDAO implements UserDAO {
    private static final Logger LOGGER = Logger.getLogger(MySQLUserDAO.class.getName());

    public MySQLUserDAO() throws DataAccessException {
        LOGGER.info("Initializing MySQLUserDAO");
        DatabaseManager.initializeDatabase();
        LOGGER.info("MySQLUserDAO initialized");
    }

    @Override
    public void clear() throws DataAccessException {
        LOGGER.info("Clearing user table");
        var statement = "DELETE FROM user";
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction
            LOGGER.info("Transaction started for clearing user table");

            try (var preparedStatement = conn.prepareStatement(statement)) {
                LOGGER.info("Executing DELETE FROM user statement");
                preparedStatement.executeUpdate();
                LOGGER.info("DELETE FROM user statement executed successfully");
            }

            conn.commit(); // Commit transaction
            LOGGER.info("Transaction committed for clearing user table");
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error clearing user table: " + ex.getMessage());
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction
                    LOGGER.warning("Transaction rolled back for clearing user table");
                } catch (SQLException rollbackEx) {
                    LOGGER.severe("Error during rollback: " + rollbackEx.getMessage());
                }
            }
            throw new DataAccessException("failed to clear user table", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close(); // Close connection
                    LOGGER.info("Database connection closed after clearing user table");
                } catch (SQLException closeEx) {
                    LOGGER.severe("Error closing connection after clearing user table: " + closeEx.getMessage());
                }
            }
        }
        LOGGER.info("User table cleared");
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        LOGGER.info("Creating user: " + user.username());
        var statement = "INSERT INTO user (username, password_hash) VALUES (?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction
            LOGGER.info("Transaction started for creating user: " + user.username());

            // Hash the password before storing
            String hashedPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());
            LOGGER.fine("Hashed password for user: " + user.username());
            
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.setString(1, user.username());
                preparedStatement.setString(2, hashedPassword);
                LOGGER.info("Executing INSERT statement for user: " + user.username());
                preparedStatement.executeUpdate();
                LOGGER.info("INSERT statement executed successfully for user: " + user.username());
            }

            conn.commit(); // Commit transaction
            LOGGER.info("Transaction committed for creating user: " + user.username());
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error creating user " + user.username() + ": " + ex.getMessage());
            if (ex.getMessage().contains("Duplicate entry")) {
                LOGGER.warning("Duplicate entry error for username: " + user.username());
                if (conn != null) {
                    try {
                        conn.rollback(); // Rollback transaction
                        LOGGER.warning("Transaction rolled back for creating user (duplicate): " + user.username());
                    } catch (SQLException rollbackEx) {
                        LOGGER.severe("Error during rollback (duplicate): " + rollbackEx.getMessage());
                    }
                }
                throw new DataAccessException("Error: username already taken");
            }
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction
                    LOGGER.warning("Transaction rolled back for creating user: " + user.username());
                } catch (SQLException rollbackEx) {
                    LOGGER.severe("Error during rollback: " + rollbackEx.getMessage());
                }
            }
            throw new DataAccessException("failed to create user", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close(); // Close connection
                    LOGGER.info("Database connection closed after creating user: " + user.username());
                } catch (SQLException closeEx) {
                    LOGGER.severe("Error closing connection after creating user: " + closeEx.getMessage());
                }
            }
        }
        LOGGER.info("Finished createUser for: " + user.username());
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        LOGGER.info("Getting user: " + username);
        var statement = "SELECT password_hash FROM user WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setString(1, username);
            LOGGER.info("Executing SELECT statement for user: " + username);
            try (var rs = preparedStatement.executeQuery()) {
                LOGGER.info("SELECT statement executed for user: " + username);
                if (rs.next()) {
                    String hashedPassword = rs.getString("password_hash");
                    LOGGER.info("User found: " + username);
                    return new UserData(username, hashedPassword, null);
                }
                LOGGER.info("User not found: " + username);
                return null;
            }
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error getting user " + username + ": " + ex.getMessage());
            throw new DataAccessException("failed to get user", ex);
        } finally {
            LOGGER.info("Finished getUser for: " + username);
        }
    }

    /**
     * Verifies if the provided password matches the stored hash
     * @param username the username to check
     * @param password the password to verify
     * @return true if the password matches, false otherwise
     * @throws DataAccessException if there's an error accessing the database
     */
    public boolean verifyPassword(String username, String password) throws DataAccessException {
        LOGGER.info("Verifying password for user: " + username);
        var statement = "SELECT password_hash FROM user WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setString(1, username);
            LOGGER.info("Executing SELECT statement for password verification: " + username);
            try (var rs = preparedStatement.executeQuery()) {
                LOGGER.info("SELECT statement executed for password verification: " + username);
                if (rs.next()) {
                    String hashedPassword = rs.getString("password_hash");
                    LOGGER.fine("Retrieved password hash for user: " + username);
                    boolean isMatch = BCrypt.checkpw(password, hashedPassword);
                    LOGGER.info("Password verification result for " + username + ": " + isMatch);
                    return isMatch;
                }
                LOGGER.info("User not found for password verification: " + username);
                return false;
            }
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error verifying password for user " + username + ": " + ex.getMessage());
            throw new DataAccessException("failed to verify password", ex);
        } finally {
             LOGGER.info("Finished verifyPassword for: " + username);
        }
    }
} 