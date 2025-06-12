package server;

import chess.ChessGame;
import chess.ChessPiece;
import com.google.gson.*;
import java.lang.reflect.Type;

public class ChessPieceAdapter implements JsonSerializer<ChessPiece>, JsonDeserializer<ChessPiece> {
    @Override
    public JsonElement serialize(ChessPiece piece, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("pieceColor", piece.getTeamColor().toString());
        jsonObject.addProperty("pieceType", piece.getPieceType().toString());
        return jsonObject;
    }

    @Override
    public ChessPiece deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        
        // Get piece color
        JsonElement colorElement = jsonObject.get("pieceColor");
        if (colorElement == null) {
            return null;
        }
        ChessGame.TeamColor color;
        try {
            color = ChessGame.TeamColor.valueOf(colorElement.getAsString());
        } catch (IllegalArgumentException e) {
            return null;
        }
        
        // Get piece type
        JsonElement typeElement = jsonObject.get("pieceType");
        if (typeElement == null) {
            typeElement = jsonObject.get("type");
        }
        if (typeElement == null) {
            return null;
        }
        ChessPiece.PieceType pieceType;
        try {
            pieceType = ChessPiece.PieceType.valueOf(typeElement.getAsString());
        } catch (IllegalArgumentException e) {
            return null;
        }
        
        return new ChessPiece(color, pieceType);
    }
} 