package edu.dbgroup.dbutils;

import com.github.davidmoten.guavamini.Preconditions;
import com.google.common.collect.Lists;
import com.opencsv.CSVReader;
import edu.dbgroup.logic.database.County;
import io.reactivex.Flowable;
import io.reactivex.annotations.Nullable;
import io.reactivex.flowables.GroupedFlowable;
import org.davidmoten.rx.jdbc.ConnectionProvider;
import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.ReturnGeneratedKeysBuilder;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A utility class which will automatically populate the weather app database with data.
 *
 * Note: this class only populates an existing database with data, the database itself is created elsewhere and must
 * be running for this class to have any effect.
 *
 * Note: Column definition for 1-1-2018_6-30-2018-ks.csv
 * "STATION","NAME","LATITUDE","LONGITUDE","ELEVATION","DATE","PRCP","SNOW","TAVG","TMAX","TMIN",
 * "WT01","WT02","WT03","WT04","WT05","WT06","WT07","WT08","WT09","WT11","WT15"
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
                    "  [WeatherType_ID] INT NOT NULL IDENTITY(1, 1) PRIMARY KEY,\n" +
                    "  [Value] VARCHAR(4) UNIQUE\n" +
//                    "  PRIMARY KEY ([WeatherType_ID])\n" +
                    ");";
//                    "\n" +
//                    "CREATE INDEX [UK] ON  [WeatherType] ([Value]);";


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
                    "  [Date] DATE\n" +
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
                    "  [Precipitation_ID] INT NOT NULL IDENTITY(1, 1) PRIMARY KEY,\n" +
                    "  [Water] DECIMAL,\n" +
                    "  [Snow] DECIMAL\n" +
//                    "  PRIMARY KEY ([Precipitation_ID])\n" +
                    ");";
//                    "\n" +
//                    "CREATE INDEX [NULLABLE] ON  [Precipitation] ([Water], [Snow]);";

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
                    "  [Name] VARCHAR(12)\n" +
                    ");";

    private static final String CREATE_TABLE_GOVERNMENT_DATA =
            "CREATE TABLE [GovernmentData] (\n" +
                    "  [GovernmentData_ID] INT NOT NULL IDENTITY(1, 1) PRIMARY KEY,\n" +
                    "  [Log_ID] INT NOT NULL FOREIGN KEY REFERENCES Log(Log_ID),\n" +
                    "  [Temperature_ID] INT NOT NULL FOREIGN KEY REFERENCES Temperature(Temperature_ID),\n" +
                    "  [Precipitation_ID] INT NOT NULL FOREIGN KEY REFERENCES Precipitation(Precipitation_ID),\n" +
//                    "  [WeatherType_ID] INT NOT NULL FOREIGN KEY REFERENCES WeatherType(WeatherType_ID),\n" +
                    "  [CreatedOn] DATETIME DEFAULT SYSDATETIME(),\n" +
                    "  [UpdatedOn] DATETIME DEFAULT SYSDATETIME(),\n" +
//                    "  PRIMARY KEY ([GovernmentData_ID])\n" +
                    ");";
//                    "\n" +
//                    "CREATE INDEX [FK] ON  [GovernmentData] ([Log_ID], [Temperature_ID], [Precipitation_ID];";
//                    "CREATE INDEX [FK] ON  [GovernmentData] ([Log_ID], [Temperature_ID], [Precipitation_ID], [WeatherType_ID]);";

    private static final String CREATE_TABLE_TEMPERATURE =
            "CREATE TABLE [Temperature] (\n" +
                    "  [Temperature_ID] INT NOT NULL IDENTITY(1, 1) PRIMARY KEY,\n" +
                    "  [Average] DECIMAL,\n" +
                    "  [High] DECIMAL,\n" +
                    "  [Low] DECIMAL\n" +
//                    "  PRIMARY KEY ([Temperature_ID])\n" +
                    ");";
