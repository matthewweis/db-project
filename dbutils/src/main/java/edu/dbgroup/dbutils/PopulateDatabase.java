package edu.dbgroup.dbutils;

import org.davidmoten.rx.jdbc.ConnectionProvider;
import org.davidmoten.rx.jdbc.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A utility class which will automatically populate the weather app database with data.
 *
 * Note: this class only populates an existing database with data, the database itself is created elsewhere and must
 * be running for this class to have any effect.
 *
 */
public class PopulateDatabase {

    private final static Logger logger = LoggerFactory.getLogger(PopulateDatabase.class);

    /*
     * Info for connecting to database. This may differ between databases.
     */
    private static final String DATABASE_URL = "jdbc:sqlserver://localhost:1433;DatabaseName=TestDB";
    private static final String DATABASE_USERNAME = "SA";
    private static final String DATABASE_PASSWORD = "Group_15";

    /*
     * String to load driver for Microsoft sqlserver. This is only here as a contingency for legacy servers.
     */
    private static final String JDBC_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        logger.info("loading microsoft jdbc driver...");
        loadMicrosoftJdbcDriver();

        logger.info("connecting to database...");
        DriverManager.setLoginTimeout(10); // after 10 seconds, assume connection isn't happening
        final Database database = connectToDatabase();


    }

    /*
     * Note: this method is only relevant for older servers, but is here just in case.
     */
    private static void loadMicrosoftJdbcDriver() throws ClassNotFoundException {
        Class.forName(JDBC_DRIVER);
    }

    private static Database connectToDatabase() throws SQLException {
        final Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
        return Database.fromBlocking(ConnectionProvider.from(connection));
    }

}
