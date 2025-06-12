package dataaccess;

import model.GameData;
import model.UserData;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.google.gson.Gson;

public class MySQLGameDAOTest {
    private MySQLGameDAO gameDAO;
    private MySQLUserDAO userDAO;
    private Gson gson;

    @BeforeEach
    void setUp() throws DataAccessException {
        gson = new Gson();
        userDAO = new MySQLUserDAO();
        userDAO.clear();
        gameDAO = new MySQLGameDAO(gson);
        gameDAO.clear();
    }

    @Test
    @DisplayName("Positive: Create game successfully")
    void createGameSuccess() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, null, null, "Test Game", game);
        
        int gameID = gameDAO.createGame(gameData);
        assertTrue(gameID > 0);
        
        GameData retrievedGame = gameDAO.getGame(gameID);
        assertNotNull(retrievedGame);
        assertEquals("Test Game", retrievedGame.gameName());
        assertNull(retrievedGame.whiteUsername());
        assertNull(retrievedGame.blackUsername());
    }

    @Test
    @DisplayName("Positive: Get existing game")
    void getGameSuccess() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, null, null, "Test Game", game);
        int gameID = gameDAO.createGame(gameData);
        
        GameData retrievedGame = gameDAO.getGame(gameID);
        assertNotNull(retrievedGame);
        assertEquals(gameID, retrievedGame.gameID());
        assertEquals("Test Game", retrievedGame.gameName());
    }

    @Test
    @DisplayName("Negative: Get non-existent game")
    void getGameNonExistent() throws DataAccessException {
        GameData retrievedGame = gameDAO.getGame(999);
        assertNull(retrievedGame);
    }

    @Test
    @DisplayName("Positive: List games")
    void listGamesSuccess() throws DataAccessException {
        ChessGame game1 = new ChessGame();
        ChessGame game2 = new ChessGame();
        GameData gameData1 = new GameData(0, null, null, "Game 1", game1);
        GameData gameData2 = new GameData(0, null, null, "Game 2", game2);
        
        gameDAO.createGame(gameData1);
        gameDAO.createGame(gameData2);
        
        List<GameData> games = gameDAO.listGames();
        assertEquals(2, games.size());
    }

    @Test
    @DisplayName("Positive: Update game with players")
    void updateGameSuccess() throws DataAccessException {
        userDAO.createUser(new UserData("whitePlayer", "password", "email"));
        userDAO.createUser(new UserData("blackPlayer", "password", "email"));
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, null, null, "Test Game", game);
        int gameID = gameDAO.createGame(gameData);
        
        gameDAO.updateGame(gameID, "whitePlayer", "blackPlayer");
        
        GameData updatedGame = gameDAO.getGame(gameID);
        assertEquals("whitePlayer", updatedGame.whiteUsername());
        assertEquals("blackPlayer", updatedGame.blackUsername());
    }

    @Test
    @DisplayName("Negative: Update non-existent game")
    void updateGameNonExistent() {
        assertThrows(DataAccessException.class, () -> {
            gameDAO.updateGame(999, "whitePlayer", "blackPlayer");
        });
    }

    @Test
    @DisplayName("Positive: Update game state")
    void updateGameStateSuccess() throws DataAccessException, chess.InvalidMoveException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, null, null, "Test Game", game);
        int gameID = gameDAO.createGame(gameData);
        
        // Make a move
        ChessPosition start = new ChessPosition(2, 1);
        ChessPosition end = new ChessPosition(3, 1);
        ChessMove move = new ChessMove(start, end, null);
        game.makeMove(move);
        
        gameDAO.updateGameState(gameID, game);
        
        GameData updatedGame = gameDAO.getGame(gameID);
        assertNotNull(updatedGame.game());
        // Verify the move was persisted by checking piece positions
        assertNull(updatedGame.game().getBoard().getPiece(start));
        assertNotNull(updatedGame.game().getBoard().getPiece(end));
    }

    @Test
    @DisplayName("Negative: Update game state for non-existent game")
    void updateGameStateNonExistent() throws chess.InvalidMoveException {
        ChessGame game = new ChessGame();
        assertThrows(DataAccessException.class, () -> {
            gameDAO.updateGameState(999, game);
        });
    }

    @Test
    @DisplayName("Positive: Clear games")
    void clearSuccess() throws DataAccessException {
        ChessGame game1 = new ChessGame();
        ChessGame game2 = new ChessGame();
        GameData gameData1 = new GameData(0, null, null, "Game 1", game1);
        GameData gameData2 = new GameData(0, null, null, "Game 2", game2);
        
        int gameID1 = gameDAO.createGame(gameData1);
        int gameID2 = gameDAO.createGame(gameData2);
        
        gameDAO.clear();
        
        assertNull(gameDAO.getGame(gameID1));
        assertNull(gameDAO.getGame(gameID2));
        assertEquals(0, gameDAO.listGames().size());
    }
} 