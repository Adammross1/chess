package chess.calculators;

import chess.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class RookMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        if (board.getPiece(myPosition) == null) {
            return Collections.emptyList();
        }

        Collection<ChessMove> moves = new ArrayList<>();
        ChessPiece rook = board.getPiece(myPosition);
        if (rook == null || rook.getPieceType() != ChessPiece.PieceType.ROOK) return moves;

        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        record Direction(int rowOffset, int colOffset) {}

        Direction[] directions = {
                new Direction(1, 0),
                new Direction(-1, 0),
                new Direction(0, 1),
                new Direction(0, -1)
        };

        for (Direction d : directions) {
            int offsetRow = row + d.rowOffset;
            int offsetCol = col + d.colOffset;

            while (offsetRow >= 1 && offsetRow <= 8 && offsetCol >= 1 && offsetCol <= 8) {
                ChessPosition newPos = new ChessPosition(offsetRow, offsetCol);
                ChessPiece target = board.getPiece(newPos);

                if (target == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else if (target.getTeamColor() != rook.getTeamColor()) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                    break;
                } else if (target.getTeamColor() == rook.getTeamColor()) {
                    break;
                }

                offsetRow += d.rowOffset;
                offsetCol += d.colOffset;
            }
        }

        return moves;
    }
}
