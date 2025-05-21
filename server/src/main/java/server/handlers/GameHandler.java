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

import java.util.Map;

public class GameHandler {
    private final GameService gameService;
    private final Gson gson = new Gson();

    public GameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public Object createGame(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            var body = gson.fromJson(req.body(), Map.class);
            String gameName = (String) body.get("gameName");

            CreateGameRequest request = new CreateGameRequest(authToken, gameName);
            CreateGameResult result = gameService.createGame(request);
            res.status(200);
            return gson.toJson(Map.of("gameID", result.gameID()));
        } catch (DataAccessException e) {
            if (e.getMessage().contains("unauthorized")) {
                res.status(401);
            } else if (e.getMessage().contains("bad request")) {
                res.status(400);
            } else {
                res.status(500);
            }
            return gson.toJson(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            res.status(400);
            return gson.toJson(Map.of("message", "Error: bad request"));
        }
    }

    public Object joinGame(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            var body = gson.fromJson(req.body(), Map.class);
            String playerColor = (String) body.get("playerColor");
            int gameID = ((Double) body.get("gameID")).intValue();

            JoinGameRequest request = new JoinGameRequest(authToken, gameID, playerColor);
            gameService.joinGame(request);
            res.status(200);
            return gson.toJson(Map.of());
        } catch (DataAccessException e) {
            if (e.getMessage().contains("unauthorized")) {
                res.status(401);
            } else if (e.getMessage().contains("already taken")) {
                res.status(403);
            } else if (e.getMessage().contains("bad request")) {
                res.status(400);
            } else {
                res.status(500);
            }
            return gson.toJson(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            res.status(400);
            return gson.toJson(Map.of("message", "Error: bad request"));
        }
    }

    public Object listGames(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            ListGamesRequest request = new ListGamesRequest(authToken);
            ListGamesResult result = gameService.listGames(request);
            res.status(200);
            return gson.toJson(Map.of("games", result.games()));
        } catch (DataAccessException e) {
            if (e.getMessage().contains("unauthorized")) {
                res.status(401);
            } else {
                res.status(500);
            }
            return gson.toJson(Map.of("message", e.getMessage()));
        }
    }
}
