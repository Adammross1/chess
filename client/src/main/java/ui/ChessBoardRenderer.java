package ui;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessGame;
import chess.ChessPosition;
import java.util.Set;
import java.util.HashSet;

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

    private static Set<ChessPosition> highlightedSquares = new HashSet<>();

    public static void setHighlightedSquares(Set<ChessPosition> squares) {
        highlightedSquares = squares;
    }

    public static void clearHighlights() {
        highlightedSquares.clear();
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
                ChessPosition pos = new ChessPosition(row, col);
                boolean isHighlighted = highlightedSquares.contains(pos);
                printSquare(board, pos, (r + c) % 2 == 0, isHighlighted);
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

    private static void printSquare(ChessBoard board, ChessPosition pos, boolean lightSquare, boolean isHighlighted) {
        String bg;
        if (isHighlighted) {
            bg = lightSquare ? EscapeSequences.SET_BG_COLOR_GREEN : EscapeSequences.SET_BG_COLOR_DARK_GREEN;
        } else {
            bg = lightSquare ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY : EscapeSequences.SET_BG_COLOR_DARK_GREY;
        }

        // Get the piece, handling any exceptions
        ChessPiece piece = null;
        try {
            piece = board.getPiece(pos);
        } catch (Exception e) {
            System.out.print(bg + EscapeSequences.EMPTY);
            return;
        }

        // If no piece or piece type is null, print empty square
        if (piece == null || piece.getPieceType() == null) {
            System.out.print(bg + EscapeSequences.EMPTY);
            return;
        }

        // Get piece color, defaulting to white if null
        ChessGame.TeamColor pieceColor = piece.getTeamColor();
        if (pieceColor == null) {
            System.out.print(bg + EscapeSequences.EMPTY);
            return;
        }

        // Get the appropriate symbol based on piece type and color
        String symbol = EscapeSequences.EMPTY;
        String fg = pieceColor == ChessGame.TeamColor.WHITE ? 
            EscapeSequences.SET_TEXT_COLOR_WHITE : 
            EscapeSequences.SET_TEXT_COLOR_BLACK;

        try {
            switch (piece.getPieceType()) {
                case KING -> symbol = pieceColor == ChessGame.TeamColor.WHITE ? 
                    EscapeSequences.WHITE_KING : EscapeSequences.BLACK_KING;
                case QUEEN -> symbol = pieceColor == ChessGame.TeamColor.WHITE ? 
                    EscapeSequences.WHITE_QUEEN : EscapeSequences.BLACK_QUEEN;
                case ROOK -> symbol = pieceColor == ChessGame.TeamColor.WHITE ? 
                    EscapeSequences.WHITE_ROOK : EscapeSequences.BLACK_ROOK;
                case BISHOP -> symbol = pieceColor == ChessGame.TeamColor.WHITE ? 
                    EscapeSequences.WHITE_BISHOP : EscapeSequences.BLACK_BISHOP;
                case KNIGHT -> symbol = pieceColor == ChessGame.TeamColor.WHITE ? 
                    EscapeSequences.WHITE_KNIGHT : EscapeSequences.BLACK_KNIGHT;
                case PAWN -> symbol = pieceColor == ChessGame.TeamColor.WHITE ? 
                    EscapeSequences.WHITE_PAWN : EscapeSequences.BLACK_PAWN;
                default -> symbol = EscapeSequences.EMPTY;
            }
        } catch (Exception e) {
            // If any error occurs while getting piece type or color, print empty square
            symbol = EscapeSequences.EMPTY;
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