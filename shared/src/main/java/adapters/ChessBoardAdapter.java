package adapters;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.*;
import java.lang.reflect.Type;

public class ChessBoardAdapter implements JsonSerializer<ChessBoard>, JsonDeserializer<ChessBoard> {
    @Override
    public JsonElement serialize(ChessBoard src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        JsonArray boardArray = new JsonArray();
        
        for (int row = 0; row < 8; row++) {
            JsonArray rowArray = new JsonArray();
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = src.getPiece(new ChessPosition(row + 1, col + 1));
                if (piece != null) {
                    rowArray.add(context.serialize(piece));
                } else {
                    rowArray.add(JsonNull.INSTANCE);
                }
            }
            boardArray.add(rowArray);
        }
        
        jsonObject.add("board", boardArray);
        return jsonObject;
    }

    @Override
    public ChessBoard deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ChessBoard board = new ChessBoard();
        JsonObject jsonObject = json.getAsJsonObject();
        JsonArray boardArray = jsonObject.getAsJsonArray("board");
        
        for (int row = 0; row < 8; row++) {
            JsonArray rowArray = boardArray.get(row).getAsJsonArray();
            for (int col = 0; col < 8; col++) {
                JsonElement pieceElement = rowArray.get(col);
                if (!pieceElement.isJsonNull()) {
                    ChessPiece piece = context.deserialize(pieceElement, ChessPiece.class);
                    board.addPiece(new ChessPosition(row + 1, col + 1), piece);
                }
            }
        }
        
        return board;
    }
} 