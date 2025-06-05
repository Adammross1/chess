package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.AuthData;
import model.UserData;
import model.RegisterResult;
import model.GameData;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessBoard;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

public class ServerFacade {
    private final String serverUrl;
    private final Gson gson;

    public ServerFacade(String serverUrl) {
        this.serverUrl = serverUrl;
        this.gson = new GsonBuilder()
            .registerTypeAdapter(ChessGame.class, new ChessGameAdapter())
            .registerTypeAdapter(ChessPiece.class, new ChessPieceAdapter())
            .registerTypeAdapter(ChessBoard.class, new ChessBoardAdapter())
            .create();
    }

    public AuthData register(String username, String password, String email) throws ResponseException {
        var path = "/user";
        var request = new UserData(username, password, email);
        var result = this.makeRequest("POST", path, request, RegisterResult.class);
        return new AuthData(result.authToken(), result.username());
    }

    public AuthData login(String username, String password) throws ResponseException {
        var path = "/session";
        var request = new UserData(username, password, null);
        var result = this.makeRequest("POST", path, request, RegisterResult.class);
        return new AuthData(result.authToken(), result.username());
    }

    public void logout(String authToken) throws ResponseException {
        var path = "/session";
        try {
            URL url = (new URI(serverUrl + path)).toURL();
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("DELETE");
            http.setDoOutput(false);  // DELETE requests don't need a body
            http.addRequestProperty("Authorization", "Bearer " + authToken);
            http.connect();
            
            var status = http.getResponseCode();
            // Consider both 200 and 401 as successful logout
            if (status != 200 && status != 401) {
                throw new ResponseException(status, "failure: " + status);
            }
        } catch (Exception ex) {
            if (ex instanceof ResponseException) {
                throw (ResponseException) ex;
            }
            throw new ResponseException(500, ex.getMessage());
        }
    }

    public int createGame(String authToken, String gameName) throws ResponseException {
        var path = "/game";
        // The request body is a JSON object with a 'gameName' field
        record CreateGameRequest(String gameName) {}
        var request = new CreateGameRequest(gameName);

        // The response body is a JSON object with a 'gameID' field
        record CreateGameResult(int gameID) {}
        var result = this.makeRequest("POST", path, request, CreateGameResult.class, authToken);
        return result.gameID();
    }

    public List<GameData> listGames(String authToken) throws ResponseException {
        var path = "/game";
        // The response body is a JSON object with a 'games' field containing an array of games
        record ListGamesResult(List<GameData> games) {}
        var result = this.makeRequest("GET", path, null, ListGamesResult.class, authToken);
        return result.games();
    }

    private <T> T makeRequest(String method, String path, Object request, Class<T> responseClass) throws ResponseException {
        return makeRequest(method, path, request, responseClass, null);
    }

    private <T> T makeRequest(String method, String path, Object request, Class<T> responseClass, String authToken) throws ResponseException {
        try {
            URL url = (new URI(serverUrl + path)).toURL();
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod(method);
            http.setDoOutput(true);

            if (authToken != null) {
                http.addRequestProperty("Authorization", "Bearer " + authToken);
            }

            writeBody(request, http);
            http.connect();
            
            var status = http.getResponseCode();
            if (!isSuccessful(status)) {
                String errorMessage = "failure: " + status;
                try (InputStream errorStream = http.getErrorStream()) {
                    if (errorStream != null) {
                        InputStreamReader reader = new InputStreamReader(errorStream);
                        // Assuming the error response is a JSON object with a 'message' field
                        record ErrorResponse(String message) {}
                        ErrorResponse errorResponse = new Gson().fromJson(reader, ErrorResponse.class);
                        if (errorResponse != null && errorResponse.message() != null) {
                            errorMessage += ": " + errorResponse.message();
                        }
                    }
                } catch (IOException e) {
                    // Ignore exception while reading error stream
                }
                throw new ResponseException(status, errorMessage);
            }
            
            return readBody(http, responseClass);
        } catch (Exception ex) {
            if (ex instanceof ResponseException) {
                throw (ResponseException) ex;
            }
            throw new ResponseException(500, ex.getMessage());
        }
    }

    private void writeBody(Object request, HttpURLConnection http) throws IOException {
        if (request != null) {
            http.addRequestProperty("Content-Type", "application/json");
            String reqData = gson.toJson(request);
            try (OutputStream reqBody = http.getOutputStream()) {
                reqBody.write(reqData.getBytes());
            }
        }
    }

    private <T> T readBody(HttpURLConnection http, Class<T> responseClass) throws IOException {
        T response = null;
        if (http.getContentLength() < 0) {
            try (InputStream respBody = http.getInputStream()) {
                InputStreamReader reader = new InputStreamReader(respBody);
                if (responseClass != null) {
                    response = gson.fromJson(reader, responseClass);
                }
            }
        }
        return response;
    }

    private boolean isSuccessful(int status) {
        return status / 100 == 2;
    }
} 