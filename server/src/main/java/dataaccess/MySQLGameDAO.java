package dataaccess;

import model.GameData;
import chess.ChessGame;
import chess.ChessPiece;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import com.google.gson.JsonSyntaxException;

public class MySQLGameDAO implements GameDAO {
    private static final Logger LOGGER = Logger.getLogger(MySQLGameDAO.class.getName());
    private final Gson gson;

    public MySQLGameDAO() throws DataAccessException {
        LOGGER.info("Initializing MySQLGameDAO");
        DatabaseManager.initializeDatabase();
        // Create Gson instance with our custom adapters
        gson = new GsonBuilder()
            .registerTypeAdapter(ChessGame.class, new ChessGameAdapter())
            .registerTypeAdapter(ChessPiece.class, new ChessPieceAdapter())
            .create();
         LOGGER.info("MySQLGameDAO initialized");
    }

    @Override
    public void clear() throws DataAccessException {
        LOGGER.info("Clearing game table");
        var statement = "DELETE FROM game";
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction
            LOGGER.info("Transaction started for clearing game table");

            try (var preparedStatement = conn.prepareStatement(statement)) {
                LOGGER.info("Executing DELETE FROM game statement");
                preparedStatement.executeUpdate();
                LOGGER.info("DELETE FROM game statement executed successfully");
            }

            conn.commit(); // Commit transaction
            LOGGER.info("Transaction committed for clearing game table");
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error clearing game table: " + ex.getMessage());
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction
                    LOGGER.warning("Transaction rolled back for clearing game table");
                } catch (SQLException rollbackEx) {
                    LOGGER.severe("Error during rollback: " + rollbackEx.getMessage());
                }
            }
            throw new DataAccessException("failed to clear game table", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close(); // Close connection
                    LOGGER.info("Database connection closed after clearing game table");
                } catch (SQLException closeEx) {
                    LOGGER.severe("Error closing connection after clearing game table: " + closeEx.getMessage());
                }
            }
        }
        LOGGER.info("Game table cleared");
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        LOGGER.info("Creating game: " + game.gameName());
        var statement = "INSERT INTO game (white_username, black_username, game_name, game_state) VALUES (?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction
            LOGGER.info("Transaction started for creating game: " + game.gameName());

            try (var preparedStatement = conn.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, game.whiteUsername());
                preparedStatement.setString(2, game.blackUsername());
                preparedStatement.setString(3, game.gameName());
                preparedStatement.setString(4, gson.toJson(game.game()));
                LOGGER.info("Executing INSERT statement for game: " + game.gameName());
                preparedStatement.executeUpdate();
                LOGGER.info("INSERT statement executed successfully for game: " + game.gameName());

                try (var rs = preparedStatement.getGeneratedKeys()) {
                    if (rs.next()) {
                        int gameID = rs.getInt(1);
                        LOGGER.info("Generated game ID: " + gameID);
                        conn.commit(); // Commit transaction
                        LOGGER.info("Transaction committed for creating game: " + game.gameName());
                        return gameID;
                    }
                    LOGGER.severe("Failed to get generated game ID for game: " + game.gameName());
                     if (conn != null) {
                        try {
                            conn.rollback(); // Rollback transaction
                            LOGGER.warning("Transaction rolled back for creating game (no ID): " + game.gameName());
                        } catch (SQLException rollbackEx) {
                            LOGGER.severe("Error during rollback (no ID): " + rollbackEx.getMessage());
                        }
                    }
                    throw new DataAccessException("failed to get generated game ID");
                }
            }
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error creating game " + game.gameName() + ": " + ex.getMessage());
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction
                    LOGGER.warning("Transaction rolled back for creating game: " + game.gameName());
                } catch (SQLException rollbackEx) {
                    LOGGER.severe("Error during rollback: " + rollbackEx.getMessage());
                }
            }
            throw new DataAccessException("failed to create game", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close(); // Close connection
                    LOGGER.info("Database connection closed after creating game: " + game.gameName());
                } catch (SQLException closeEx) {
                    LOGGER.severe("Error closing connection after creating game: " + closeEx.getMessage());
                }
            }
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        LOGGER.info("Getting game with ID: " + gameID);
        var statement = "SELECT * FROM game WHERE id = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setInt(1, gameID);
            LOGGER.info("Executing SELECT statement for game ID: " + gameID);
            try (var rs = preparedStatement.executeQuery()) {
                LOGGER.info("SELECT statement executed for game ID: " + gameID);
                if (rs.next()) {
                    String gameStateJson = rs.getString("game_state");
                    LOGGER.fine("Retrieved game state JSON for ID " + gameID + ": " + gameStateJson);
                    ChessGame game = gson.fromJson(gameStateJson, ChessGame.class);
                    LOGGER.fine("Deserialized game state for ID " + gameID + ": " + game);
                    LOGGER.info("Game found with ID: " + gameID);

                    return new GameData(
                        rs.getInt("id"),
                        rs.getString("white_username"),
                        rs.getString("black_username"),
                        rs.getString("game_name"),
                        game
                    );
                }
                LOGGER.info("Game not found with ID: " + gameID);
                return null;
            }
        } catch (SQLException ex) {
             LOGGER.severe("SQL Error getting game with ID " + gameID + ": " + ex.getMessage());
            throw new DataAccessException("failed to get game", ex);
        } catch (JsonSyntaxException ex) {
            LOGGER.severe("JSON Syntax Error getting game with ID " + gameID + ": " + ex.getMessage());
            throw new DataAccessException("failed to deserialize game state", ex);
        } finally {
            LOGGER.info("Finished getGame for ID: " + gameID);
        }
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        LOGGER.info("Listing all games");
        var statement = "SELECT * FROM game";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement);
             var rs = preparedStatement.executeQuery()) {
            LOGGER.info("Executing SELECT statement for listing games");
            var games = new ArrayList<GameData>();
            while (rs.next()) {
                String gameStateJson = rs.getString("game_state");
                LOGGER.fine("Retrieved game state JSON for list: " + gameStateJson);
                try {
                    ChessGame game = gson.fromJson(gameStateJson, ChessGame.class);
                     LOGGER.fine("Deserialized game state for list: " + game);
                
                    games.add(new GameData(
                        rs.getInt("id"),
                        rs.getString("white_username"),
                        rs.getString("black_username"),
                        rs.getString("game_name"),
                        game
                    ));
                } catch (JsonSyntaxException ex) {
                    LOGGER.severe("JSON Syntax Error listing games: " + ex.getMessage());
                    // Depending on requirements, you might skip this game or re-throw
                    throw new DataAccessException("failed to deserialize game state in list", ex);
                }
            }
            LOGGER.info("Finished listing games, found " + games.size() + " games");
            return games;
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error listing games: " + ex.getMessage());
            throw new DataAccessException("failed to list games", ex);
        } finally {
            LOGGER.info("Finished listGames");
        }
    }

    @Override
    public void updateGame(int gameID, String whiteUsername, String blackUsername) throws DataAccessException {
        LOGGER.info("Updating game with ID: " + gameID + ", white: " + whiteUsername + ", black: " + blackUsername);
        // First get the current game state
        GameData currentGame = getGame(gameID);
        if (currentGame == null) {
            LOGGER.warning("Game not found for update with ID: " + gameID);
            throw new DataAccessException("Error: game not found");
        }

        var statement = "UPDATE game SET white_username = ?, black_username = ?, game_state = ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction
            LOGGER.info("Transaction started for updating game with ID: " + gameID);

            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.setString(1, whiteUsername);
                preparedStatement.setString(2, blackUsername);
                // Re-serialize the current game state
                preparedStatement.setString(3, gson.toJson(currentGame.game()));
                preparedStatement.setInt(4, gameID);
                LOGGER.info("Executing UPDATE statement for game ID: " + gameID);
                int rowsAffected = preparedStatement.executeUpdate();
                LOGGER.info("UPDATE statement executed for game ID: " + gameID + ", rows affected: " + rowsAffected);
                if (rowsAffected == 0) {
                    LOGGER.warning("Game not found for update (after get): " + gameID);
                    if (conn != null) {
                        try {
                            conn.rollback(); // Rollback transaction
                            LOGGER.warning("Transaction rolled back for updating game (not found): " + gameID);
                        } catch (SQLException rollbackEx) {
                            LOGGER.severe("Error during rollback (not found): " + rollbackEx.getMessage());
                        }
                    }
                    throw new DataAccessException("Error: game not found");
                }
            }
            conn.commit(); // Commit transaction
            LOGGER.info("Transaction committed for updating game with ID: " + gameID);
        } catch (SQLException ex) {
             LOGGER.severe("SQL Error updating game with ID " + gameID + ": " + ex.getMessage());
             if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction
                    LOGGER.warning("Transaction rolled back for updating game: " + gameID);
                } catch (SQLException rollbackEx) {
                    LOGGER.severe("Error during rollback: " + rollbackEx.getMessage());
                }
            }
            throw new DataAccessException("failed to update game", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close(); // Close connection
                     LOGGER.info("Database connection closed after updating game with ID: " + gameID);
                } catch (SQLException closeEx) {
                     LOGGER.severe("Error closing connection after updating game with ID: " + closeEx.getMessage());
                }
            }
        }
        LOGGER.info("Finished updateGame for ID: " + gameID);
    }

    /**
     * Updates the game state for a specific game
     * @param gameID the ID of the game to update
     * @param updatedGame the updated ChessGame object
     * @throws DataAccessException if the game doesn't exist or there's a database error
     */
    public void updateGameState(int gameID, ChessGame updatedGame) throws DataAccessException {
        LOGGER.info("Updating game state for game ID: " + gameID);
        var statement = "UPDATE game SET game_state = ? WHERE id = ?";
        Connection conn = null;
        try {
             conn = DatabaseManager.getConnection();
             conn.setAutoCommit(false); // Start transaction
             LOGGER.info("Transaction started for updating game state for game ID: " + gameID);

             try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.setString(1, gson.toJson(updatedGame));
                preparedStatement.setInt(2, gameID);
                LOGGER.info("Executing UPDATE statement for game state, ID: " + gameID);
                int rowsAffected = preparedStatement.executeUpdate();
                LOGGER.info("UPDATE statement executed for game state, ID: " + gameID + ", rows affected: " + rowsAffected);
                if (rowsAffected == 0) {
                    LOGGER.warning("Game not found for state update with ID: " + gameID);
                     if (conn != null) {
                        try {
                            conn.rollback(); // Rollback transaction
                            LOGGER.warning("Transaction rolled back for updating game state (not found): " + gameID);
                        } catch (SQLException rollbackEx) {
                            LOGGER.severe("Error during rollback (not found): " + rollbackEx.getMessage());
                        }
                    }
                    throw new DataAccessException("Error: game not found");
                }
            }
             conn.commit(); // Commit transaction
             LOGGER.info("Transaction committed for updating game state for game ID: " + gameID);
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error updating game state for ID " + gameID + ": " + ex.getMessage());
             if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction
                    LOGGER.warning("Transaction rolled back for updating game state: " + gameID);
                } catch (SQLException rollbackEx) {
                    LOGGER.severe("Error during rollback: " + rollbackEx.getMessage());
                }
            }
            throw new DataAccessException("failed to update game state", ex);
        } finally {
             if (conn != null) {
                try {
                    conn.close(); // Close connection
                    LOGGER.info("Database connection closed after updating game state for ID: " + gameID);
                } catch (SQLException closeEx) {
                    LOGGER.severe("Error closing connection after updating game state for ID: " + closeEx.getMessage());
                }
            }
        }
        LOGGER.info("Finished updateGameState for ID: " + gameID);
    }
} 