package server;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.*;
import java.lang.reflect.Type;

public class ChessBoardAdapter implements JsonSerializer<ChessBoard>, JsonDeserializer<ChessBoard> {
    @Override
    public JsonElement serialize(ChessBoard board, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        JsonArray squares = new JsonArray();
        
        for (int row = 1; row <= 8; row++) {
            JsonArray rowArray = new JsonArray();
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                rowArray.add(context.serialize(piece));
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
                JsonElement pieceElement = rowArray.get(col);
                if (!pieceElement.isJsonNull()) {
                    ChessPiece piece = context.deserialize(pieceElement, ChessPiece.class);
                    if (piece != null) {
                        board.addPiece(new ChessPosition(row + 1, col + 1), piece);
                    }
                }
            }
        }
        
        return board;
    }
} 