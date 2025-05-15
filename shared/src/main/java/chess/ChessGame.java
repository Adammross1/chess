package chess;

import java.util.ArrayList;
import java.util.Collection;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private ChessBoard board;
    private TeamColor teamTurn;

    public ChessGame() {
        this.board = new ChessBoard();
        board.resetBoard();
        this.teamTurn = TeamColor.WHITE;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    private boolean isValidMove(ChessMove move) {
        ChessPiece movingPiece = board.getPiece(move.getStartPosition());
        ChessPiece target = board.getPiece(move.getEndPosition());

        board.addPiece(move.getStartPosition(), null);
        board.addPiece(move.getEndPosition(), movingPiece);

        boolean validMove = !isInCheck(movingPiece.getTeamColor());

        board.addPiece(move.getStartPosition(), movingPiece);
        board.addPiece(move.getEndPosition(), target);

        return validMove;
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);

        if (piece == null) {
            return null;
        }

        Collection<ChessMove> possibleMoves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> validMoves = new ArrayList<>();

        for (ChessMove move : possibleMoves) {
            if (isValidMove(move)) {
                validMoves.add(move);
            }
        }

        return validMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition startPosition = move.getStartPosition();
        ChessPosition endPosition = move.getEndPosition();
        ChessPiece piece = board.getPiece(startPosition);

        if (piece == null) {
            throw new InvalidMoveException("There's no piece at the starting position");
        }

        if (piece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("It's not " + piece.getTeamColor() + "'s turn");
        }

        Collection<ChessMove> moves = validMoves(startPosition);
        if (!moves.contains(move)) {
            throw new InvalidMoveException("Invalid move");
        }

        board.addPiece(startPosition, null);

        ChessPiece.PieceType promotionPieceType = move.getPromotionPiece();
        if (promotionPieceType != null) {
            ChessPiece promotedPiece = new ChessPiece(piece.getTeamColor(), promotionPieceType);
            board.addPiece(endPosition, promotedPiece);
        } else {
            board.addPiece(endPosition, piece);
        }

        teamTurn = teamTurn == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
    }

    private ChessPosition findKing(TeamColor teamColor) {
        ChessPosition kingPosition = null;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);

                if (piece != null &&
                    piece.getTeamColor() == teamColor &&
                    piece.getPieceType() == ChessPiece.PieceType.KING) {
                    kingPosition = position;
                }
            }
        }

        return kingPosition;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPosition = findKing(teamColor);

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);

                if (piece != null && piece.getTeamColor() != teamColor) {
                    Collection<ChessMove> moves = piece.pieceMoves(board, position);

                    for (ChessMove move : moves) {
                        if (move.getEndPosition() == kingPosition) {
                            return true;
                        }
                    }
                }

            }
        }

        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        boolean inCheck = isInCheck(teamColor);
        if (!inCheck) {
            return false;
        }

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);

                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = validMoves(position);
                    if (moves != null && !moves.isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {

    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return this.board;
    }
}
