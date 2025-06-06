package ui;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessGame;

public class ChessBoardRenderer {
    public enum Perspective {
        WHITE, BLACK, OBSERVER;
        public static Perspective fromString(String s) {
            if (s == null) return OBSERVER;
            return switch (s.trim().toLowerCase()) {
                case "white" -> WHITE;
                case "black" -> BLACK;
                case "observer" -> OBSERVER;
                default -> OBSERVER;
            };
        }
    }

    public static void render(ChessBoard board, String perspectiveStr) {
        if (board == null) {
            System.out.println("Could not display board: board is missing.");
            return;
        }
        Perspective perspective = Perspective.fromString(perspectiveStr);
        boolean whiteBottom = (perspective == Perspective.WHITE || perspective == Perspective.OBSERVER);

        // Print column letters
        printColumnLabels(whiteBottom);
        for (int r = 0; r < 8; r++) {
            int row = whiteBottom ? 8 - r : r + 1;
            System.out.print(" " + row + " ");
            for (int c = 0; c < 8; c++) {
                int col = whiteBottom ? c + 1 : 8 - c;
                printSquare(board, row, col, (r + c) % 2 == 0);
            }
            System.out.print(" " + row);
            System.out.println(EscapeSequences.RESET_BG_COLOR + EscapeSequences.RESET_TEXT_COLOR);
        }
        printColumnLabels(whiteBottom);
    }

    private static void printColumnLabels(boolean whiteBottom) {
        System.out.print("   ");
        for (int c = 0; c < 8; c++) {
            char colChar = (char) ((whiteBottom ? 'a' + c : 'h' - c));
            System.out.print(" " + colChar + " ");
        }
        System.out.println();
    }

    private static void printSquare(ChessBoard board, int row, int col, boolean lightSquare) {
        String bg = lightSquare ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY : EscapeSequences.SET_BG_COLOR_DARK_GREY;
        ChessPiece piece = null;
        try {
            piece = board.getPiece(new chess.ChessPosition(row, col));
        } catch (Exception e) {
            System.out.print(bg + EscapeSequences.EMPTY);
            return;
        }
        String symbol = EscapeSequences.EMPTY;
        String fg = EscapeSequences.RESET_TEXT_COLOR;
        if (piece != null) {
            switch (piece.getPieceType()) {
                case KING -> symbol = (piece.getTeamColor() == ChessGame.TeamColor.WHITE) ? EscapeSequences.WHITE_KING : EscapeSequences.BLACK_KING;
                case QUEEN -> symbol = (piece.getTeamColor() == ChessGame.TeamColor.WHITE) ? EscapeSequences.WHITE_QUEEN : EscapeSequences.BLACK_QUEEN;
                case ROOK -> symbol = (piece.getTeamColor() == ChessGame.TeamColor.WHITE) ? EscapeSequences.WHITE_ROOK : EscapeSequences.BLACK_ROOK;
                case BISHOP -> symbol = (piece.getTeamColor() == ChessGame.TeamColor.WHITE) ? EscapeSequences.WHITE_BISHOP : EscapeSequences.BLACK_BISHOP;
                case KNIGHT -> symbol = (piece.getTeamColor() == ChessGame.TeamColor.WHITE) ? EscapeSequences.WHITE_KNIGHT : EscapeSequences.BLACK_KNIGHT;
                case PAWN -> symbol = (piece.getTeamColor() == ChessGame.TeamColor.WHITE) ? EscapeSequences.WHITE_PAWN : EscapeSequences.BLACK_PAWN;
            }
            fg = (piece.getTeamColor() == ChessGame.TeamColor.WHITE) ? EscapeSequences.SET_TEXT_COLOR_WHITE : EscapeSequences.SET_TEXT_COLOR_BLACK;
        }
        System.out.print(bg + fg + symbol + EscapeSequences.RESET_TEXT_COLOR + EscapeSequences.RESET_BG_COLOR);
    }

    // Demo main method
    public static void main(String[] args) {
        System.out.println("White/Observer Perspective:");
        ChessBoard board = new chess.ChessBoard();
        board.resetBoard();
        render(board, "white");
        System.out.println();
        System.out.println("Black Perspective:");
        render(board, "black");
    }
} 