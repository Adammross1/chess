package chess.calculators;

import chess.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class QueenMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition position) {
        if (board.getPiece(position) == null) {
            return Collections.emptyList();
        }

        Collection<ChessMove> moves = new ArrayList<>();
        ChessPiece queen = board.getPiece(position);
        if (queen == null || queen.getPieceType() != ChessPiece.PieceType.QUEEN) {
            return moves;
        }

        int row = position.getRow();
        int col = position.getColumn();

        record Direction(int rowOffset, int colOffset) {}

        Direction[] queenMoves = {
                new Direction(1, 0),
                new Direction(-1, 0),
                new Direction(0, 1),
                new Direction(0, -1),
                new Direction(1, 1),
                new Direction(1, -1),
                new Direction(-1, 1),
                new Direction(-1, -1)
        };

        for (Direction d : queenMoves) {
            int offsetRow = row + d.rowOffset;
            int offsetCol = col + d.colOffset;

            while (offsetRow >= 1 && offsetRow <= 8 && offsetCol >= 1 && offsetCol <= 8) {
                ChessPosition newPos = new ChessPosition(offsetRow, offsetCol);
                ChessPiece target = board.getPiece(newPos);

                if (target == null) {
                    moves.add(new ChessMove(position, newPos, null));
                } else if (target.getTeamColor() != queen.getTeamColor()) {
                    moves.add(new ChessMove(position, newPos, null));
                    break;
                } else if (target.getTeamColor() == queen.getTeamColor()) {
                    break;
                }

                offsetRow += d.rowOffset;
                offsetCol += d.colOffset;
            }
        }

        return moves;
    }
}
