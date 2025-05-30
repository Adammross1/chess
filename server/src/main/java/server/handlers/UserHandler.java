package server.handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.UserService;
import service.requests.LoginRequest;
import service.requests.LogoutRequest;
import service.requests.RegisterRequest;
import service.results.LoginResult;
import service.results.RegisterResult;
import spark.Request;
import spark.Response;

import java.util.Map;

public class UserHandler {
    private final UserService userService;
    private final Gson gson = new Gson();

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    private Object handleDataAccessException(DataAccessException e, Response res) {
        if (e.getMessage().contains("unauthorized")) {
            res.status(401);
        } else if (e.getMessage().contains("already taken")) {
            res.status(403);
        } else {
            res.status(500);
        }
        return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
    }

    public Object register(Request req, Response res) {
        try {
            RegisterRequest request = gson.fromJson(req.body(), RegisterRequest.class);
            
            if (request.username() == null || request.password() == null || request.email() == null) {
                res.status(400);
                return gson.toJson(Map.of("message", "Error: bad request"));
            }
            
            RegisterResult result = userService.register(request);
            res.status(200);
            return gson.toJson(result);
        } catch (DataAccessException e) {
            return handleDataAccessException(e, res);
        } catch (Exception e) {
            res.status(400);
            return gson.toJson(Map.of("message", "Error: bad request"));
        }
    }

    public Object login(Request req, Response res) {
        try {
            LoginRequest request = gson.fromJson(req.body(), LoginRequest.class);
            
            if (request.username() == null || request.password() == null) {
                res.status(400);
                return gson.toJson(Map.of("message", "Error: bad request"));
            }
            
            LoginResult result = userService.login(request);
            res.status(200);
            return gson.toJson(result);
        } catch (DataAccessException e) {
            return handleDataAccessException(e, res);
        } catch (Exception e) {
            res.status(400);
            return gson.toJson(Map.of("message", "Error: bad request"));
        }
    }

    public Object logout(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");
            LogoutRequest request = new LogoutRequest(authToken);
            userService.logout(request);
            res.status(200);
            return gson.toJson(Map.of());
        } catch (DataAccessException e) {
            return handleDataAccessException(e, res);
        }
    }
}
