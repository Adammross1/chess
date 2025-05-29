package server.handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.GameService;
import service.requests.CreateGameRequest;
import service.requests.JoinGameRequest;
import service.requests.ListGamesRequest;
import service.results.CreateGameResult;
import service.results.ListGamesResult;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class GameHandler {
    private static final Logger LOGGER = Logger.getLogger(GameHandler.class.getName());
    private final GameService gameService;
    private final Gson gson = new Gson();

    public GameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    private Object handleDataAccessException(DataAccessException e, Response res) {
        LOGGER.severe("DataAccessException in GameHandler: " + e.getMessage());
        if (e.getMessage().contains("unauthorized")) {
            res.status(401);
        } else if (e.getMessage().contains("already taken")) {
            res.status(403);
        } else if (e.getMessage().contains("bad request")) {
            res.status(400);
        } else {
            res.status(500);
        }
        return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
    }

    public Object createGame(Request req, Response res) {
        LOGGER.info("Handling createGame request");
        try {
            String authToken = req.headers("authorization");
             LOGGER.fine("Auth token from headers: " + authToken);
            var body = gson.fromJson(req.body(), Map.class);
            String gameName = (String) body.get("gameName");

            if (gameName == null) {
                LOGGER.warning("Bad request: gameName is null");
                res.status(400);
                return gson.toJson(Map.of("message", "Error: bad request"));
            }

            CreateGameRequest request = new CreateGameRequest(authToken, gameName);
            LOGGER.info("Calling gameService.createGame");
            CreateGameResult result = gameService.createGame(request);
             LOGGER.info("gameService.createGame returned gameID: " + result.gameID());
            res.status(200);
            return gson.toJson(Map.of("gameID", result.gameID()));
        } catch (DataAccessException e) {
            return handleDataAccessException(e, res);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error in createGame: " + e.getMessage());
            res.status(400);
            return gson.toJson(Map.of("message", "Error: bad request"));
        }
    }

    public Object joinGame(Request req, Response res) {
        LOGGER.info("Handling joinGame request");
        try {
            String authToken = req.headers("authorization");
             LOGGER.fine("Auth token from headers: " + authToken);
            var body = gson.fromJson(req.body(), Map.class);
            String playerColor = (String) body.get("playerColor");
            // Handle potential NumberFormatException or ClassCastException
            int gameID;
            try {
                 gameID = ((Double) body.get("gameID")).intValue();
            } catch (Exception e) {
                 LOGGER.warning("Bad request: invalid gameID format");
                 res.status(400);
                 return gson.toJson(Map.of("message", "Error: bad request"));
            }

            JoinGameRequest request = new JoinGameRequest(authToken, gameID, playerColor);
            LOGGER.info("Calling gameService.joinGame");
            gameService.joinGame(request);
            LOGGER.info("gameService.joinGame successful");
            res.status(200);
            return gson.toJson(Map.of());
        } catch (DataAccessException e) {
            return handleDataAccessException(e, res);
        } catch (Exception e) {
             LOGGER.severe("Unexpected error in joinGame: " + e.getMessage());
            res.status(400);
            return gson.toJson(Map.of("message", "Error: bad request"));
        }
    }

    public Object listGames(Request req, Response res) {
        LOGGER.info("Handling listGames request");
        try {
            String authToken = req.headers("authorization");
             LOGGER.fine("Auth token from headers: " + authToken);
            ListGamesRequest request = new ListGamesRequest(authToken);
            LOGGER.info("Calling gameService.listGames");
            ListGamesResult result = gameService.listGames(request);
             LOGGER.info("gameService.listGames returned result: " + result);

            res.status(200);
             LOGGER.info("Attempting to serialize games list");
             Object responseBody = Map.of("games", result.games());
             LOGGER.fine("Response body object: " + responseBody);
            return gson.toJson(responseBody);
        } catch (DataAccessException e) {
            return handleDataAccessException(e, res);
        } catch (Exception e) {
             LOGGER.severe("Unexpected error in listGames: " + e.getMessage());
            res.status(500);
            return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
        }
    }
}
