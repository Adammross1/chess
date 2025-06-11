package websocket;

import chess.ChessGame;
import chess.ChessMove;
import com.google.gson.Gson;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import service.GameService;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;
import chess.InvalidMoveException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketHandler {
    private final GameService gameService;
    private final AuthDAO authDAO;
    private final Gson gson;
    // Map of gameID to Map of authToken to Session
    private final Map<Integer, Map<String, Session>> gameSessions = new ConcurrentHashMap<>();

    public WebSocketHandler(GameService gameService, AuthDAO authDAO, Gson gson) {
        this.gameService = gameService;
        this.authDAO = authDAO;
        this.gson = gson;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("WebSocket connected: " + session.getRemoteAddress().getAddress());
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("WebSocket closed: " + session.getRemoteAddress().getAddress());
        // Remove the session from all games
        for (Map<String, Session> sessions : gameSessions.values()) {
            sessions.values().remove(session);
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error: " + throwable.getMessage());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
            String authToken = command.getAuthToken();
            int gameID = command.getGameID();

            // Verify auth token
            AuthData authData = authDAO.getAuth(authToken);
            if (authData == null) {
                sendError(session, "Error: unauthorized");
                return;
            }

            // Get or create game sessions map
            Map<String, Session> sessions = gameSessions.computeIfAbsent(gameID, k -> new ConcurrentHashMap<>());

            switch (command.getCommandType()) {
                case CONNECT -> handleConnect(session, authToken, gameID, sessions);
                case MAKE_MOVE -> handleMakeMove(session, authToken, gameID, command.getMove(), sessions);
                case LEAVE -> handleLeave(session, authToken, gameID, sessions);
                case RESIGN -> handleResign(session, authToken, gameID, sessions);
                default -> sendError(session, "Error: unknown command");
            }
        } catch (Exception e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleConnect(Session session, String authToken, int gameID, Map<String, Session> sessions) {
        try {
            // Get game data
            GameData game = gameService.getGame(authToken, gameID);
            if (game == null) {
                sendError(session, "Error: game not found");
                return;
            }

            // Add session to game
            sessions.put(authToken, session);

            // Send LOAD_GAME message
            ServerMessage loadGameMessage = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME, game.game());
            sendMessage(session, gson.toJson(loadGameMessage));

            // Notify others
            ServerMessage notification = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION, 
                authDAO.getAuth(authToken).username() + " joined the game");
            broadcastMessage(gameID, authToken, gson.toJson(notification));
        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleMakeMove(Session session, String authToken, int gameID, ChessMove move, Map<String, Session> sessions) {
        try {
            // Get game data
            GameData game = gameService.getGame(authToken, gameID);
            if (game == null) {
                sendError(session, "Error: game not found");
                return;
            }

            // Verify it's the player's turn
            String username = authDAO.getAuth(authToken).username();
            if (!game.whiteUsername().equals(username) && !game.blackUsername().equals(username)) {
                sendError(session, "Error: not your turn");
                return;
            }

            // Make the move
            ChessGame chessGame = game.game();
            try {
                chessGame.makeMove(move);
            } catch (InvalidMoveException e) {
                sendError(session, "Error: " + e.getMessage());
                return;
            }

            // Update game in database
            gameService.updateGame(gameID, chessGame);

            // Send LOAD_GAME message to all clients
            ServerMessage loadGameMessage = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME, chessGame);
            broadcastMessage(gameID, null, gson.toJson(loadGameMessage));

            // Send notification
            ServerMessage notification = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION, 
                username + " made a move");
            broadcastMessage(gameID, null, gson.toJson(notification));
        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleLeave(Session session, String authToken, int gameID, Map<String, Session> sessions) {
        try {
            // Remove session
            sessions.remove(authToken);

            // Notify others
            String username = authDAO.getAuth(authToken).username();
            ServerMessage notification = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION, 
                username + " left the game");
            broadcastMessage(gameID, authToken, gson.toJson(notification));

            // Close session
            session.close();
        } catch (Exception e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleResign(Session session, String authToken, int gameID, Map<String, Session> sessions) {
        try {
            // Get game data
            GameData game = gameService.getGame(authToken, gameID);
            if (game == null) {
                sendError(session, "Error: game not found");
                return;
            }

            // Verify player is in the game
            String username = authDAO.getAuth(authToken).username();
            if (!game.whiteUsername().equals(username) && !game.blackUsername().equals(username)) {
                sendError(session, "Error: you are not a player in this game");
                return;
            }

            // Update game state
            ChessGame chessGame = game.game();
            chessGame.setTeamTurn(null); // Indicates game is over
            gameService.updateGame(gameID, chessGame);

            // Send LOAD_GAME message to all clients
            ServerMessage loadGameMessage = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME, chessGame);
            broadcastMessage(gameID, null, gson.toJson(loadGameMessage));

            // Send notification
            ServerMessage notification = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION, 
                username + " resigned the game");
            broadcastMessage(gameID, null, gson.toJson(notification));
        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void broadcastMessage(int gameID, String excludeAuthToken, String message) {
        Map<String, Session> sessions = gameSessions.get(gameID);
        if (sessions != null) {
            for (Map.Entry<String, Session> entry : sessions.entrySet()) {
                if (!entry.getKey().equals(excludeAuthToken)) {
                    sendMessage(entry.getValue(), message);
                }
            }
        }
    }

    private void sendMessage(Session session, String message) {
        try {
            session.getRemote().sendString(message);
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    private void sendError(Session session, String errorMessage) {
        ServerMessage error = new ServerMessage(ServerMessage.ServerMessageType.ERROR, errorMessage);
        sendMessage(session, gson.toJson(error));
    }
} 