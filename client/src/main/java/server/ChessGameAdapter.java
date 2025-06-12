package server;

import chess.ChessGame;
import chess.ChessBoard;
import com.google.gson.*;
import java.lang.reflect.Type;

public class ChessGameAdapter implements JsonSerializer<ChessGame>, JsonDeserializer<ChessGame> {
    @Override
    public JsonElement serialize(ChessGame game, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("board", context.serialize(game.getBoard()));
        jsonObject.addProperty("teamTurn", game.getTeamTurn().toString());
        return jsonObject;
    }

    @Override
    public ChessGame deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        ChessGame game = new ChessGame();
        
        JsonElement boardElement = jsonObject.get("board");
        ChessBoard board = context.deserialize(boardElement, ChessBoard.class);
        game.setBoard(board);
        
        String teamTurn = jsonObject.get("teamTurn").getAsString();
        game.setTeamTurn(ChessGame.TeamColor.valueOf(teamTurn));
        
        return game;
    }
} 