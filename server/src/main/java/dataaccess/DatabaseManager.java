package dataaccess;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private static String databaseName;
    private static String dbUsername;
    private static String dbPassword;
    private static String connectionUrl;

    /*
     * Load the database information for the db.properties file.
     */
    static {
        loadPropertiesFromResources();
    }

    /**
     * Creates the database if it does not already exist.
     */
    static public void createDatabase() throws DataAccessException {
        LOGGER.info("Attempting to create database: " + databaseName);
        var statement = "CREATE DATABASE IF NOT EXISTS " + databaseName;
        try (var conn = DriverManager.getConnection(connectionUrl.replace("/" + databaseName, ""), dbUsername, dbPassword);
             var preparedStatement = conn.prepareStatement(statement)) {
            LOGGER.info("Executing CREATE DATABASE statement");
            preparedStatement.executeUpdate();
            LOGGER.info("CREATE DATABASE statement executed successfully");
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error creating database: " + ex.getMessage());
            throw new DataAccessException("failed to create database", ex);
        }
        LOGGER.info("Finished createDatabase");
    }

    /**
     * Initializes the database and all required tables.
     * This method is idempotent and can be called multiple times safely.
     */
    static public void initializeDatabase() throws DataAccessException {
        LOGGER.info("Initializing database and tables");
        // First create the database if it doesn't exist
        createDatabase();

        // Then create all tables
        try (var conn = getConnection()) {
            LOGGER.info("Connected to database for table creation");
            // Create user table
            var createUserTable = """
                CREATE TABLE IF NOT EXISTS user (
                    username VARCHAR(255) PRIMARY KEY,
                    password_hash VARCHAR(255) NOT NULL
                )
                """;
            
            // Create auth table
            var createAuthTable = """
                CREATE TABLE IF NOT EXISTS auth (
                    auth_token VARCHAR(255) PRIMARY KEY,
                    username VARCHAR(255) NOT NULL,
                    FOREIGN KEY (username) REFERENCES user(username) ON DELETE CASCADE
                )
                """;
            
            // Create game table
            var createGameTable = """
                CREATE TABLE IF NOT EXISTS game (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    white_username VARCHAR(255),
                    black_username VARCHAR(255),
                    game_name VARCHAR(255) NOT NULL,
                    game_state JSON NOT NULL,
                    FOREIGN KEY (white_username) REFERENCES user(username) ON DELETE SET NULL,
                    FOREIGN KEY (black_username) REFERENCES user(username) ON DELETE SET NULL
                )
                """;

            try (var statement = conn.createStatement()) {
                LOGGER.info("Executing CREATE TABLE statements");
                statement.execute(createUserTable);
                statement.execute(createAuthTable);
                statement.execute(createGameTable);
                LOGGER.info("CREATE TABLE statements executed successfully");
            }
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error creating tables: " + ex.getMessage());
            throw new DataAccessException("failed to create tables", ex);
        }
        LOGGER.info("Database and tables initialized");
    }

    /**
     * Create a connection to the database and sets the catalog based upon the
     * properties specified in db.properties. Connections to the database should
     * be short-lived, and you must close the connection when you are done with it.
     * The easiest way to do that is with a try-with-resource block.
     * <br/>
     * <code>
     * try (var conn = DatabaseManager.getConnection()) {
     * // execute SQL statements.
     * }
     * </code>
     */
    static Connection getConnection() throws DataAccessException {
        LOGGER.info("Attempting to get database connection");
        try {
            var conn = DriverManager.getConnection(connectionUrl, dbUsername, dbPassword);
            conn.setCatalog(databaseName);
            LOGGER.info("Database connection obtained successfully");
            return conn;
        } catch (SQLException ex) {
            LOGGER.severe("SQL Error getting database connection: " + ex.getMessage());
            throw new DataAccessException("failed to get connection", ex);
        }
    }

    private static void loadPropertiesFromResources() {
        LOGGER.info("Loading database properties from resources");
        try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
            if (propStream == null) {
                 LOGGER.severe("Unable to load db.properties");
                throw new Exception("Unable to load db.properties");
            }
            Properties props = new Properties();
            props.load(propStream);
            loadProperties(props);
             LOGGER.info("Database properties loaded successfully");
        } catch (Exception ex) {
             LOGGER.severe("Error loading db.properties: " + ex.getMessage());
            throw new RuntimeException("unable to process db.properties", ex);
        }
    }

    private static void loadProperties(Properties props) {
        databaseName = props.getProperty("db.name");
        dbUsername = props.getProperty("db.user");
        dbPassword = props.getProperty("db.password");

        var host = props.getProperty("db.host");
        var port = Integer.parseInt(props.getProperty("db.port"));
        connectionUrl = String.format("jdbc:mysql://%s:%d/%s", host, port, databaseName);
        LOGGER.info("Database properties loaded: " + connectionUrl);
    }
}
