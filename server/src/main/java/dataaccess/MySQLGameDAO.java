package dataaccess;

import model.GameData;
import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLGameDAO implements GameDAO {
    private final Gson gson;

    public MySQLGameDAO() throws DataAccessException {
        DatabaseManager.initializeDatabase();
        // Create Gson instance with our custom adapter
        gson = new GsonBuilder()
            .registerTypeAdapter(ChessGame.class, new ChessGameAdapter())
            .create();
    }

    @Override
    public void clear() throws DataAccessException {
        var statement = "TRUNCATE game";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to clear game table", ex);
        }
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        var statement = "INSERT INTO game (white_username, black_username, game_name, game_state) VALUES (?, ?, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, game.whiteUsername());
            preparedStatement.setString(2, game.blackUsername());
            preparedStatement.setString(3, game.gameName());
            preparedStatement.setString(4, gson.toJson(game.game()));
            preparedStatement.executeUpdate();
            
            try (var rs = preparedStatement.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new DataAccessException("failed to get generated game ID");
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create game", ex);
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        var statement = "SELECT * FROM game WHERE id = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setInt(1, gameID);
            try (var rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    String gameStateJson = rs.getString("game_state");
                    ChessGame game = gson.fromJson(gameStateJson, ChessGame.class);
                    
                    return new GameData(
                        rs.getInt("id"),
                        rs.getString("white_username"),
                        rs.getString("black_username"),
                        rs.getString("game_name"),
                        game
                    );
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get game", ex);
        }
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        var statement = "SELECT * FROM game";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement);
             var rs = preparedStatement.executeQuery()) {
            var games = new ArrayList<GameData>();
            while (rs.next()) {
                String gameStateJson = rs.getString("game_state");
                ChessGame game = gson.fromJson(gameStateJson, ChessGame.class);
                
                games.add(new GameData(
                    rs.getInt("id"),
                    rs.getString("white_username"),
                    rs.getString("black_username"),
                    rs.getString("game_name"),
                    game
                ));
            }
            return games;
        } catch (SQLException ex) {
            throw new DataAccessException("failed to list games", ex);
        }
    }

    @Override
    public void updateGame(int gameID, String whiteUsername, String blackUsername) throws DataAccessException {
        // First get the current game state
        GameData currentGame = getGame(gameID);
        if (currentGame == null) {
            throw new DataAccessException("Error: game not found");
        }

        var statement = "UPDATE game SET white_username = ?, black_username = ?, game_state = ? WHERE id = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setString(1, whiteUsername);
            preparedStatement.setString(2, blackUsername);
            // Re-serialize the current game state
            preparedStatement.setString(3, gson.toJson(currentGame.game()));
            preparedStatement.setInt(4, gameID);
            
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                throw new DataAccessException("Error: game not found");
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to update game", ex);
        }
    }

    /**
     * Updates the game state for a specific game
     * @param gameID the ID of the game to update
     * @param updatedGame the updated ChessGame object
     * @throws DataAccessException if the game doesn't exist or there's a database error
     */
    public void updateGameState(int gameID, ChessGame updatedGame) throws DataAccessException {
        var statement = "UPDATE game SET game_state = ? WHERE id = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setString(1, gson.toJson(updatedGame));
            preparedStatement.setInt(2, gameID);
            
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                throw new DataAccessException("Error: game not found");
            }
        } catch (SQLException ex) {
            throw new DataAccessException("failed to update game state", ex);
        }
    }
} 