package dataaccess;

import model.AuthData;
import java.sql.*;

public class MySQLAuthDAO implements AuthDAO {
    public MySQLAuthDAO() throws DataAccessException {
        DatabaseManager.initializeDatabase();
    }

    @Override
    public void clear() throws DataAccessException {
        var statement = "TRUNCATE auth";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to clear auth table", ex);
        }
    }

    @Override
    public AuthData createAuth(AuthData authData) throws DataAccessException {
        var statement = "INSERT INTO auth (auth_token, username) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            String authToken = authData.authToken();
            if (authToken == null || authToken.isEmpty()) {
                authToken = java.util.UUID.randomUUID().toString();
            }
            
            preparedStatement.setString(1, authToken);
            preparedStatement.setString(2, authData.username());
            preparedStatement.executeUpdate();
            
            return new AuthData(authToken, authData.username());
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create auth token", ex);
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        var statement = "SELECT username FROM auth WHERE auth_token = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setString(1, authToken);
            try (var rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    String username = rs.getString("username");
                    return new AuthData(authToken, username);
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get auth token", ex);
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        var statement = "DELETE FROM auth WHERE auth_token = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setString(1, authToken);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                throw new DataAccessException("Error: unauthorized");
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to delete auth token", ex);
        }
    }
} 