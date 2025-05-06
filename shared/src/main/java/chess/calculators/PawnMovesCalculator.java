package chess.calculators;

import chess.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PawnMovesCalculator implements PieceMovesCalculator {

    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition position) {
        Collection<ChessMove> moves = new ArrayList<>();
        ChessPiece pawn = board.getPiece(position);
        if (pawn == null || pawn.getPieceType() != ChessPiece.PieceType.PAWN) return moves;

        int direction = pawn.getTeamColor() == ChessGame.TeamColor.WHITE ? 1 : -1;
        int startRow = pawn.getTeamColor() == ChessGame.TeamColor.WHITE ? 2 : 7;

        int row = position.getRow();
        int col = position.getColumn();

        // normal move
        ChessPosition oneForward = new ChessPosition(row + direction, col);
        if (board.getPiece(oneForward) == null) {
            moves.add(new ChessMove(position, oneForward, null));

            // first move
            if (row == startRow) {
                ChessPosition twoForward = new ChessPosition(row + (direction*2), col);
                if (board.getPiece(twoForward) == null) {
                    moves.add(new ChessMove(position, twoForward, null));
                }
            }
        }

        // capture
        ChessPosition diagLeft = new ChessPosition(row + direction, col - 1);
        ChessPosition diagRight = new ChessPosition(row + direction, col + 1);
        ChessPiece target1 = board.getPiece(diagLeft);
        ChessPiece target2 = board.getPiece(diagRight);
        if (target1 != null && target1.getTeamColor() != pawn.getTeamColor()) {
            moves.add(new ChessMove(position, diagLeft, null));
        }
        if (target2 != null && target2.getTeamColor() != pawn.getTeamColor()) {
            moves.add(new ChessMove(position, diagRight, null));
        }

        return moves;
    }
}
