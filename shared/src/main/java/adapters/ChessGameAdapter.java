package adapters;

import chess.ChessGame;
import chess.ChessBoard;
import com.google.gson.*;
import java.lang.reflect.Type;

public class ChessGameAdapter implements JsonSerializer<ChessGame>, JsonDeserializer<ChessGame> {
    @Override
    public JsonElement serialize(ChessGame game, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        
        // Serialize the board
        jsonObject.add("board", context.serialize(game.getBoard()));
        
        // Serialize the current turn and game state
        jsonObject.addProperty("teamTurn", game.getTeamTurn().toString());
        jsonObject.addProperty("gameState", game.getGameState().toString());
        
        return jsonObject;
    }

    @Override
    public ChessGame deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        
        // Create a new game
        ChessGame game = new ChessGame();
        
        // Deserialize and set the board
        ChessBoard board = context.deserialize(jsonObject.get("board"), ChessBoard.class);
        game.setBoard(board);
        
        // Deserialize and set the team turn
        JsonElement teamTurnElement = jsonObject.get("teamTurn");
        if (teamTurnElement != null && !teamTurnElement.isJsonNull()) {
            String teamTurn = teamTurnElement.getAsString();
            game.setTeamTurn(ChessGame.TeamColor.valueOf(teamTurn));
        } else {
            // If teamTurn is null in JSON, set it to the current player's color
            // This ensures we always have a valid teamTurn
            game.setTeamTurn(ChessGame.TeamColor.WHITE);
        }
        
        // Deserialize and set the game state
        String gameState = jsonObject.get("gameState").getAsString();
        game.setGameState(ChessGame.GameState.valueOf(gameState));
        
        return game;
    }
} 