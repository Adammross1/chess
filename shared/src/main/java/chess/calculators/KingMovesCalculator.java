package chess.calculators;

import chess.*;

import java.util.ArrayList;
import java.util.Collection;

public class KingMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition position) {
        Collection<ChessMove> moves = new ArrayList<>();
        ChessPiece king = board.getPiece(position);
        if (king == null || king.getPieceType() != ChessPiece.PieceType.KING) return moves;

        int row = position.getRow();
        int col = position.getColumn();

        record Direction(int rowOffset, int colOffset) {}

        Direction[] kingMoves = {
                new Direction(1, 0),
                new Direction(-1, 0),
                new Direction(0, -1),
                new Direction(0, 1),
                new Direction(1, -1),
                new Direction(1, 1),
                new Direction(-1, -1),
                new Direction(-1, 1)
        };

        for (Direction d : kingMoves) {
            ChessPosition newPos = new ChessPosition(row + d.rowOffset(), col + d.colOffset());
            ChessPiece target = board.getPiece(newPos);
            if (target == null || target.getTeamColor() != king.getTeamColor()) {
                moves.add(new ChessMove(position, newPos, null));
            }
        }

        return moves;
    }
}
