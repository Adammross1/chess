package server;

import com.google.gson.Gson;
import model.AuthData;
import model.UserData;
import model.RegisterResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class ServerFacade {
    private final String serverUrl;
    private final Gson gson;

    public ServerFacade(String serverUrl) {
        this.serverUrl = serverUrl;
        this.gson = new Gson();
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
            http.addRequestProperty("authorization", authToken);
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
                http.addRequestProperty("authorization", authToken);
            }

            writeBody(request, http);
            http.connect();
            
            var status = http.getResponseCode();
            if (!isSuccessful(status)) {
                throw new ResponseException(status, "failure: " + status);
            }
            
            return readBody(http, responseClass);
        } catch (Exception ex) {
            if (ex instanceof ResponseException) {
                throw (ResponseException) ex;
            }
            throw new ResponseException(500, ex.getMessage());
        }
    }

    private static void writeBody(Object request, HttpURLConnection http) throws IOException {
        if (request != null) {
            http.addRequestProperty("Content-Type", "application/json");
            String reqData = new Gson().toJson(request);
            try (OutputStream reqBody = http.getOutputStream()) {
                reqBody.write(reqData.getBytes());
            }
        }
    }

    private static <T> T readBody(HttpURLConnection http, Class<T> responseClass) throws IOException {
        T response = null;
        if (http.getContentLength() < 0) {
            try (InputStream respBody = http.getInputStream()) {
                InputStreamReader reader = new InputStreamReader(respBody);
                if (responseClass != null) {
                    response = new Gson().fromJson(reader, responseClass);
                }
            }
        }
        return response;
    }

    private boolean isSuccessful(int status) {
        return status / 100 == 2;
    }
} 