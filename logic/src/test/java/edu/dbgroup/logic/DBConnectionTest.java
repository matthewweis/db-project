package edu.dbgroup.logic;

import org.davidmoten.rx.jdbc.ConnectionProvider;
import org.davidmoten.rx.jdbc.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Test showing how to connect to a Microsoft SQL Server.
 * This will only work if there is an active server to connect to.
 *
 * OPTION 1: (Microsoft SQL Management Studio / Azure)
 *
 * Creating a server as he showed in lecture will work perfectly fine! Just type the ip (likely localhost a.k.a.
 * 127.0.0.1) and the port (likely 1433) alongside the database name into the variable DATABASE_URL.
 *
 *
 * OPTION 2: (Microsoft mssql server running on VM with Docker) *I prefer this method!*
 *
 * A server can be created using Docker, which requires less setup (and is far smaller in size) than Microsoft
 *
 * For an example of running a server using Docker:
 * https://docs.microsoft.com/en-us/sql/linux/quickstart-install-connect-docker?view=sql-server-2017
 *
 *
 * step 1) install Docker
 *
 *      > this will allow you to create local SQL servers in a lightweight virtual machines called "containers"
 *
 *
 * step 2) type in terminal:
 *              docker pull mcr.microsoft.com/mssql/server:2017-latest
 *              docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=Group_15' -p 1433:1433 \
 *                  --name sql1 -d mcr.microsoft.com/mssql/server:2017-latest
 *
 *
 *      > this will create a Docker container called "sql1" which hosts a SQL server on localhost, port 1433
 *      > note that the second command (docker run...) should be 1 line, but is broken into two lines with the backslash
 *
 *
 * step 3) type in terminal:
 *              docker exec -it sql1 "bash"
 *
 *      > this will give a shell in the container
 *
 *
 * step 4) type in terminal:
 *              /opt/mssql-tools/bin/sqlcmd -S localhost -U SA -P 'Group_15'
 *              CREATE DATABASE TestDB
 *              GO
 *
 *      > this will connect to the database internally and create a table called TestDB
 *
 *
 * step 5) connect to database from the outside by running the main method in this class, {@link DBConnectionTest}
 *
 *      > at this point, the shell to the docker container (from step 3) is no longer needed.
 *        > the sqlcmd program (from step 4) can be exited using ctrl-c
 *        > the container's shell (from step 3) can be exited by typing "exit" (without quotes)
 *
 *      > once the container is exited, the container can...
 *        > be stopped by typing "docker stop sql1" (without quotes)
 *        > be started again by typing "docker start sql1" (without quotes)
 *        > have its status viewed by typing "docker inspect sql1" (without quotes)
 *        > be deleted by typing "docker rm sql1" (without quotes)
 *          > if deleted, go back to the "docker run..." command in step 2 to get the container back
 *
 */
public class DBConnectionTest {

    private final static Logger logger = LoggerFactory.getLogger(DBConnectionTest.class);

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
        logger.info("attempting to load microsoft jdbc driver...");
        loadMicrosoftJdbcDriver();

        logger.info("attempting to connect to database (a database must be running for this to work)");
        DriverManager.setLoginTimeout(10); // after 10 seconds, assume connection isn't happening
        final Database database = connectToDatabase();

        logger.info("success!");
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