//                    "\n" +
//                    "CREATE INDEX [NULLABLE] ON  [Temperature] ([Average], [High], [Low]);";

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

    public static void main(String[] args) throws SQLException, IOException {
        dropTables();
        createTables();
        populateCountyTable();
        populateLogTable(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 7, 1));
        populateGovernmentDataTable();
    }

    private static Database connectToDatabase() throws SQLException {
        final Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
        return Database.fromBlocking(ConnectionProvider.from(connection));
    }

    private static void dropTables() throws SQLException {
        dropTable("GovernmentData");
        dropTable("Precipitation");
        dropTable("Temperature");
        dropTable("WeatherType");
        dropTable("Log");
        dropTable("County");
    }

    private static void createTables() throws SQLException {
        createTable(CREATE_TABLE_COUNTY);
        createTable(CREATE_TABLE_LOG);
        createTable(CREATE_TABLE_PRECIPITATION);
        createTable(CREATE_TABLE_TEMPERATURE);
        createTable(CREATE_TABLE_WEATHER_TYPE);
        createTable(CREATE_TABLE_GOVERNMENT_DATA);
    }

    private static void dropTable(String tableName) throws SQLException {
        connectToDatabase()
                .update(String.format("DROP TABLE IF EXISTS %s", tableName))
                .complete()
                .doOnError(Throwable::printStackTrace)
                .blockingAwait();
    }

    private static void createTable(String createString) throws SQLException {
        connectToDatabase()
                .update(createString)
                .complete()
                .doOnError(Throwable::printStackTrace)
                .blockingAwait();
    }

    private static void populateCountyTable() throws SQLException, FileNotFoundException {
        // create reader to iterate county and geocode information in ks-county-geocodes.csv
        final CSVReader csvReader = new CSVReader(
                new FileReader(PopulateDatabase.class.getResource(
                        "/edu/dbgroup/dbutils/ks-county-geocodes.csv"
                ).getFile())
        );

        // send update command with reader's values, then block until this is done
        connectToDatabase()
                .update("INSERT INTO County(County_ID, Name) VALUES(?,?)")
                .parameterListStream(Flowable.fromIterable(csvReader)
                        .map(strings -> Arrays.asList(strings[0], strings[1])))
                .complete()
                .doOnError(Throwable::printStackTrace)
                .blockingAwait();
    }

    private static void populateLogTable(LocalDate startDate, LocalDate endDateExclusive) throws SQLException {
        // every day from startDate to endDate
        final Flowable<Date> dates =
                Flowable.rangeLong(0L, ChronoUnit.DAYS.between(startDate, endDateExclusive))
                        .map(daysSince -> Date.valueOf(startDate.plusDays(daysSince)));

        // every county in the County table
//        final Flowable<County> counties =
//        connectToDatabase()
//                .select("SELECT County_ID, Name FROM County")
//                .autoMap(County.class)
//                .blockingSubscribe(county -> {
//                    dates.blockingSubscribe(date -> {
//                        connectToDatabase()
//                                .update("INSERT INTO Log(County_ID, Date) VALUES(?, ?)")
//                                .parameters(county.countyID(), date)
//                                .complete()
//                                .doOnError(Throwable::printStackTrace)
//                                .blockingAwait();
//                    });
//                });

        final Flowable<County> counties =
            connectToDatabase()
                .select("SELECT County_ID, Name FROM County")
                .autoMap(County.class);

        // all possible pairs between the above dates and counties
        final Flowable<List<?>> cartesianProduct =
                counties.flatMap(county -> dates.map(date -> Lists.newArrayList(county.countyID(), date)));

        // insert all pairs as rows into Log table
        connectToDatabase()
                .update("INSERT INTO Log(County_ID, Date) VALUES(?, ?)")
                .parameterListStream(cartesianProduct)
                .complete()
                .doOnError(Throwable::printStackTrace)
                .blockingAwait();
    }

    private static void populateGovernmentDataTable() throws SQLException, IOException {
        @Nullable FileDataStore closeableRef = null;
        try {

            // load county border data
            final FileDataStore countyBorderDefs = FileDataStoreFinder.getDataStore(
                    PopulateDatabase.class.getResource("/edu/dbgroup/gui/data/counties/tl_2018_us_county.shp")
            );
            closeableRef = countyBorderDefs;

            // create reader to iterate gov weather data
            final CSVReader csvReader = new CSVReader(
                    new FileReader(PopulateDatabase.class.getResource(
                            "/edu/dbgroup/dbutils/1-1-2018_6-30-2018-ks.csv"
                    ).getFile())
            );

            // all rows of data taken from the read csv file
            final Flowable<GovDataRow> governmentData =
                    Flowable.fromIterable(csvReader).map(GovDataRow::createFromArray);

////             sort gov data into groups based on their county which is determined by their latitude/longitude
//            final Flowable<GroupedFlowable<Integer, GovDataRow>> govDataGroupedByCounty =
//                    governmentData.groupBy(govData -> getCountyByCoordinate(
//                            new Coordinate(govData.latitude, govData.longitude),
//                            countyBorderDefs
//                    ));

//            final peekOrSteal =
//
//            connectToDatabase()
//                    .update("INSERT INTO Precipitation(Water, Snow) VALUES(?,?)")
//                    .parameters(governmentData.bl, governmentData.snow)
//                    .returnGeneratedKeys()
//                    .getAs(Integer.class)


            // todo (if slow) reduce coordinate checks by ~6months*30days times with govData -> groupBy station
            // todo    -> map each station to countyID with only one comparison check
            governmentData.blockingSubscribe(govData -> {

                logger.info(govData.toString());

                final int precipKey = connectToDatabase()
                        .update("INSERT INTO Precipitation(Water, Snow) VALUES(?,?)")
                        .parameters(govData.precipitation, govData.snow)
                        .returnGeneratedKeys()
                        .getAs(Integer.class)
                        .singleOrError()
                        .blockingGet();

                final int tempKey = connectToDatabase()
                        .update("INSERT INTO Temperature(Average, Low, High) VALUES(?,?,?)")
                        .parameters(govData.tavg, govData.tmin, govData.tmax)
                        .returnGeneratedKeys()
                        .getAs(Integer.class)
                        .singleOrError()
                        .blockingGet();

                // todo WeatherKeys

                final int countyID =
                        getCountyByCoordinate(new Coordinate(govData.longitude, govData.latitude), countyBorderDefs);

                // get Log_ID from countyID
                final int logID = connectToDatabase()
                        .select("SELECT Log_ID FROM Log WHERE (County_ID = ?) AND (Date = ?)")
                        .parameters(countyID, govData.date)
                        .getAs(Integer.class)
                        .single(-1) // todo look into census coord->county edge cases, -1 is not a good solution
                        .blockingGet();

                // todo look into cencus cases (code represents extension of above todo)
                if (logID == -1) {
                    logger.warn(
                            String.format("forced to skip: %s\n unreconcilable noaa->census conversion",
                            govData.toString())
                    );
                    return;
                }

                connectToDatabase()
                        .update("INSERT INTO GovernmentData(Log_ID, Temperature_ID, Precipitation_ID) VALUES(?,?,?)")
                        .parameters(logID, tempKey, precipKey)
                        .complete()
                        .blockingAwait();
            }, Throwable::printStackTrace);

            // todo reconsider: do coords equal lat,long or do coord sys conversions need to occur???
//            governmentData
//                    .groupBy(data -> data.name) // first group by name (string comparison > n^2 geoutil check)
//                    .map(nameGrouping -> {
//                        final GovDataRow peek = nameGrouping.blockingFirst();
//                        final Coordinate coordinate = new Coordinate(peek.latitude, peek.longitude);
//
//                        re
//                    })

//            governmentData.blockingSubscribe(govData -> {
//
//                logger.debug(govData.toString());
//
//                final int precipKey = connectToDatabase()
//                        .update("INSERT INTO Precipitation(Water, Snow) VALUES(?,?)")
//                        .parameters(govData.precipitation, govData.snow)
//                        .returnGeneratedKeys()
//                        .getAs(Integer.class)
//                        .singleOrError()
//                        .blockingGet();
//
//                final int tempKey = connectToDatabase()
//                        .update("INSERT INTO Temperature(Average, Low, High) VALUES(?,?,?)")
//                        .parameters(govData.tavg, govData.tmin, govData.tmax)
//                        .returnGeneratedKeys()
//                        .getAs(Integer.class)
//                        .singleOrError()
//                        .blockingGet();
//
//                // todo WeatherKeys
//
//                final int countyID =
//                        getCountyByCoordinate(new Coordinate(govData.longitude, govData.latitude), countyBorderDefs);
//
//                // get Log_ID from countyID
//                final int logID = connectToDatabase()
//                        .select("SELECT Log_ID FROM Log WHERE (County_ID = ?) AND (Date = ?)")
//                        .parameters(countyID, govData.date)
//                        .getAs(Integer.class)
//                        .single(-1) // todo look into census coord->county edge cases, -1 is not a good solution
//                        .blockingGet();
//
//                // todo look into cencus cases (code represents extension of above todo)
//                if (logID == -1) {
//                    logger.warn(
//                            String.format("forced to skip: %s\n unreconcilable noaa->census conversion",
//                                    govData.toString())
//                    );
//                    return;
//                }
//
//                connectToDatabase()
//                        .update("INSERT INTO GovernmentData(Log_ID, Temperature_ID, Precipitation_ID) VALUES(?,?,?)")
//                        .parameters(logID, tempKey, precipKey)
//                        .complete()
//                        .blockingAwait();
//            }, Throwable::printStackTrace);

//            // TODO ADD PRECIP, TEMP, ETC TO DB THEN SHOVE IDS INTO GOV_DATA
//            connectToDatabase()
//                    .update("INSERT INTO Precipitation(Water, Snow) VALUES(?,?)")
//                    .parameterListStream(
//
//                    )
//                    .complete()
//                    .blockingAwait();
//
//
//            govDataGroupedByCounty.blockingSubscribe(groupedSubset -> connectToDatabase()
//                    .update("INSERT INTO GovernmentData(Log_ID, Temperature_ID, Precipitation_ID) VALUES(?,?,?)")
//                    .parameterListStream(
//                            groupedSubset.map(govData -> Lists.newArrayList("todo put stuff from INSERTS prior"))
//                    )
//                    .complete()
//                    .blockingAwait());


        } finally {
            if (closeableRef != null) {
                closeableRef.dispose();
            }
        }

    }

    private static Integer getCountyByCoordinate(Coordinate coordinate, FileDataStore kansasShapeData) throws IOException {

        try {
            final SimpleFeatureType schema = kansasShapeData.getFeatureSource().getSchema();
            final CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
            final String geomName = schema.getGeometryDescriptor().getLocalName();

//        final ReferencedEnvelope bounds = kansasShapeData.getFeatureSource().getBounds();

            final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
            final Filter filter = ff.intersects(ff.property(geomName), ff.literal(coordinate));

            SimpleFeatureCollection features = null;
            try {
                features = kansasShapeData.getFeatureSource().getFeatures(filter);

                if (features.size() != 1) {
                    throw new RuntimeException(
                            String.format("A coordinate was given which returned %d counties", features.size())
                    );
                }

                final String id = features.features().next().getID();

                return Integer.parseInt(id.substring("tl_2018_us_county.".length()));

            } finally {
                if (features != null) {
                    features.features().close();
                }
            }
        } catch (Exception e) {
            // todo REMOVE COMMENT
            // todo REMOVE COMMENT
            // todo REMOVE COMMENT
            // todo currently swallowing huge number of exceptions as a result of noaa -> census reconciliation issues
            // todo ... cannot display stack trace as collecting stack is way too expensive given number of ops
            // logger.error(String.format("unable to convert coordinate to county @ %s", coordinate.toString()), e);
            // todo REMOVE COMMENT
            // todo REMOVE COMMENT
            // todo REMOVE COMMENT
            return -1;
        }
    }

    /**
     * Helper class which stores a row of government data (from csv) as a class with all elements named.
     */
    private static class GovDataRow {

        final String station, name, /*latitude, longitude,*/ elevation, /*date,*/ /*precipitation, snow, tavg, tmax, tmin,*/
            wt01, wt02, wt03, wt04, wt05, wt06, wt07, wt08, wt09, wt11, wt15;

        final Double latitude, longitude, precipitation, snow, tavg, tmax, tmin;

        final Date date;

        GovDataRow(String station, String name, String latitude, String longitude, String elevation, String date,
                          String precipitation, String snow, String tavg, String tmax, String tmin, String wt01,
                          String wt02, String wt03, String wt04, String wt05, String wt06, String wt07, String wt08,
                          String wt09, String wt11, String wt15) {

            this.station = station;
            this.name = name;
            this.latitude = parseDoubleOrReturnNull(latitude);
            this.longitude = parseDoubleOrReturnNull(longitude);
            this.elevation = elevation;

            // parse date string YYYY-MM-DD to SQL date
            final String[] dateSlices = date.split("-");
            Preconditions.checkArgument(dateSlices.length == 3);
            final int year = Integer.parseInt(dateSlices[0]);
            final int month = Integer.parseInt(dateSlices[1]);
            final int day = Integer.parseInt(dateSlices[2]);
            // end parsing

            this.date = Date.valueOf(LocalDate.of(year, month, day));
            this.precipitation = parseDoubleOrReturnNull(precipitation);
            this.snow = parseDoubleOrReturnNull(snow);
            this.tavg = parseDoubleOrReturnNull(tavg);
            this.tmax = parseDoubleOrReturnNull(tmax);
            this.tmin = parseDoubleOrReturnNull(tmin);
            this.wt01 = wt01;
            this.wt02 = wt02;
            this.wt03 = wt03;
            this.wt04 = wt04;
            this.wt05 = wt05;
            this.wt06 = wt06;
            this.wt07 = wt07;
            this.wt08 = wt08;
            this.wt09 = wt09;
            this.wt11 = wt11;
            this.wt15 = wt15;
        }

        @Nullable
        Double parseDoubleOrReturnNull(String potentialDouble) {
            try {
                return Double.parseDouble(potentialDouble);
            } catch (Exception e) {
                return null;
            }
        }

        @Nullable
        Integer parseIntegerOrReturnNull(String potentialInteger) {
            try {
                return Integer.parseInt(potentialInteger);
            } catch (Exception e) {
                return null;
            }
        }

        static GovDataRow createFromArray(String[] strings) {
            System.out.println(strings.length + ", " + Arrays.toString(strings));
            Preconditions.checkArgument(strings.length == 22);
            return new GovDataRow(strings[0], strings[1], strings[2], strings[3],
                    strings[4], strings[5], strings[6], strings[7], strings[8],
                    strings[9], strings[10], strings[11], strings[12], strings[13],
                    strings[14], strings[15], strings[16], strings[17], strings[18],
                    strings[19], strings[20], strings[21]);
        }

        @Override
        public String toString() {
            return "GovDataRow{" +
                    "station='" + station + '\'' +
                    ", name='" + name + '\'' +
                    ", elevation='" + elevation + '\'' +
                    ", wt01='" + wt01 + '\'' +
                    ", wt02='" + wt02 + '\'' +
                    ", wt03='" + wt03 + '\'' +
                    ", wt04='" + wt04 + '\'' +
                    ", wt05='" + wt05 + '\'' +
                    ", wt06='" + wt06 + '\'' +
                    ", wt07='" + wt07 + '\'' +
                    ", wt08='" + wt08 + '\'' +
                    ", wt09='" + wt09 + '\'' +
                    ", wt11='" + wt11 + '\'' +
                    ", wt15='" + wt15 + '\'' +
                    ", latitude=" + latitude +
                    ", longitude=" + longitude +
                    ", precipitation=" + precipitation +
                    ", snow=" + snow +
                    ", tavg=" + tavg +
                    ", tmax=" + tmax +
                    ", tmin=" + tmin +
                    ", date=" + date +
                    '}';
        }
    }

}
