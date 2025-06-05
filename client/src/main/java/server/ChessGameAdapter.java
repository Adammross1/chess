package server;

import chess.ChessGame;
import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPosition;
import com.google.gson.*;
import java.lang.reflect.Type;

public class ChessGameAdapter implements JsonSerializer<ChessGame>, JsonDeserializer<ChessGame> {
    @Override
    public JsonElement serialize(ChessGame game, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        
        // Serialize the board
        jsonObject.add("board", context.serialize(game.getBoard()));
        
        // Serialize the current turn
        jsonObject.addProperty("teamTurn", game.getTeamTurn().toString());
        
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
        String teamTurn = jsonObject.get("teamTurn").getAsString();
        game.setTeamTurn(ChessGame.TeamColor.valueOf(teamTurn));
        
        return game;
    }
} 