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
        int promotionRow = pawn.getTeamColor() == ChessGame.TeamColor.WHITE ? 8 : 1;

        int row = position.getRow();
        int col = position.getColumn();
        int newRow = row + direction;

        if (newRow >= 1 && newRow <= 8) {

            // normal move
            ChessPosition oneForward = new ChessPosition(newRow, col);
            if (board.getPiece(oneForward) == null) {

                if (oneForward.getRow() == promotionRow) {
                    moves.add(new ChessMove(position, oneForward, ChessPiece.PieceType.QUEEN));
                    moves.add(new ChessMove(position, oneForward, ChessPiece.PieceType.ROOK));
                    moves.add(new ChessMove(position, oneForward, ChessPiece.PieceType.BISHOP));
                    moves.add(new ChessMove(position, oneForward, ChessPiece.PieceType.KNIGHT));
                } else {
                    moves.add(new ChessMove(position, oneForward, null));
                }

                // first move
                if (row == startRow) {
                    ChessPosition twoForward = new ChessPosition(row + (direction*2), col);
                    if (board.getPiece(twoForward) == null) {
                        moves.add(new ChessMove(position, twoForward, null));
                    }
                }
            }

// Capture left
            if (col - 1 >= 1) {
                ChessPosition diagLeft = new ChessPosition(row + direction, col - 1);
                ChessPiece target1 = board.getPiece(diagLeft);
                if (target1 != null && target1.getTeamColor() != pawn.getTeamColor()) {
                    if (diagLeft.getRow() == promotionRow) {
                        moves.add(new ChessMove(position, diagLeft, ChessPiece.PieceType.QUEEN));
                        moves.add(new ChessMove(position, diagLeft, ChessPiece.PieceType.ROOK));
                        moves.add(new ChessMove(position, diagLeft, ChessPiece.PieceType.BISHOP));
                        moves.add(new ChessMove(position, diagLeft, ChessPiece.PieceType.KNIGHT));
                    } else {
                        moves.add(new ChessMove(position, diagLeft, null));
                    }
                }
            }

// Capture right
            if (col + 1 <= 8) {
                ChessPosition diagRight = new ChessPosition(row + direction, col + 1);
                ChessPiece target2 = board.getPiece(diagRight);
                if (target2 != null && target2.getTeamColor() != pawn.getTeamColor()) {
                    if (diagRight.getRow() == promotionRow) {
                        moves.add(new ChessMove(position, diagRight, ChessPiece.PieceType.QUEEN));
                        moves.add(new ChessMove(position, diagRight, ChessPiece.PieceType.ROOK));
                        moves.add(new ChessMove(position, diagRight, ChessPiece.PieceType.BISHOP));
                        moves.add(new ChessMove(position, diagRight, ChessPiece.PieceType.KNIGHT));
                    } else {
                        moves.add(new ChessMove(position, diagRight, null));
                    }
                }
            }
        }

        return moves;
    }
}
