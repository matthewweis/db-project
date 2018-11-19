package edu.dbgroup.dbutils;

import com.opencsv.CSVReader;
import io.reactivex.Flowable;
import org.davidmoten.rx.jdbc.ConnectionProvider;
import org.davidmoten.rx.jdbc.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

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

    /*
     * Strings for creating each table in the database.
     */
    private static final String CREATE_TABLE_USER_DATA =
            "CREATE TABLE [UserData] (\n" +
                    "  [UserData_ID] INT,\n" +
                    "  [User_ID] INT,\n" +
                    "  [Log_ID] INT,\n" +
                    "  [Temperature_ID] INT,\n" +
                    "  [Precipitation_ID] INT,\n" +
                    "  [CreatedOn] DATETIME,\n" +
                    "  [UpdatedOn] DATETIME,\n" +
                    "  PRIMARY KEY ([UserData_ID])\n" +
                    ");\n" +
                    "\n" +
                    "CREATE INDEX [FK] ON  [UserData] ([User_ID], [Log_ID], [Temperature_ID], [Precipitation_ID]);";

    private static final String CREATE_TABLE_WEATHER_TYPE =
            "CREATE TABLE [WeatherType] (\n" +
                    "  [WeatherType_ID] INT,\n" +
                    "  [Value] VARCHAR(4),\n" +
                    "  PRIMARY KEY ([WeatherType_ID])\n" +
                    ");\n" +
                    "\n" +
                    "CREATE INDEX [UK] ON  [WeatherType] ([Value]);";


    private static final String CREATE_TABLE_USER_WEATHER_TYPE =
            "CREATE TABLE [UserWeatherType] (\n" +
                    "  [UserData_ID] INT,\n" +
                    "  [WeatherType_ID] INT\n" +
                    ");\n" +
                    "\n" +
                    "CREATE INDEX [UK] ON  [UserWeatherType] ([UserData_ID], [WeatherType_ID]);";

    private static final String CREATE_TABLE_GOVERNMENT_WEATHER_TYPE =
            "CREATE TABLE [GovernmentWeatherType] (\n" +
                    "  [GovernmentData_ID] INT,\n" +
                    "  [WeatherType_ID] INT\n" +
                    ");\n" +
                    "\n" +
                    "CREATE INDEX [UK] ON  [GovernmentWeatherType] ([GovernmentData_ID], [WeatherType_ID]);";

    private static final String CREATE_TABLE_LOG =
            "CREATE TABLE [Log] (\n" +
                    "  [Log_ID] INT NOT NULL IDENTITY(1, 1) PRIMARY KEY,\n" +
                    "  [County_ID] INT NOT NULL FOREIGN KEY REFERENCES County(County_ID),\n" +
                    "  [Date] DATE,\n" +
//                    "  PRIMARY KEY ([Log_ID])\n" +
                    ");";
//                    ")\n" +
//                    "\n" +
//                    "CREATE INDEX [UK] ON  [Log] ([Date]);";

    private static final String CREATE_TABLE_ZIP =
            "CREATE TABLE [Zip] (\n" +
                    "  [Zip_Code ] INT,\n" +
                    "  [County_ID] INT,\n" +
                    "  PRIMARY KEY ([Zip_Code ])\n" +
                    ");\n" +
                    "\n" +
                    "CREATE INDEX [FK] ON  [Zip] ([County_ID]);";

    private static final String CREATE_TABLE_PRECIPITATION =
            "CREATE TABLE [Precipitation] (\n" +
                    "  [Precipitation_ID] INT,\n" +
                    "  [Water] DECIMAL,\n" +
                    "  [Snow] DECIMAL,\n" +
                    "  PRIMARY KEY ([Precipitation_ID])\n" +
                    ");\n" +
                    "\n" +
                    "CREATE INDEX [NULLABLE] ON  [Precipitation] ([Water], [Snow]);";

    private static final String CREATE_TABLE_WARNING =
            "CREATE TABLE [Warning] (\n" +
                    "  [Warning_ID] INT,\n" +
                    "  [UserData_ID] INT,\n" +
                    "  [Severity] INT,\n" +
                    "  [Category] INT,\n" +
                    "  [Description] VARCHAR(256),\n" +
                    "  [CreatedOn] DATETIME,\n" +
                    "  [UpdatedOn] DATETIME,\n" +
                    "  PRIMARY KEY ([Warning_ID])\n" +
                    ");\n" +
                    "\n" +
                    "CREATE INDEX [FK] ON  [Warning] ([UserData_ID]);";

    // depends on Log
    private static final String CREATE_TABLE_COUNTY =
            "CREATE TABLE [County] (\n" +
//                    "  [County_ID] INT NOT NULL IDENTITY(1, 1) PRIMARY KEY,\n" +
                    "  [County_ID] INT NOT NULL PRIMARY KEY,\n" + // equals GEOCODE
