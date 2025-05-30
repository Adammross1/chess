package dataaccess;

import model.UserData;
import java.util.HashMap;
import java.util.Map;

/**
 * Memory-based implementation of UserDAO
 */
public class MemoryUserDAO implements UserDAO {
    private final Map<String, UserData> users = new HashMap<>();

    @Override
    public void clear() {
        users.clear();
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (users.containsKey(user.username())) {
            throw new DataAccessException("Error: username already taken");
        }
        users.put(user.username(), user);
    }

    @Override
    public UserData getUser(String username) {
        return users.get(username);
    }

    @Override
    public boolean verifyPassword(String username, String password) {
        UserData user = users.get(username);
        return user != null && user.password().equals(password);
    }
} 