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
    private static final Logger logger = Logger.getLogger(GameService.class.getName());
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    public CreateGameResult createGame(CreateGameRequest request) throws DataAccessException {
        logger.info("Attempting to create game for auth token: " + request.authToken());
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
            logger.warning("Unauthorized game creation attempt with token: " + request.authToken());
            throw new DataAccessException("Error: unauthorized");
        }
        logger.info("Auth token valid for user: " + authData.username() + " for game creation.");

        // Create a new game with a default board setup
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, null, null, request.gameName(), game);
        int gameID = gameDAO.createGame(gameData);
         logger.info("Game created with ID: " + gameID + " by user: " + authData.username());

        return new CreateGameResult(gameID);
    }

    public JoinGameResult joinGame(JoinGameRequest request) throws DataAccessException {
        logger.info("Attempting to join game " + request.gameID() + " with color " + request.playerColor() + " for auth token: " + request.authToken());
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
             logger.warning("Unauthorized game join attempt with token: " + request.authToken());
            throw new DataAccessException("Error: unauthorized");
        }
        logger.info("Auth token valid for user: " + authData.username() + " for joining game.");

        GameData game = gameDAO.getGame(request.gameID());
        if (game == null) {
             logger.warning("Game not found for join attempt with ID: " + request.gameID());
            throw new DataAccessException("Error: bad request");
        }
        logger.info("Game found with ID: " + request.gameID() + " for join attempt.");

        String username = authData.username();
        if (request.playerColor().equalsIgnoreCase("WHITE")) {
            if (game.whiteUsername() != null) {
                 logger.warning("White spot already taken for game ID: " + request.gameID());
                throw new DataAccessException("Error: already taken");
            }
            gameDAO.updateGame(request.gameID(), username, game.blackUsername());
             logger.info("User " + username + " joined as WHITE in game ID: " + request.gameID());
        } else if (request.playerColor().equalsIgnoreCase("BLACK")) {
            if (game.blackUsername() != null) {
                 logger.warning("Black spot already taken for game ID: " + request.gameID());
                throw new DataAccessException("Error: already taken");
            }
            gameDAO.updateGame(request.gameID(), game.whiteUsername(), username);
             logger.info("User " + username + " joined as BLACK in game ID: " + request.gameID());
        } else {
             logger.warning("Invalid player color for join attempt in game ID: " + request.gameID() + ", color: " + request.playerColor());
            throw new DataAccessException("Error: bad request");
        }

        return new JoinGameResult();
    }

    public ListGamesResult listGames(ListGamesRequest request) throws DataAccessException {
        logger.info("Attempting to list games for auth token: " + request.authToken());
        AuthData authData = authDAO.getAuth(request.authToken());
        logger.info("AuthData result for token " + request.authToken() + ": " + (authData != null ? "Found" : "Not Found"));
        if (authData == null) {
             logger.warning("Unauthorized list games attempt with token: " + request.authToken());
            throw new DataAccessException("Error: unauthorized");
        }
        logger.info("Auth token valid for user: " + authData.username() + " for listing games.");

        logger.info("Calling gameDAO.listGames()");
        List<GameData> games = gameDAO.listGames();
        logger.info("gameDAO.listGames() returned " + games.size() + " games.");

        return new ListGamesResult(games);
    }
}
