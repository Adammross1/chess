package dataaccess;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

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
        logger.info("Attempting to create database: " + databaseName);
        var statement = "CREATE DATABASE IF NOT EXISTS " + databaseName;
        try (var conn = DriverManager.getConnection(connectionUrl, dbUsername, dbPassword);
             var preparedStatement = conn.prepareStatement(statement)) {
            logger.info("Executing CREATE DATABASE statement");
            preparedStatement.executeUpdate();
            System.out.println("Database " + databaseName + " created or already exists");
            logger.info("CREATE DATABASE statement executed successfully");
        } catch (SQLException ex) {
            logger.severe("SQL Error creating database: " + ex.getMessage());
            throw new DataAccessException("failed to create database", ex);
        }
         logger.info("Finished createDatabase");
    }

    /**
     * Initializes the database and all required tables.
     * This method is idempotent and can be called multiple times safely.
     */
    static public void initializeDatabase() throws DataAccessException {
        logger.info("Initializing database and tables");
        // First create the database if it doesn't exist
        createDatabase();

        // Then create all tables
        try (var conn = getConnection()) {
            logger.info("Connected to database for table creation");
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
                logger.info("Executing CREATE TABLE statements");
                statement.execute(createUserTable);
                System.out.println("User table created or already exists");
                statement.execute(createAuthTable);
                System.out.println("Auth table created or already exists");
                statement.execute(createGameTable);
                System.out.println("Game table created or already exists");
                 logger.info("CREATE TABLE statements executed successfully");
            }
        } catch (SQLException ex) {
             logger.severe("SQL Error creating tables: " + ex.getMessage());
            throw new DataAccessException("failed to create tables", ex);
        }
         logger.info("Database and tables initialized");
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
        logger.info("Attempting to get database connection");
        try {
            //do not wrap the following line with a try-with-resources
            var conn = DriverManager.getConnection(connectionUrl, dbUsername, dbPassword);
            conn.setCatalog(databaseName);
             logger.info("Database connection obtained successfully");
            return conn;
        } catch (SQLException ex) {
             logger.severe("SQL Error getting database connection: " + ex.getMessage());
            throw new DataAccessException("failed to get connection", ex);
        }
    }

    private static void loadPropertiesFromResources() {
        logger.info("Loading database properties from resources");
        try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
            if (propStream == null) {
                 logger.severe("Unable to load db.properties");
                throw new Exception("Unable to load db.properties");
            }
            Properties props = new Properties();
            props.load(propStream);
            loadProperties(props);
            System.out.println("Database properties loaded successfully");
             logger.info("Database properties loaded successfully");
        } catch (Exception ex) {
             logger.severe("Error loading db.properties: " + ex.getMessage());
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
        System.out.println("Database connection URL: " + connectionUrl);
        logger.info("Database properties loaded: " + connectionUrl);
    }
}
