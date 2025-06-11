package websocket.messages;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;

public class ServerMessage {
    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    private final ServerMessageType serverMessageType;
    private final String message;
    private final ChessGame game;

    public ServerMessage(ServerMessageType serverMessageType, String message, ChessGame game) {
        this.serverMessageType = serverMessageType;
        this.message = message;
        this.game = game;
    }

    public ServerMessageType getServerMessageType() {
        return serverMessageType;
    }

    public String getMessage() {
        return message;
    }

    public ChessGame getGame() {
        return game;
    }

    // Custom deserializer for ServerMessage
    public static class ServerMessageDeserializer implements JsonDeserializer<ServerMessage> {
        private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ChessGame.class, new server.ChessGameAdapter())
            .create();

        @Override
        public ServerMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            var jsonObject = json.getAsJsonObject();
            var messageType = ServerMessageType.valueOf(jsonObject.get("serverMessageType").getAsString());
            
            String message = null;
            if (jsonObject.has("message")) {
                message = jsonObject.get("message").getAsString();
            }
            
            ChessGame game = null;
            if (jsonObject.has("game")) {
                game = gson.fromJson(jsonObject.get("game"), ChessGame.class);
            }
            
            return new ServerMessage(messageType, message, game);
        }
    }
} 