package websocket.messages;

import java.util.Objects;
import chess.ChessGame;

/**
 * Represents a Message the server can send through a WebSocket
 * 
 * Note: You can add to this class, but you should not alter the existing
 * methods.
 */
public class ServerMessage {
    ServerMessageType serverMessageType;
    private ChessGame game;
    private String errorMessage;
    private String message;

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    public ServerMessage(ServerMessageType type, ChessGame game) {
        this.serverMessageType = type;
        this.game = game;
    }

    public ServerMessage(ServerMessageType type, String errorMessage, boolean isError) {
        this.serverMessageType = type;
        if (isError) {
            this.errorMessage = errorMessage;
        } else {
            this.message = errorMessage;
        }
    }

    public ServerMessage(ServerMessageType type, String message) {
        this.serverMessageType = type;
        this.message = message;
    }

    // No-args constructor for deserialization
    public ServerMessage() {
        this.serverMessageType = null;
        this.game = null;
        this.errorMessage = null;
        this.message = null;
    }

    public ServerMessageType getServerMessageType() {
        return this.serverMessageType;
    }

    public ChessGame getGame() {
        return game;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerMessage)) {
            return false;
        }
        ServerMessage that = (ServerMessage) o;
        return getServerMessageType() == that.getServerMessageType() &&
                Objects.equals(getGame(), that.getGame()) &&
                Objects.equals(getErrorMessage(), that.getErrorMessage()) &&
                Objects.equals(getMessage(), that.getMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServerMessageType(), getGame(), getErrorMessage(), getMessage());
    }
}
