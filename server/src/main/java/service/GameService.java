package service;

import chess.ChessGame;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import model.AuthData;
import model.GameData;
import service.requests.CreateGameRequest;
import service.requests.JoinGameRequest;
import service.requests.ListGamesRequest;
import service.results.CreateGameResult;
import service.results.JoinGameResult;
import service.results.ListGamesResult;

import java.util.List;
import java.util.logging.Logger;

public class GameService {
    private static final Logger LOGGER = Logger.getLogger(GameService.class.getName());
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    public CreateGameResult createGame(CreateGameRequest request) throws DataAccessException {
        LOGGER.info("Attempting to create game for auth token: [" + request.authToken() + "]");
        AuthData authData = authDAO.getAuth(request.authToken());
        LOGGER.info("Auth data result: " + (authData != null ? "Found for user " + authData.username() : "Not found"));
        
        if (authData == null) {
            LOGGER.warning("Unauthorized game creation attempt with token: [" + request.authToken() + "]");
            throw new DataAccessException("Error: unauthorized");
        }
        LOGGER.info("Auth token valid for user: " + authData.username() + " for game creation.");

        // Create a new game with a default board setup
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, null, null, request.gameName(), game);
        int gameID = gameDAO.createGame(gameData);
        LOGGER.info("Game created with ID: " + gameID + " by user: " + authData.username());

        return new CreateGameResult(gameID);
    }

    public JoinGameResult joinGame(JoinGameRequest request) throws DataAccessException {
        LOGGER.info("Attempting to join game " + request.gameID() + " with color " + request.playerColor() + " for auth token: " + request.authToken());
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
             LOGGER.warning("Unauthorized game join attempt with token: " + request.authToken());
            throw new DataAccessException("Error: unauthorized");
        }
        LOGGER.info("Auth token valid for user: " + authData.username() + " for joining game.");

        GameData game = gameDAO.getGame(request.gameID());
        if (game == null) {
             LOGGER.warning("Game not found for join attempt with ID: " + request.gameID());
            throw new DataAccessException("Error: bad request");
        }
        LOGGER.info("Game found with ID: " + request.gameID() + " for join attempt.");

        String username = authData.username();
        
        // Validate team color - return 400 for null, empty, or invalid colors
        if (request.playerColor() == null || request.playerColor().trim().isEmpty()) {
            LOGGER.warning("Invalid team color: null or empty");
            throw new DataAccessException("Error: bad request");
        }

        String color = request.playerColor().toUpperCase();
        if (!color.equals("WHITE") && !color.equals("BLACK")) {
            LOGGER.warning("Invalid team color: " + color);
            throw new DataAccessException("Error: bad request");
        }

        if (color.equals("WHITE")) {
            if (game.whiteUsername() != null) {
                 LOGGER.warning("White spot already taken for game ID: " + request.gameID());
                throw new DataAccessException("Error: already taken");
            }
            gameDAO.updateGame(request.gameID(), username, game.blackUsername());
             LOGGER.info("User " + username + " joined as WHITE in game ID: " + request.gameID());
        } else {
            if (game.blackUsername() != null) {
                 LOGGER.warning("Black spot already taken for game ID: " + request.gameID());
                throw new DataAccessException("Error: already taken");
            }
            gameDAO.updateGame(request.gameID(), game.whiteUsername(), username);
             LOGGER.info("User " + username + " joined as BLACK in game ID: " + request.gameID());
        }

        return new JoinGameResult();
    }

    public ListGamesResult listGames(ListGamesRequest request) throws DataAccessException {
        LOGGER.info("Attempting to list games for auth token: " + request.authToken());
        AuthData authData = authDAO.getAuth(request.authToken());
        LOGGER.info("AuthData result for token " + request.authToken() + ": " + (authData != null ? "Found" : "Not Found"));
        if (authData == null) {
             LOGGER.warning("Unauthorized list games attempt with token: " + request.authToken());
            throw new DataAccessException("Error: unauthorized");
        }
        LOGGER.info("Auth token valid for user: " + authData.username() + " for listing games.");

        LOGGER.info("Calling gameDAO.listGames()");
        List<GameData> games = gameDAO.listGames();
        LOGGER.info("gameDAO.listGames() returned " + games.size() + " games.");

        return new ListGamesResult(games);
    }

    public GameData getGame(String authToken, int gameID) throws DataAccessException {
        LOGGER.info("GameService.getGame called with authToken: " + authToken + ", gameID: " + gameID);
        AuthData authData = authDAO.getAuth(authToken);
        if (authData == null) {
            LOGGER.warning("AuthData not found for token: " + authToken);
            throw new DataAccessException("Error: unauthorized");
        }
        GameData game = gameDAO.getGame(gameID);
        if (game == null) {
            LOGGER.warning("GameData not found for gameID: " + gameID);
            throw new DataAccessException("Error: game not found");
        }
        LOGGER.info("GameData found for gameID: " + gameID + ", game: " + game);
        return game;
    }

    /**
     * Updates the game state in the database.
     * @param gameID The ID of the game to update
     * @param game The new game state
     * @throws DataAccessException if the update fails
     */
    public void updateGame(int gameID, ChessGame game) throws DataAccessException {
        System.out.println("TEAM_TURN: GameService updateGame - teamTurn: " + game.getTeamTurn());
        gameDAO.updateGameState(gameID, game);
        System.out.println("TEAM_TURN: GameService after update - teamTurn: " + game.getTeamTurn());
    }
}
