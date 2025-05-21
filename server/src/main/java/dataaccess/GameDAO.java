package dataaccess;

import model.GameData;
import java.util.List;

/**
 * Interface for game data access operations
 */
public interface GameDAO {
    /**
     * Clears all game data
     * @throws DataAccessException if an error occurs
     */
    void clear() throws DataAccessException;

    /**
     * Creates a new game
     * @param game the game to create
     * @return the created game ID
     * @throws DataAccessException if an error occurs
     */
    int createGame(GameData game) throws DataAccessException;

    /**
     * Gets a game by ID
     * @param gameID the game ID to look up
     * @return the game data, or null if not found
     * @throws DataAccessException if an error occurs
     */
    GameData getGame(int gameID) throws DataAccessException;

    /**
     * Lists all games
     * @return list of all games
     * @throws DataAccessException if an error occurs
     */
    List<GameData> listGames() throws DataAccessException;

    /**
     * Updates a game
     * @param gameID the game ID to update
     * @param whiteUsername username of white player (can be null)
     * @param blackUsername username of black player (can be null)
     * @throws DataAccessException if an error occurs or game not found
     */
    void updateGame(int gameID, String whiteUsername, String blackUsername) throws DataAccessException;
}
