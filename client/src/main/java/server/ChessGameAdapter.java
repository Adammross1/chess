package server;

import chess.ChessGame;
import chess.ChessBoard;
import com.google.gson.*;
import java.lang.reflect.Type;

public class ChessGameAdapter implements JsonSerializer<ChessGame>, JsonDeserializer<ChessGame> {
    @Override
    public JsonElement serialize(ChessGame game, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("board", game.getBoard().toString());
        jsonObject.addProperty("teamTurn", game.getTeamTurn().toString());
        jsonObject.addProperty("gameState", game.getGameState().toString());
        return jsonObject;
    }

    @Override
    public ChessGame deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        ChessGame game = new ChessGame();
        
        JsonElement boardElement = jsonObject.get("board");
        ChessBoard board = context.deserialize(boardElement, ChessBoard.class);
        game.setBoard(board);
        
        JsonElement teamTurnElement = jsonObject.get("teamTurn");
        if (teamTurnElement != null && !teamTurnElement.isJsonNull()) {
            String teamTurn = teamTurnElement.getAsString();
            game.setTeamTurn(ChessGame.TeamColor.valueOf(teamTurn));
        } else {
            // If teamTurn is null in JSON, set it to the current player's color
            // This ensures we always have a valid teamTurn
            game.setTeamTurn(ChessGame.TeamColor.WHITE);
        }
        
        JsonElement gameStateElement = jsonObject.get("gameState");
        if (gameStateElement != null && !gameStateElement.isJsonNull()) {
            String gameState = gameStateElement.getAsString();
            game.setGameState(ChessGame.GameState.valueOf(gameState));
        }
        
        return game;
    }
} 