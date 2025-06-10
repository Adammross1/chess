package server;

import javax.websocket.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
        UserGameCommand connectCommand = new UserGameCommand("CONNECT", authToken, gameID);
        String json = gson.toJson(connectCommand);
        session.getBasicRemote().sendText(json);
    }

    /**
     * For testing: returns true if the WebSocket is open.
     */
    public boolean isOpen() {
        return session != null && session.isOpen();
    }

    // Add more methods for sending/receiving gameplay commands as needed.
}

// Dummy UserGameCommand for demonstration. Replace with actual implementation if available.
class UserGameCommand {
    private final String commandType;
    private final String authToken;
    private final int gameID;

    public UserGameCommand(String commandType, String authToken, int gameID) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
    }
} 