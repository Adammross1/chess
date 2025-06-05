package server;

import chess.ChessBoard;
import chess.ChessPiece;
import com.google.gson.*;
import java.lang.reflect.Type;

public class ChessBoardAdapter implements JsonSerializer<ChessBoard>, JsonDeserializer<ChessBoard> {
    @Override
    public JsonElement serialize(ChessBoard board, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        JsonArray squares = new JsonArray();
        
        for (int row = 0; row < 8; row++) {
            JsonArray rowArray = new JsonArray();
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(new chess.ChessPosition(row + 1, col + 1));
                if (piece == null) {
                    rowArray.add(JsonNull.INSTANCE);
                } else {
                    rowArray.add(context.serialize(piece));
                }
            }
            squares.add(rowArray);
        }
        
        jsonObject.add("squares", squares);
        return jsonObject;
    }

    @Override
    public ChessBoard deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonArray squares = jsonObject.getAsJsonArray("squares");
        
        ChessBoard board = new ChessBoard();
        
        for (int row = 0; row < 8; row++) {
            JsonArray rowArray = squares.get(row).getAsJsonArray();
            for (int col = 0; col < 8; col++) {
                JsonElement element = rowArray.get(col);
                if (!element.isJsonNull()) {
                    ChessPiece piece = context.deserialize(element, ChessPiece.class);
                    board.addPiece(new chess.ChessPosition(row + 1, col + 1), piece);
                }
            }
        }
        
        return board;
    }
} 