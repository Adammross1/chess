package websocket;

import chess.ChessGame;
import chess.ChessMove;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.ChessGameAdapter;
import dataaccess.ChessBoardAdapter;
import dataaccess.ChessPieceAdapter;
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
        this.gson = gson;  // Use the passed-in Gson instance that already has the adapters registered
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
        System.out.println("WEBSOCKET: Received message: " + message);
        try {
            UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
            String authToken = command.getAuthToken();
            int gameID = command.getGameID();

            System.out.println("WEBSOCKET: Command type: " + command.getCommandType());

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
                case RESIGN -> {
                    System.out.println("WEBSOCKET: Handling resign command");
                    handleResign(session, authToken, gameID, sessions);
                }
                default -> sendError(session, "Error: unknown command");
            }
        } catch (Exception e) {
            System.out.println("WEBSOCKET: Error handling message: " + e.getMessage());
            e.printStackTrace();
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
                sendError(session, "Error: observers cannot make moves");
                return;
            }

            // Check if game is over
            ChessGame chessGame = game.game();
            if (chessGame.getGameState() != ChessGame.GameState.ACTIVE) {
                sendError(session, "Error: game is over");
                return;
            }

            // Verify it's the player's turn
            ChessGame.TeamColor playerColor = game.whiteUsername().equals(username) ? 
                ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;
            if (chessGame.getTeamTurn() != playerColor) {
                sendError(session, "Error: not your turn");
                return;
            }

            // Make the move
            try {
                chessGame.makeMove(move);
            } catch (InvalidMoveException e) {
                sendError(session, "Error: " + e.getMessage());
                return;
            }

            // Check for game over conditions
            String gameOverMessage = null;
            if (chessGame.getGameState() == ChessGame.GameState.CHECKMATE) {
                gameOverMessage = "Game over: " + chessGame.getTeamTurn() + " is in checkmate!";
            } else if (chessGame.getGameState() == ChessGame.GameState.STALEMATE) {
                gameOverMessage = "Game over: Stalemate!";
            }

            // Update game in database
            gameService.updateGame(gameID, chessGame);

            // Send LOAD_GAME message to all clients
            ServerMessage loadGameMessage = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME, chessGame);
            broadcastMessage(gameID, null, gson.toJson(loadGameMessage));

            // Send move notification
            ServerMessage moveNotification = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION, 
                username + " made a move");
            broadcastMessage(gameID, null, gson.toJson(moveNotification));

            // Send game over notification if applicable
            if (gameOverMessage != null) {
                ServerMessage gameOverNotification = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION, 
                    gameOverMessage);
                broadcastMessage(gameID, null, gson.toJson(gameOverNotification));
            }

            // Send check notification if applicable
            if (chessGame.isInCheck(ChessGame.TeamColor.WHITE)) {
                ServerMessage checkNotification = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION, 
                    "White is in check");
                broadcastMessage(gameID, null, gson.toJson(checkNotification));
            } else if (chessGame.isInCheck(ChessGame.TeamColor.BLACK)) {
                ServerMessage checkNotification = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION, 
                    "Black is in check");
                broadcastMessage(gameID, null, gson.toJson(checkNotification));
            }
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
                sendError(session, "Error: observers cannot resign");
                return;
            }

            // Check if game is already over
            ChessGame chessGame = game.game();
            System.out.println("TEAM_TURN: Initial state - " + chessGame.getTeamTurn());
            
            if (chessGame.getGameState() != ChessGame.GameState.ACTIVE) {
                sendError(session, "Error: game is already over");
                return;
            }

            // Check if the player has already resigned
            if (chessGame.getGameState() == ChessGame.GameState.RESIGNED) {
                sendError(session, "Error: you have already resigned");
                return;
            }

            // Set team turn to the resigning player's color BEFORE setting game state
            ChessGame.TeamColor resigningColor = game.whiteUsername().equals(username) ? 
                ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;
            System.out.println("TEAM_TURN: Setting to resigning color - " + resigningColor);
            chessGame.setTeamTurn(resigningColor);
            System.out.println("TEAM_TURN: After setting - " + chessGame.getTeamTurn());
            
            // Now set the game state
            chessGame.setGameState(ChessGame.GameState.RESIGNED);
            System.out.println("TEAM_TURN: After setting game state - " + chessGame.getTeamTurn());
            
            // Update game in database
            gameService.updateGame(gameID, chessGame);
            System.out.println("TEAM_TURN: After database update - " + chessGame.getTeamTurn());

            // Send LOAD_GAME message to all clients
            ServerMessage loadGameMessage = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME, chessGame);
            String loadGameJson = gson.toJson(loadGameMessage);
            System.out.println("TEAM_TURN: Before sending LOAD_GAME - " + chessGame.getTeamTurn());
            System.out.println("TEAM_TURN: LOAD_GAME JSON - " + loadGameJson);
            broadcastMessage(gameID, null, loadGameJson);

            // Send resignation notification
            String teamColor = game.whiteUsername().equals(username) ? "White" : "Black";
            ServerMessage notification = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION, 
                teamColor + " player (" + username + ") has resigned the game");
            broadcastMessage(gameID, null, gson.toJson(notification));
        } catch (DataAccessException e) {
            System.out.println("TEAM_TURN: Error during resignation - " + e.getMessage());
            e.printStackTrace();
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