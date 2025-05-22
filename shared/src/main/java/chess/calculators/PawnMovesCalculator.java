package chess.calculators;

import chess.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PawnMovesCalculator implements PieceMovesCalculator {

    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        if (board.getPiece(myPosition) == null) {
            return Collections.emptyList();
        }

        Collection<ChessMove> moves = new ArrayList<>();
        ChessPiece pawn = board.getPiece(myPosition);
        if (pawn.getPieceType() != ChessPiece.PieceType.PAWN) return moves;

        int direction = pawn.getTeamColor() == ChessGame.TeamColor.WHITE ? 1 : -1;
        int startRow = pawn.getTeamColor() == ChessGame.TeamColor.WHITE ? 2 : 7;
        int promotionRow = pawn.getTeamColor() == ChessGame.TeamColor.WHITE ? 8 : 1;

        int row = myPosition.getRow();
        int col = myPosition.getColumn();
        int newRow = row + direction;

        if (newRow >= 1 && newRow <= 8) {

            // normal move
            ChessPosition oneForward = new ChessPosition(newRow, col);
            if (board.getPiece(oneForward) == null) {

                if (oneForward.getRow() == promotionRow) {
                    moves.add(new ChessMove(myPosition, oneForward, ChessPiece.PieceType.QUEEN));
                    moves.add(new ChessMove(myPosition, oneForward, ChessPiece.PieceType.ROOK));
                    moves.add(new ChessMove(myPosition, oneForward, ChessPiece.PieceType.BISHOP));
                    moves.add(new ChessMove(myPosition, oneForward, ChessPiece.PieceType.KNIGHT));
                } else {
                    moves.add(new ChessMove(myPosition, oneForward, null));
                }

                // first move
                if (row == startRow) {
                    ChessPosition twoForward = new ChessPosition(row + (direction*2), col);
                    if (board.getPiece(twoForward) == null) {
                        moves.add(new ChessMove(myPosition, twoForward, null));
                    }
                }
            }

            // Capture left
            if (col - 1 >= 1) {
                ChessPosition diagLeft = new ChessPosition(row + direction, col - 1);
                ChessPiece target1 = board.getPiece(diagLeft);
                if (target1 != null && target1.getTeamColor() != pawn.getTeamColor()) {
                    if (diagLeft.getRow() == promotionRow) {
                        moves.add(new ChessMove(myPosition, diagLeft, ChessPiece.PieceType.QUEEN));
                        moves.add(new ChessMove(myPosition, diagLeft, ChessPiece.PieceType.ROOK));
                        moves.add(new ChessMove(myPosition, diagLeft, ChessPiece.PieceType.BISHOP));
                        moves.add(new ChessMove(myPosition, diagLeft, ChessPiece.PieceType.KNIGHT));
                    } else {
                        moves.add(new ChessMove(myPosition, diagLeft, null));
                    }
                }
            }

// Capture right
            if (col + 1 <= 8) {
                ChessPosition diagRight = new ChessPosition(row + direction, col + 1);
                ChessPiece target2 = board.getPiece(diagRight);
                if (target2 != null && target2.getTeamColor() != pawn.getTeamColor()) {
                    if (diagRight.getRow() == promotionRow) {
                        moves.add(new ChessMove(myPosition, diagRight, ChessPiece.PieceType.QUEEN));
                        moves.add(new ChessMove(myPosition, diagRight, ChessPiece.PieceType.ROOK));
                        moves.add(new ChessMove(myPosition, diagRight, ChessPiece.PieceType.BISHOP));
                        moves.add(new ChessMove(myPosition, diagRight, ChessPiece.PieceType.KNIGHT));
                    } else {
                        moves.add(new ChessMove(myPosition, diagRight, null));
                    }
                }
            }
        }

        return moves;
    }
}
