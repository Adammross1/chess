package server;

import javax.websocket.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import websocket.messages.ServerMessage;
import chess.ChessGame;
import chess.ChessBoard;
import ui.ChessBoardRenderer;
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
    private final Gson gson = new Gson();
    private final CountDownLatch connectLatch = new CountDownLatch(1);
    private BoardUpdateHandler boardUpdateHandler;
    private NotificationHandler notificationHandler;
    private String playerPerspective = "observer";

    public interface NotificationHandler {
        void onNotification(String message);
    }

    public WebsocketCommunicator(String serverUrl) {
        this.serverUrl = serverUrl;
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

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        connectLatch.countDown();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error: " + throwable.getMessage());
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("WebSocket closed: " + closeReason);
    }

    /**
     * Sends a CONNECT UserGameCommand to the server.
     */
    private void sendConnectCommand(String authToken, int gameID) throws IOException {
        UserGameCommand connectCommand = new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID);
        String json = gson.toJson(connectCommand);
        session.getBasicRemote().sendText(json);
    }

    /**
     * For testing: returns true if the WebSocket is open.
     */
    public boolean isOpen() {
        return session != null && session.isOpen();
    }

    public void setBoardUpdateHandler(BoardUpdateHandler handler) {
        this.boardUpdateHandler = handler;
    }

    public void setPlayerPerspective(String perspective) {
        this.playerPerspective = perspective;
    }

    public void setNotificationHandler(NotificationHandler handler) {
        this.notificationHandler = handler;
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            // Deserialize the server message using the custom deserializer
            ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);
            
            switch (serverMessage.getServerMessageType()) {
                case LOAD_GAME -> {
                    ChessGame game = serverMessage.getGame();
                    if (game == null) {
                        System.err.println("Error: Received null game state from server");
                        return;
                    }
                    
                    ChessBoard board = game.getBoard();
                    if (board == null) {
                        System.err.println("Error: Game board is null");
                        return;
                    }
                    
                    if (boardUpdateHandler != null) {
                        boardUpdateHandler.updateBoard(board, playerPerspective);
                    } else {
                        // Fallback: render directly
                        ChessBoardRenderer.render(board, playerPerspective);
                    }
                }
                case ERROR -> {
                    // Display error message to user
                    String errorMessage = serverMessage.getMessage();
                    if (errorMessage == null) {
                        errorMessage = "Error: Unknown error occurred";
                    } else if (!errorMessage.toLowerCase().contains("error")) {
                        errorMessage = "Error: " + errorMessage;
                    }
                    System.err.println(errorMessage);
                }
                case NOTIFICATION -> {
                    // Display notification message to user
                    String notification = serverMessage.getMessage();
                    if (notification != null) {
                        System.out.println(notification);
                        if (notificationHandler != null) {
                            notificationHandler.onNotification(notification);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Handle any deserialization or processing errors
            System.err.println("Error processing server message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getMessage());
            }
        }
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