package dataaccess;

import chess.ChessGame;
import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPosition;
import com.google.gson.*;
import java.lang.reflect.Type;
import model.GameData;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ChessGameAdapter implements JsonSerializer<ChessGame>, JsonDeserializer<ChessGame> {
    private static final Logger LOGGER = Logger.getLogger(ChessGameAdapter.class.getName());

    @Override
    public JsonElement serialize(ChessGame game, Type type, JsonSerializationContext context) {
        LOGGER.log(Level.INFO, "TEAM_TURN: ====== Serialization START =====");
        LOGGER.log(Level.INFO, "TEAM_TURN: Game object: " + (game != null ? "not null" : "null"));
        
        if (game == null) {
            LOGGER.log(Level.WARNING, "TEAM_TURN: Error: Game is null");
            return JsonNull.INSTANCE;
        }
        
        ChessGame.TeamColor teamTurn = game.getTeamTurn();
        LOGGER.log(Level.INFO, "TEAM_TURN: Initial teamTurn: " + teamTurn);
        LOGGER.log(Level.INFO, "TEAM_TURN: Game state: " + game.getGameState());
        
        JsonObject jsonObject = new JsonObject();
        
        try {
            // Serialize the board
            jsonObject.add("board", context.serialize(game.getBoard()));
            
            // Serialize the current turn and game state
            if (teamTurn != null) {
                LOGGER.log(Level.INFO, "TEAM_TURN: Serializing teamTurn: " + teamTurn);
                jsonObject.addProperty("teamTurn", teamTurn.toString());
            } else {
                LOGGER.log(Level.WARNING, "TEAM_TURN: Warning: teamTurn is null, using default WHITE");
                jsonObject.addProperty("teamTurn", ChessGame.TeamColor.WHITE.toString());
            }
            jsonObject.addProperty("gameState", game.getGameState().toString());
            
            LOGGER.log(Level.INFO, "TEAM_TURN: Final JSON teamTurn: " + jsonObject.get("teamTurn"));
            LOGGER.log(Level.INFO, "TEAM_TURN: Final JSON gameState: " + jsonObject.get("gameState"));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "TEAM_TURN: Error during serialization: " + e.getMessage());
            e.printStackTrace();
        }
        
        LOGGER.log(Level.INFO, "TEAM_TURN: ====== Serialization END =====");
        return jsonObject;
    }

    @Override
    public ChessGame deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        LOGGER.log(Level.INFO, "TEAM_TURN: ====== Deserialization START =====");
        JsonObject jsonObject = json.getAsJsonObject();
        LOGGER.log(Level.INFO, "TEAM_TURN: Raw JSON: " + jsonObject);
        
        // Create a new game
        ChessGame game = new ChessGame();
        
        // Deserialize and set the board
        JsonElement boardElement = jsonObject.get("board");
        if (boardElement != null && !boardElement.isJsonNull()) {
            ChessBoard board = context.deserialize(boardElement, ChessBoard.class);
            game.setBoard(board);
        }
        
        // Deserialize and set the team turn
        JsonElement teamTurnElement = jsonObject.get("teamTurn");
        LOGGER.log(Level.INFO, "TEAM_TURN: teamTurn element: " + teamTurnElement);
        if (teamTurnElement != null && !teamTurnElement.isJsonNull()) {
            String teamTurn = teamTurnElement.getAsString();
            LOGGER.log(Level.INFO, "TEAM_TURN: Setting teamTurn from JSON: " + teamTurn);
            game.setTeamTurn(ChessGame.TeamColor.valueOf(teamTurn));
        } else {
            LOGGER.log(Level.WARNING, "TEAM_TURN: teamTurn is null in JSON, setting to WHITE");
            game.setTeamTurn(ChessGame.TeamColor.WHITE);
        }
        
        // Deserialize and set the game state
        JsonElement gameStateElement = jsonObject.get("gameState");
        if (gameStateElement != null && !gameStateElement.isJsonNull()) {
            try {
                String gameState = gameStateElement.getAsString();
                LOGGER.log(Level.INFO, "TEAM_TURN: Setting gameState from JSON: " + gameState);
                game.setGameState(ChessGame.GameState.valueOf(gameState));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "TEAM_TURN: Error parsing gameState, using default ACTIVE: " + e.getMessage());
                game.setGameState(ChessGame.GameState.ACTIVE);
            }
        } else {
            LOGGER.log(Level.WARNING, "TEAM_TURN: gameState is null in JSON, setting to ACTIVE");
            game.setGameState(ChessGame.GameState.ACTIVE);
        }
        
        LOGGER.log(Level.INFO, "TEAM_TURN: Final game state - teamTurn: " + game.getTeamTurn() + ", gameState: " + game.getGameState());
        LOGGER.log(Level.INFO, "TEAM_TURN: ====== Deserialization END =====");
        return game;
    }
} 