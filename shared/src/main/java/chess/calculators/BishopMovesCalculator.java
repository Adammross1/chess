package chess.calculators;

import chess.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class BishopMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        if (board.getPiece(myPosition) == null) {
            return Collections.emptyList();
        }

        Collection<ChessMove> moves = new ArrayList<>();
        ChessPiece bishop = board.getPiece(myPosition);
        if (bishop.getPieceType() != ChessPiece.PieceType.BISHOP) {
            return moves;
        }

        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        record Direction(int rowOffset, int colOffset) {}

        Direction[] diagonals = {
                new Direction(1, 1),
                new Direction(1, -1),
                new Direction(-1, 1),
                new Direction(-1, -1)
        };

        for (Direction d : diagonals) {
            int offsetRow = row + d.rowOffset;
            int offsetCol = col + d.colOffset;

            while (offsetRow >= 1 && offsetRow <= 8 && offsetCol >= 1 && offsetCol <= 8) {
                ChessPosition newPos = new ChessPosition(offsetRow, offsetCol);
                ChessPiece target = board.getPiece(newPos);

                if (target == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else if (target.getTeamColor() != bishop.getTeamColor()) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                    break;
                } else if (target.getTeamColor() == bishop.getTeamColor()) {
                    break;
                }

                offsetRow += d.rowOffset;
                offsetCol += d.colOffset;
            }
        }

        return moves;
    }

    @Override
    public String toString() {
        return "BishopMovesCalculator{}";
    }
}
