package server;

import chess.ChessBoard;

/**
 * Interface for handling board updates from the WebSocket connection.
 * Implemented by UI classes that need to update their display when the board changes.
 */
public interface BoardUpdateHandler {
    /**
     * Called when the board state is updated from the server.
     * @param board The updated chess board
     * @param perspective The perspective to view the board from ("white", "black", or "observer")
     */
    void updateBoard(ChessBoard board, String perspective);
} 