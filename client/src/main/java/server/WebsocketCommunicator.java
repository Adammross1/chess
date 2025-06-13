package server;

import javax.websocket.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import websocket.messages.ServerMessage;
import chess.ChessGame;
import chess.ChessBoard;
import chess.ChessPiece;
import websocket.commands.UserGameCommand;
import chess.ChessMove;

/**
 * Manages a persistent WebSocket connection to the server for gameplay communication.
 * Opens a connection to /ws, sends a CONNECT UserGameCommand, and provides error handling.
 */
@ClientEndpoint
public class WebsocketCommunicator {
    private Session session;
    private final String serverUrl;
    private final Gson gson;
    private final CountDownLatch connectLatch = new CountDownLatch(1);
    private BoardUpdateHandler boardUpdateHandler;
    private NotificationHandler notificationHandler;
    private String playerPerspective = "observer";

    public interface NotificationHandler {
        void onNotification(String message);
    }

    public interface BoardUpdateHandler {
        void updateGameState(ChessBoard board, ChessGame game, String perspective);
    }

    public WebsocketCommunicator(String serverUrl) {
        this.serverUrl = serverUrl;
        // Create Gson instance with our custom adapters
        this.gson = new GsonBuilder()
            .registerTypeAdapter(ChessGame.class, new ChessGameAdapter())
            .registerTypeAdapter(ChessBoard.class, new ChessBoardAdapter())
            .registerTypeAdapter(ChessPiece.class, new ChessPieceAdapter())
            .create();
    }

    /**
     * Opens a persistent WebSocket connection to the /ws endpoint and sends a CONNECT command.
     * @param authToken The user's auth token
     * @param gameID The game ID to connect to
     * @throws Exception if connection fails
     */
    public void connect(String authToken, int gameID) throws Exception {
        String wsUrl = serverUrl.replaceFirst("^http", "ws") + "/ws";
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        try {
            container.connectToServer(this, new URI(wsUrl));
            if (!connectLatch.await(5, TimeUnit.SECONDS)) {
                throw new IOException("WebSocket connection timeout");
            }
            sendConnectCommand(authToken, gameID);
        } catch (Exception e) {
            throw new IOException("Failed to connect to WebSocket: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a CONNECT UserGameCommand to the server.
     */
    private void sendConnectCommand(String authToken, int gameID) throws IOException {
        UserGameCommand connectCommand = new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID);
        String json = gson.toJson(connectCommand);
        session.getBasicRemote().sendText(json);
    }

    public void setBoardUpdateHandler(BoardUpdateHandler handler) {
        this.boardUpdateHandler = handler;
    }

    public void setPlayerPerspective(String perspective) {
        this.playerPerspective = perspective.toLowerCase();
    }

    public void sendLeaveCommand(String authToken, int gameID) throws IOException {
        UserGameCommand leaveCommand = new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, gameID);
        String json = gson.toJson(leaveCommand);
        if (session != null && session.isOpen()) {
            session.getBasicRemote().sendText(json);
        }
    }

    /**
     * Sends a MAKE_MOVE UserGameCommand to the server.
     * @param authToken The user's auth token
     * @param gameID The game ID
     * @param move The chess move to make
     * @throws IOException if the WebSocket send fails
     */
    public void sendMakeMoveCommand(String authToken, int gameID, ChessMove move) throws IOException {
        UserGameCommand moveCommand = new UserGameCommand(UserGameCommand.CommandType.MAKE_MOVE, authToken, gameID, move);
        String json = gson.toJson(moveCommand);
        if (session != null && session.isOpen()) {
            session.getBasicRemote().sendText(json);
        } else {
            throw new IOException("WebSocket is not connected");
        }
    }

    /**
     * Sends a RESIGN UserGameCommand to the server.
     * @param authToken The user's auth token
     * @param gameID The game ID
     * @throws IOException if the WebSocket send fails
     */
    public void sendResignCommand(String authToken, int gameID) throws IOException {
        UserGameCommand resignCommand = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameID);
        String json = gson.toJson(resignCommand);
        if (session != null && session.isOpen()) {
            session.getBasicRemote().sendText(json);
        } else {
            throw new IOException("WebSocket is not connected");
        }
    }

    /**
     * Closes the WebSocket connection and stops receiving updates.
     */
    public void closeAndCleanup() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            // Ignore
        }
        session = null;
        boardUpdateHandler = null;
    }

    // Add more methods for sending/receiving gameplay commands as needed.
}

// Move BoardUpdateHandler to its own file 