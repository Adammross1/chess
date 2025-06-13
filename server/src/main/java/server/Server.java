package server;

import dataaccess.*;
import server.handlers.ClearHandler;
import server.handlers.GameHandler;
import server.handlers.UserHandler;
import service.ClearService;
import service.GameService;
import service.UserService;
import spark.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import chess.ChessGame;
import chess.ChessBoard;
import chess.ChessPiece;
import dataaccess.ChessGameAdapter;
import dataaccess.ChessBoardAdapter;
import dataaccess.ChessPieceAdapter;
import websocket.WebSocketHandler;

public class Server {
    private final UserHandler userHandler;
    private final GameHandler gameHandler;
    private final ClearHandler clearHandler;
    private final Gson gson;
    private final WebSocketHandler webSocketHandler;

    public Server() {
        try {
            // Initialize the database
            DatabaseManager.initializeDatabase();

            // Create Gson instance with our custom adapters
            System.out.println("TEAM_TURN: Server - Creating Gson instance with adapters");
            gson = new GsonBuilder()
                .registerTypeAdapter(ChessGame.class, new ChessGameAdapter())
                .registerTypeAdapter(ChessBoard.class, new ChessBoardAdapter())
                .registerTypeAdapter(ChessPiece.class, new ChessPieceAdapter())
                .create();
            System.out.println("TEAM_TURN: Server - Gson instance created");

            // Create MySQL DAOs
            UserDAO userDAO = new MySQLUserDAO();
            GameDAO gameDAO = new MySQLGameDAO(gson);
            AuthDAO authDAO = new MySQLAuthDAO();

            UserService userService = new UserService(userDAO, authDAO);
            GameService gameService = new GameService(gameDAO, authDAO);
            ClearService clearService = new ClearService(userDAO, gameDAO, authDAO);

            userHandler = new UserHandler(userService);
            gameHandler = new GameHandler(gameService, gson);
            clearHandler = new ClearHandler(clearService);
            webSocketHandler = new WebSocketHandler(gameService, authDAO, gson, gameDAO);
            System.out.println("TEAM_TURN: Server - All handlers initialized with Gson instance");
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to initialize server: " + e.getMessage(), e);
        }
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        registerEndpoints();

        setupExceptionHandling();

        Spark.before((req, res) -> {
            String logMsg = "Incoming request: " + req.requestMethod() + " " + req.pathInfo();
            System.out.println(logMsg);
        });

        Spark.awaitInitialization();
        return Spark.port();
    }

    private void registerEndpoints() {
        // Register WebSocket endpoint first
        Spark.webSocket("/ws", webSocketHandler);

        // Register HTTP endpoints
        Spark.post("/user", userHandler::register);
        Spark.post("/session", userHandler::login);
        Spark.delete("/session", userHandler::logout);

        Spark.get("/game/:id", gameHandler::getGame);
        Spark.get("/game", gameHandler::listGames);
        Spark.post("/game", gameHandler::createGame);
        Spark.put("/game", gameHandler::joinGame);

        Spark.delete("/db", clearHandler::clearApplication);

        // Catch-all handler for unmatched GET requests, but exclude /ws
        Spark.get("/*", (req, res) -> {
            // Skip WebSocket endpoint
            if (req.pathInfo().equals("/ws")) {
                return null;
            }
            String logMsg = "UNMATCHED GET: " + req.pathInfo();
            System.out.println(logMsg);
            res.status(404);
            return "{\"message\": \"Not found\"}";
        });
    }

    private void setupExceptionHandling() {
        Spark.exception(Exception.class, (e, req, res) -> {
            String logMsg = "Exception: " + e.getMessage();
            System.err.println(logMsg);
            res.status(500);
            res.body("{ \"message\": \"Error: " + e.getMessage() + "\" }");
        });
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
