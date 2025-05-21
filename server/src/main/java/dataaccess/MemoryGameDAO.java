package dataaccess;

import model.GameData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Memory-based implementation of GameDAO
 */
public class MemoryGameDAO implements GameDAO {
    private final Map<Integer, GameData> games = new HashMap<>();
    private final AtomicInteger nextGameID = new AtomicInteger(1);

    @Override
    public void clear() {
        games.clear();
    }

    @Override
    public int createGame(GameData game) {
        int gameID = nextGameID.getAndIncrement();

        GameData newGame = new GameData(gameID, game.whiteUsername(), game.blackUsername(),
                game.gameName(), game.game());
        games.put(gameID, newGame);
        return gameID;
    }

    @Override
    public GameData getGame(int gameID) {
        return games.get(gameID);
    }

    @Override
    public List<GameData> listGames() {
        return new ArrayList<>(games.values());
    }

    @Override
    public void updateGame(int gameID, String whiteUsername, String blackUsername) throws DataAccessException {
        GameData game = games.get(gameID);
        if (game == null) {
            throw new DataAccessException("Error: game not found");
        }

        // Create updated game with new player info
        GameData updatedGame = new GameData(
                game.gameID(),
                whiteUsername == null ? game.whiteUsername() : whiteUsername,
                blackUsername == null ? game.blackUsername() : blackUsername,
                game.gameName(),
                game.game()
        );

        games.put(gameID, updatedGame);
    }
}