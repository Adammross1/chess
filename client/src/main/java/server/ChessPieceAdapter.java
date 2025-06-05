package server;

import chess.ChessGame;
import chess.ChessPiece;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.gson.TypeAdapter;
import java.io.IOException;

public class ChessPieceAdapter extends TypeAdapter<ChessPiece> {

    @Override
    public void write(JsonWriter out, ChessPiece value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginObject();
        out.name("pieceColor").value(value.getTeamColor().toString());
        out.name("pieceType").value(value.getPieceType().toString());
        out.endObject();
    }

    @Override
    public ChessPiece read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        ChessGame.TeamColor pieceColor = null;
        ChessPiece.PieceType pieceType = null;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            if (name.equals("pieceColor")) {
                pieceColor = ChessGame.TeamColor.valueOf(in.nextString());
            } else if (name.equals("pieceType")) {
                pieceType = ChessPiece.PieceType.valueOf(in.nextString());
            } else if (name.equals("type")) {
                pieceType = ChessPiece.PieceType.valueOf(in.nextString());
            } else if (name.equals("calculator")) {
                // Skip the calculator field - it will be created by the ChessPiece constructor
                in.skipValue();
            }
        }
        in.endObject();

        if (pieceColor != null && pieceType != null) {
            return new ChessPiece(pieceColor, pieceType);
        }
        return null;
    }
} 