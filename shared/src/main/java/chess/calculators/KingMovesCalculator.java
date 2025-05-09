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
            int newRow = row + d.rowOffset;
            int newCol = col + d.colOffset;
            if (newRow >= 1 && newRow <= 8 && newCol >= 1 && newCol <=8) {
                ChessPosition newPos = new ChessPosition(newRow, newCol);
                ChessPiece target = board.getPiece(newPos);
                if (target == null || target.getTeamColor() != king.getTeamColor()) {
                    moves.add(new ChessMove(position, newPos, null));
                }
            }
        }

        for (ChessMove move : moves) {
            System.out.printf("From (%d, %d) to (%d, %d)%n",
                    move.getStartPosition().getRow(),
                    move.getStartPosition().getColumn(),
                    move.getEndPosition().getRow(),
                    move.getEndPosition().getColumn());
        }
        return moves;
    }

}
