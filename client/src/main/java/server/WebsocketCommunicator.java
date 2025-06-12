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
import ui.ChessBoardRenderer;
import websocket.commands.UserGameCommand;
import chess.ChessMove;
import chess.ChessPosition;

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
        this.playerPerspective = perspective.toLowerCase();
    }

    public void setNotificationHandler(NotificationHandler handler) {
        this.notificationHandler = handler;
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("CLIENT: Received message from server: " + message);
        try {
            ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);
            System.out.println("CLIENT: Message type: " + serverMessage.getServerMessageType());
            
            switch (serverMessage.getServerMessageType()) {
                case LOAD_GAME -> {
                    ChessGame game = serverMessage.getGame();
                    System.out.println("CLIENT: Received game state - teamTurn: " + (game != null ? game.getTeamTurn() : "null"));
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
                        boardUpdateHandler.updateGameState(board, game, playerPerspective);
                    }
                }
                case ERROR -> {
                    String errorMessage = serverMessage.getMessage();
                    if (errorMessage == null) {
                        errorMessage = "Error: Unknown error occurred";
                    } else if (!errorMessage.toLowerCase().contains("error")) {
                        errorMessage = "Error: " + errorMessage;
                    }
                    System.err.println(errorMessage);
                }
                case NOTIFICATION -> {
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
            System.err.println("CLIENT: Error processing message: " + e.getMessage());
            e.printStackTrace();
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
        System.out.println("CLIENT: Sending resign command...");
        UserGameCommand resignCommand = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameID);
        String json = gson.toJson(resignCommand);
        System.out.println("CLIENT: Resign command JSON: " + json);
        if (session != null && session.isOpen()) {
            session.getBasicRemote().sendText(json);
            System.out.println("CLIENT: Resign command sent successfully");
        } else {
            System.out.println("CLIENT: Error - WebSocket is not connected");
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