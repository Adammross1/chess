package adapters;

import chess.ChessPiece;
import chess.ChessGame;
import com.google.gson.*;
import java.lang.reflect.Type;

public class ChessPieceAdapter implements JsonSerializer<ChessPiece>, JsonDeserializer<ChessPiece> {
    @Override
    public JsonElement serialize(ChessPiece src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", src.getPieceType().toString());
        jsonObject.addProperty("teamColor", src.getTeamColor().toString());
        return jsonObject;
    }

    @Override
    public ChessPiece deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String pieceType = jsonObject.get("type").getAsString();
        String teamColor = jsonObject.get("teamColor").getAsString();
        
        return new ChessPiece(
            ChessGame.TeamColor.valueOf(teamColor),
            ChessPiece.PieceType.valueOf(pieceType)
        );
    }
} 