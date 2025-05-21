package server.handlers;

import com.google.gson.Gson;
import service.ClearService;
import service.requests.ClearAppRequest;
import spark.Request;
import spark.Response;

import java.util.Map;

public class ClearHandler {
    private final ClearService clearService;
    private final Gson gson = new Gson();

    public ClearHandler(ClearService clearService) {
        this.clearService = clearService;
    }

    public Object clearApplication(Request req, Response res) {
        try {
            ClearAppRequest request = new ClearAppRequest();
            clearService.clearApplication(request);
            res.status(200);
            return gson.toJson(Map.of());
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
        }
    }
}
