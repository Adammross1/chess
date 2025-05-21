package dataaccess;

import model.AuthData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Memory-based implementation of AuthDAO
 */
public class MemoryAuthDAO implements AuthDAO {
    private final Map<String, AuthData> authTokens = new HashMap<>();

    @Override
    public void clear() {
        authTokens.clear();
    }

    @Override
    public AuthData createAuth(AuthData authData) {
        String authToken = authData.authToken();
        if (authToken == null || authToken.isEmpty()) {
            authToken = UUID.randomUUID().toString();
        }

        AuthData newAuthData = new AuthData(authToken, authData.username());
        authTokens.put(authToken, newAuthData);
        return newAuthData;
    }

    @Override
    public AuthData getAuth(String authToken) {
        return authTokens.get(authToken);
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        if (!authTokens.containsKey(authToken)) {
            throw new DataAccessException("Error: unauthorized");
        }
        authTokens.remove(authToken);
    }
}
