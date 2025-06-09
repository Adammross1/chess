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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.*;

public class Server {
    static {
        try {
            Handler fileHandler = new FileHandler("server.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            Logger rootLogger = Logger.getLogger("");
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.ALL);
        } catch (Exception e) {
            System.err.println("Failed to set up file logging: " + e.getMessage());
        }
    }

    private final UserHandler userHandler;
    private final GameHandler gameHandler;
    private final ClearHandler clearHandler;
    private final Gson gson;

    public Server() {
        try {
            // Initialize the database
            DatabaseManager.initializeDatabase();

            // Create MySQL DAOs
            UserDAO userDAO = new MySQLUserDAO();
            GameDAO gameDAO = new MySQLGameDAO();
            AuthDAO authDAO = new MySQLAuthDAO();

            UserService userService = new UserService(userDAO, authDAO);
            GameService gameService = new GameService(gameDAO, authDAO);
            ClearService clearService = new ClearService(userDAO, gameDAO, authDAO);

            userHandler = new UserHandler(userService);
            gson = new GsonBuilder()
                .registerTypeAdapter(ChessGame.class, new ChessGameAdapter())
                .registerTypeAdapter(ChessBoard.class, new ChessBoardAdapter())
                .registerTypeAdapter(ChessPiece.class, new ChessPieceAdapter())
                .create();
            gameHandler = new GameHandler(gameService, gson);
            clearHandler = new ClearHandler(clearService);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to initialize server: " + e.getMessage(), e);
        }
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        // Spark.staticFiles.location("web");

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
        Spark.post("/user", userHandler::register);
        Spark.post("/session", userHandler::login);
        Spark.delete("/session", userHandler::logout);

        Spark.get("/game/:id", gameHandler::getGame);
        Spark.get("/game", gameHandler::listGames);
        Spark.post("/game", gameHandler::createGame);
        Spark.put("/game", gameHandler::joinGame);

        Spark.delete("/db", clearHandler::clearApplication);

        Spark.get("/*", (req, res) -> {
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
