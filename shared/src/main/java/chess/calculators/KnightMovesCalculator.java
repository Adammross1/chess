package chess.calculators;

import chess.*;

import java.util.ArrayList;
import java.util.Collection;

public class KnightMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition position) {
        Collection<ChessMove> moves = new ArrayList<>();
        ChessPiece knight = board.getPiece(position);
        if (knight == null || knight.getPieceType() != ChessPiece.PieceType.KNIGHT) return moves;

        int row = position.getRow();
        int col = position.getColumn();

        record Direction(int rowOffset, int colOffset) {}

        Direction[] knightMoves = {
                new Direction(2, 1),
                new Direction(1, 2),
                new Direction(-1, 2),
                new Direction(-2, 1),
                new Direction(-2, -1),
                new Direction(-1, -2),
                new Direction(1, -2),
                new Direction(2, -1)
        };

        for (Direction d : knightMoves) {
            int newRow = row + d.rowOffset;
            int newCol = col + d.colOffset;
            if (newRow >= 1 && newRow <= 8 && newCol >= 1 && newCol <= 8) {
                ChessPosition newPos = new ChessPosition(newRow, newCol);
                ChessPiece target = board.getPiece(newPos);
                if ((target == null || target.getTeamColor() != knight.getTeamColor())) {
                    moves.add(new ChessMove(position, newPos, null));
                }
            }
        }

        return moves;
    }
}
