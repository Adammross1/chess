package dataaccess;

import model.UserData;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class MySQLUserDAO implements UserDAO {
    public MySQLUserDAO() throws DataAccessException {
        DatabaseManager.initializeDatabase();
    }

    @Override
    public void clear() throws DataAccessException {
        var statement = "TRUNCATE user";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to clear user table", ex);
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        var statement = "INSERT INTO user (username, password_hash) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            // Hash the password before storing
            String hashedPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());
            
            preparedStatement.setString(1, user.username());
            preparedStatement.setString(2, hashedPassword);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            if (ex.getMessage().contains("Duplicate entry")) {
                throw new DataAccessException("Error: username already taken");
            }
            throw new DataAccessException("failed to create user", ex);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        var statement = "SELECT password_hash FROM user WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setString(1, username);
            try (var rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    String hashedPassword = rs.getString("password_hash");
                    return new UserData(username, hashedPassword, null);
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get user", ex);
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
        var statement = "SELECT password_hash FROM user WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setString(1, username);
            try (var rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    String hashedPassword = rs.getString("password_hash");
                    return BCrypt.checkpw(password, hashedPassword);
                }
                return false;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to verify password", ex);
        }
    }
} 