//                    "  [Log_ID] INT NOT NULL FOREIGN KEY REFERENCES Log(Log_ID),\n" +
//            "  [PhysioRegion_ID] INT,\n" +
                    "  [Name] VARCHAR(12),\n" +
                    ");";

    private static final String CREATE_TABLE_GOVERNMENT_DATA =
            "CREATE TABLE [GovernmentData] (\n" +
                    "  [GovernmentData_ID] INT,\n" +
                    "  [Log_ID] INT,\n" +
                    "  [Temperature_ID] INT,\n" +
                    "  [Precipitation_ID] INT,\n" +
//            "  [WeatherType_ID] INT,\n" +
                    "  [CreatedOn] DATETIME,\n" +
                    "  [UpdatedOn] DATETIME,\n" +
                    "  PRIMARY KEY ([GovernmentData_ID])\n" +
                    ");\n" +
                    "\n" +
                    "CREATE INDEX [FK] ON  [GovernmentData] ([Log_ID], [Temperature_ID], [Precipitation_ID], [WeatherType_ID]);";

    private static final String CREATE_TABLE_TEMPERATURE =
            "CREATE TABLE [Temperature] (\n" +
                    "  [Temperature_ID] INT,\n" +
                    "  [Average] DECIMAL,\n" +
                    "  [High] DECIMAL,\n" +
                    "  [Low] DECIMAL,\n" +
                    "  PRIMARY KEY ([Temperature_ID])\n" +
                    ");\n" +
                    "\n" +
                    "CREATE INDEX [NULLABLE] ON  [Temperature] ([Average], [High], [Low]);";

    private static final String CREATE_TABLE_COUNTY_PHYSIO_REGION =
            "CREATE TABLE [CountyPhysioRegion] (\n" +
                    "  [County_ID] INT,\n" +
                    "  [PhysioRegion_ID] INT\n" +
                    ");\n" +
                    "\n" +
                    "CREATE INDEX [UK] ON  [CountyPhysioRegion] ([County_ID], [PhysioRegion_ID]);";

    private static final String CREATE_TABLE_PHYSIO_REGION =
            "CREATE TABLE [PhysioRegion] (\n" +
                    "  [PhysioRegion_ID] INT,\n" +
                    "  [Name] VARCHAR(12),\n" +
                    "  PRIMARY KEY ([PhysioRegion_ID])\n" +
                    ");";

    private static final String CREATE_TABLE_USER =
            "CREATE TABLE [User] (\n" +
                    "  [User_ID] INT,\n" +
                    "  [FirstName] VARCHAR(30),\n" +
                    "  [LastName] VARCHAR(30),\n" +
                    "  [Username] VARCHAR(20),\n" +
                    "  [CreatedOn] DATETIME,\n" +
                    "  [UpdatedOn] DATETIME,\n" +
                    "  PRIMARY KEY ([User_ID])\n" +
                    ");\n" +
                    "\n" +
                    "CREATE INDEX [UK] ON  [User] ([Username]);";

    public static void main(String[] args) throws SQLException, FileNotFoundException {
        clearTables();
        createTables();
        populateCountyTable();
    }

    private static Database connectToDatabase() throws SQLException {
        final Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
        return Database.fromBlocking(ConnectionProvider.from(connection));
    }

    private static void clearTables() throws SQLException {
        connectToDatabase()
                .update("DROP TABLE IF EXISTS LOG")
                .complete()
                .doOnError(Throwable::printStackTrace)
                .blockingAwait();

        connectToDatabase()
                .update("DROP TABLE IF EXISTS COUNTY")
                .complete()
                .doOnError(Throwable::printStackTrace)
                .blockingAwait();
    }

    private static void createTables() throws SQLException {
        connectToDatabase()
                .update(CREATE_TABLE_COUNTY)
                .complete()
                .doOnError(Throwable::printStackTrace)
                .blockingAwait();

        connectToDatabase()
                .update(CREATE_TABLE_LOG)
                .complete()
                .doOnError(Throwable::printStackTrace)
                .blockingAwait();
    }

    private static void populateCountyTable() throws SQLException, FileNotFoundException {
        // create reader to iterate county and geocode information in ks-county-geocodes.csv
        final CSVReader csvReader = new CSVReader(
                new FileReader(PopulateDatabase.class.getResource("/edu/dbgroup/dbutils/ks-county-geocodes.csv").getFile())
        );

        // send update command with reader's values, then block until this is done
        connectToDatabase()
                .update("INSERT INTO County(County_ID, Name) VALUES(?,?)")
                .parameterListStream(Flowable.fromIterable(csvReader)
                        .map(strings -> Arrays.asList(strings[0], strings[1])))
                .complete()
                .blockingAwait();
    }

}
