package dataaccess;

import model.UserData;
import java.sql.*;

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
            preparedStatement.setString(1, user.username());
            preparedStatement.setString(2, user.password());
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
                    String password = rs.getString("password_hash");
                    return new UserData(username, password, null);
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get user", ex);
        }
    }
} 