package server.handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.GameService;
import service.requests.CreateGameRequest;
import service.requests.JoinGameRequest;
import service.requests.ListGamesRequest;
import service.results.CreateGameResult;
import service.results.ListGamesResult;
import model.GameData;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class GameHandler {
    private static final Logger LOGGER = Logger.getLogger(GameHandler.class.getName());
    private final GameService gameService;
    private final Gson gson;

    public GameHandler(GameService gameService, Gson gson) {
        this.gameService = gameService;
        this.gson = gson;
    }

    private Object handleDataAccessException(DataAccessException e, Response res) {
        LOGGER.severe("DataAccessException in GameHandler: " + e.getMessage());
        if (e.getMessage().contains("unauthorized")) {
            res.status(401);
        } else if (e.getMessage().contains("already taken")) {
            res.status(403);
            // Custom error message for already taken
            String msg = e.getMessage().toLowerCase().contains("white") ?
                "White position is already taken, join black" :
                e.getMessage().toLowerCase().contains("black") ?
                "Black position is already taken, join white" :
                "That position is already taken, try another color";
            return gson.toJson(Map.of("message", msg));
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
            LOGGER.info("Auth token from headers: [" + authToken + "]");
            LOGGER.info("All headers: " + req.headers());
            
            // Remove "Bearer " prefix if present
            if (authToken != null && authToken.startsWith("Bearer ")) {
                authToken = authToken.substring(7);
            }
            
            var body = gson.fromJson(req.body(), Map.class);
            String gameName = (String) body.get("gameName");
            LOGGER.info("Game name from request: [" + gameName + "]");

            if (gameName == null) {
                LOGGER.warning("Bad request: gameName is null");
                res.status(400);
                return gson.toJson(Map.of("message", "Error: bad request"));
            }

            CreateGameRequest request = new CreateGameRequest(authToken, gameName);
            LOGGER.info("Calling gameService.createGame with auth token: [" + authToken + "]");
            CreateGameResult result = gameService.createGame(request);
            LOGGER.info("gameService.createGame returned gameID: " + result.gameID());
            res.status(200);
            return gson.toJson(Map.of("gameID", result.gameID()));
        } catch (DataAccessException e) {
            LOGGER.severe("DataAccessException in createGame: " + e.getMessage());
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
            String authTokenHeader = req.headers("authorization");
            String authToken = authTokenHeader;
            if (authTokenHeader != null && authTokenHeader.startsWith("Bearer ")) {
                authToken = authTokenHeader.substring(7);
            }
            LOGGER.fine("Auth token from headers: " + authToken);
            var body = gson.fromJson(req.body(), Map.class);
            LOGGER.info("joinGame request body: " + req.body());
            String playerColor = (String) body.get("playerColor");
            LOGGER.info("Parsed playerColor: " + playerColor);
            // Handle potential NumberFormatException or ClassCastException
            int gameID;
            try {
                 gameID = ((Double) body.get("gameID")).intValue();
                 LOGGER.info("Parsed gameID: " + gameID);
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
            LOGGER.info("joinGame response status: 200");
            return gson.toJson(Map.of());
        } catch (DataAccessException e) {
            LOGGER.severe("DataAccessException in joinGame: " + e.getMessage());
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
            
            // Remove "Bearer " prefix if present
            if (authToken != null && authToken.startsWith("Bearer ")) {
                authToken = authToken.substring(7);
            }
            
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

    public Object getGame(Request req, Response res) {
        LOGGER.info("Handling getGame request");
        LOGGER.info("Request path: " + req.pathInfo());
        LOGGER.info("Request method: " + req.requestMethod());
        LOGGER.info("Request headers: " + req.headers());
        LOGGER.info("Request params: " + req.params());
        try {
            String authToken = req.headers("authorization");
            LOGGER.info("Authorization header: " + authToken);
            if (authToken != null && authToken.startsWith("Bearer ")) {
                authToken = authToken.substring(7);
            }
            int gameId;
            try {
                String gameIdParam = req.params(":id");
                LOGGER.info("Extracted gameId param: " + gameIdParam);
                gameId = Integer.parseInt(gameIdParam);
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid game ID format");
                res.status(400);
                return gson.toJson(Map.of("message", "Invalid game ID"));
            }
            GameData game = gameService.getGame(authToken, gameId);
            LOGGER.info("GameData fetched successfully for gameId: " + gameId);
            String json = gson.toJson(game);
            LOGGER.info("Serialized GameData JSON: " + json);
            res.status(200);
            return json;
        } catch (DataAccessException e) {
            LOGGER.severe("DataAccessException in getGame: " + e.getMessage());
            return handleDataAccessException(e, res);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error in getGame: " + e.getMessage());
            res.status(500);
            return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
        }
    }
}
