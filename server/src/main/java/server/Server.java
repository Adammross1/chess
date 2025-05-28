package server;

import dataaccess.*;
import server.handlers.ClearHandler;
import server.handlers.GameHandler;
import server.handlers.UserHandler;
import service.ClearService;
import service.GameService;
import service.UserService;
import spark.*;

public class Server {
    private final UserHandler userHandler;
    private final GameHandler gameHandler;
    private final ClearHandler clearHandler;

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
            gameHandler = new GameHandler(gameService);
            clearHandler = new ClearHandler(clearService);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to initialize server: " + e.getMessage(), e);
        }
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        registerEndpoints();

        setupExceptionHandling();

        Spark.awaitInitialization();
        return Spark.port();
    }

    private void registerEndpoints() {
        Spark.post("/user", userHandler::register);
        Spark.post("/session", userHandler::login);
        Spark.delete("/session", userHandler::logout);

        Spark.get("/game", gameHandler::listGames);
        Spark.post("/game", gameHandler::createGame);
        Spark.put("/game", gameHandler::joinGame);

        Spark.delete("/db", clearHandler::clearApplication);
    }

    private void setupExceptionHandling() {
        Spark.exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.body("{ \"message\": \"Error: " + e.getMessage() + "\" }");
        });
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
