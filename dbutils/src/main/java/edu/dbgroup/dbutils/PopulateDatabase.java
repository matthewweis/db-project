package edu.dbgroup.dbutils;

import java.io.*;
import java.sql.Date;
import com.github.davidmoten.guavamini.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.ICSVParser;
import com.opencsv.RFC4180Parser;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import edu.dbgroup.logic.database.County;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.GroupedFlowable;
import io.reactivex.functions.Function3;
import io.reactivex.schedulers.Schedulers;
import org.davidmoten.rx.jdbc.*;
import org.davidmoten.rx.jdbc.pool.DatabaseType;
import org.davidmoten.rx.jdbc.tuple.Tuple2;
import org.davidmoten.rx.jdbc.tuple.Tuple3;
import org.geotools.data.FileDataStore;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    private static final String DATABASE_URL = "jdbc:sqlserver://localhost:1433";

    private final static Properties DATABASE_PROPERTIES = new Properties();
    static {
        DATABASE_PROPERTIES.setProperty("DatabaseName", "TestDB");
        DATABASE_PROPERTIES.setProperty("user", "SA");
        DATABASE_PROPERTIES.setProperty("password", "Group_15");
//        DATABASE_PROPERTIES.setProperty("rewriteBatchedStatements", "true");
    }

    // Durations should be in seconds or above
    private final static Duration IDLE_TIME_BEFORE_HEALTH_CHECK = Duration.ofDays(2);
    private final static Duration MAX_IDLE_TIME = Duration.ofDays(2); // max time for any (batch) operation to run
    private final static Duration CONNECTION_RETRY_INTERVAL = Duration.ofDays(2); // must also be very high

    private final static int MAX_POOL_SIZE = 4;
    private final static int MAX_BATCH_SIZE = 1000; // as per sqlserver specification

    /*
     * String to load driver for Microsoft sqlserver. This is only here as a contingency for legacy servers.
     */
//    private static final String JDBC_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    /*
     * Strings for creating each table in the database.
     */
//    private static final String CREATE_TABLE_USER_DATA =
//            "CREATE TABLE [UserData] (\n" +
//                    "  [UserData_ID] INT,\n" +
//                    "  [User_ID] INT,\n" +
//                    "  [Log_ID] INT,\n" +
//                    "  [Temperature_ID] INT,\n" +
//                    "  [Precipitation_ID] INT,\n" +
//                    "  [CreatedOn] DATETIME,\n" +
//                    "  [UpdatedOn] DATETIME,\n" +
//                    "  PRIMARY KEY ([UserData_ID])\n" +
//                    ");\n" +
//                    "\n" +
//                    "CREATE INDEX [FK] ON  [UserData] ([User_ID], [Log_ID], [Temperature_ID], [Precipitation_ID]);";

    private static final String CREATE_TABLE_WEATHER_TYPE =
            "CREATE TABLE [WeatherType] (\n" +
                    "  [WeatherType_ID] INT NOT NULL IDENTITY(1, 1) PRIMARY KEY,\n" +
                    "  [Value] VARCHAR(4) UNIQUE\n" +
//                    "  PRIMARY KEY ([WeatherType_ID])\n" +
                    ");";
//                    "\n" +
//                    "CREATE INDEX [UK] ON  [WeatherType] ([Value]);";


//    private static final String CREATE_TABLE_USER_WEATHER_TYPE =
//            "CREATE TABLE [UserWeatherType] (\n" +
//                    "  [UserData_ID] INT DEFAULT NULL,\n" +
//                    "  [WeatherType_ID] INT DEFAULT NULL\n" +
//                    ");\n" +
//                    "\n" +
//                    "CREATE INDEX [UK] ON  [UserWeatherType] ([UserData_ID], [WeatherType_ID]);";

//    private static final String CREATE_TABLE_GOVERNMENT_WEATHER_TYPE =
//            "CREATE TABLE [GovernmentWeatherType] (\n" +
//                    "  [GovernmentData_ID] INT DEFAULT NULL,\n" +
//                    "  [WeatherType_ID] INT DEFAULT NULL\n" +
//                    ");\n" +
//                    "\n" +
//                    "CREATE INDEX [UK] ON  [GovernmentWeatherType] ([GovernmentData_ID], [WeatherType_ID]);";

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

//    private static final String CREATE_TABLE_ZIP =
//            "CREATE TABLE [Zip] (\n" +
//                    "  [Zip_Code ] INT,\n" +
//                    "  [County_ID] INT,\n" +
//                    "  PRIMARY KEY ([Zip_Code ])\n" +
//                    ");\n" +
//                    "\n" +
//                    "CREATE INDEX [FK] ON  [Zip] ([County_ID]);";

    private static final String CREATE_TABLE_PRECIPITATION =
            "CREATE TABLE [Precipitation] (\n" +
                    "  [Precipitation_ID] INT NOT NULL IDENTITY(1, 1) PRIMARY KEY,\n" +
                    "  [Water] DECIMAL(3,2) DEFAULT NULL,\n" +
                    "  [Snow] DECIMAL(2,1) DEFAULT NULL\n" +
//                    "  PRIMARY KEY ([Precipitation_ID])\n" +
                    ");";
//                    "\n" +
//                    "CREATE INDEX [NULLABLE] ON  [Precipitation] ([Water], [Snow]);";

//    private static final String CREATE_TABLE_WARNING =
//            "CREATE TABLE [Warning] (\n" +
//                    "  [Warning_ID] INT,\n" +
//                    "  [UserData_ID] INT,\n" +
//                    "  [Severity] INT,\n" +
//                    "  [Category] INT,\n" +
//                    "  [Description] VARCHAR(256),\n" +
//                    "  [CreatedOn] DATETIME,\n" +
//                    "  [UpdatedOn] DATETIME,\n" +
//                    "  PRIMARY KEY ([Warning_ID])\n" +
//                    ");\n" +
//                    "\n" +
//                    "CREATE INDEX [FK] ON  [Warning] ([UserData_ID]);";

    // depends on Log
    private static final String CREATE_TABLE_COUNTY =
            "CREATE TABLE [County] (\n" +
//                    "  [County_ID] INT NOT NULL IDENTITY(1, 1) PRIMARY KEY,\n" +
                    "  [County_ID] INT NOT NULL PRIMARY KEY,\n" + // equals GEOCODE
//                    "  [Log_ID] INT NOT NULL FOREIGN KEY REFERENCES Log(Log_ID),\n" +
//            "  [PhysioRegion_ID] INT,\n" +
                    "  [Name] VARCHAR(12) NOT NULL\n" +
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
                    "  [Average] INT DEFAULT NULL,\n" +
                    "  [High] INT DEFAULT NULL,\n" +
                    "  [Low] INT DEFAULT NULL\n" +
//                    "  PRIMARY KEY ([Temperature_ID])\n" +
                    ");";
//                    "\n" +
//                    "CREATE INDEX [NULLABLE] ON  [Temperature] ([Average], [High], [Low]);";

//    private static final String CREATE_TABLE_COUNTY_PHYSIO_REGION =
//            "CREATE TABLE [CountyPhysioRegion] (\n" +
//                    "  [County_ID] INT,\n" +
//                    "  [PhysioRegion_ID] INT\n" +
//                    ");\n" +
//                    "\n" +
//                    "CREATE INDEX [UK] ON  [CountyPhysioRegion] ([County_ID], [PhysioRegion_ID]);";

//    private static final String CREATE_TABLE_PHYSIO_REGION =
//            "CREATE TABLE [PhysioRegion] (\n" +
//                    "  [PhysioRegion_ID] INT,\n" +
//                    "  [Name] VARCHAR(12),\n" +
//                    "  PRIMARY KEY ([PhysioRegion_ID])\n" +
//                    ");";

//    private static final String CREATE_TABLE_USER =
//            "CREATE TABLE [User] (\n" +
//                    "  [User_ID] INT,\n" +
//                    "  [FirstName] VARCHAR(30),\n" +
//                    "  [LastName] VARCHAR(30),\n" +
//                    "  [Username] VARCHAR(20),\n" +
//                    "  [CreatedOn] DATETIME,\n" +
//                    "  [UpdatedOn] DATETIME,\n" +
//                    "  PRIMARY KEY ([User_ID])\n" +
//                    ");\n" +
//                    "\n" +
//                    "CREATE INDEX [UK] ON  [User] ([Username]);";

    public static void main(String[] args) throws IOException {

        boolean useNewMethod = true;

        if (useNewMethod) {
            return; // todo rm if generating is needed
//            createFiles();
//            return;
        }


        final ImmutableBiMap<String, Integer> countyToIDMap = createCountyByNameMap();

        final Flowable<File> files2 = Flowable.fromArray(
                Objects.requireNonNull(
                        new File(PopulateDatabase.class.getResource(
                                "/edu/dbgroup/dbutils/weather-data-flat").getPath()).listFiles()
                )
        ).filter(file -> file.getName().endsWith(".csv"));
        files2.blockingSubscribe(file -> {
            final String[] colDefs = getFirstLine(file);
            final int[] rowIndicesMapping = getRowIndicesMapping(colDefs);

            System.out.println("County: " + file.getName() + " [ id = " +
                    countyToIDMap.get(file.getName().substring(0, file.getName().length()-".csv".length())) + " ]");
            System.out.println(" => " + Arrays.toString(colDefs));
            System.out.println(" => " + Arrays.toString(rowIndicesMapping));
        });

        blockUntilUserTypesDelete();

        logger.info("Connecting to database...");
        try (final Database database = getDatabase()) {

            logger.info("Dropping existing tables in database...");
            dropTables(database);

            logger.info("Creating tables in database...");
            createTables(database);

            logger.info("Populating table: County...");
            populateCountyTable(database);

            logger.info("Populating table: Log...");
            populateLogTable(LocalDate.of(2004, 1, 1), LocalDate.of(2004, 1, 3), database);

            logger.info("Populating table GovernmentData...");

            final Flowable<File> files = Flowable.fromArray(
                    Objects.requireNonNull(
                            new File(PopulateDatabase.class.getResource(
                                    "/edu/dbgroup/dbutils/weather-data-flat").getPath()).listFiles()
                    )
            ).filter(file -> file.getName().endsWith(".csv"));

            files.blockingSubscribe(file -> {
                logger.info(String.format(" => Populating GovernmentData with %s", file.getName()));

                String countyName = file.getName().substring(0, file.getName().length()-".csv".length());
                if (countyName.endsWith("20072018") || countyName.endsWith("20022018")) {
                    countyName = countyName.substring(0, countyName.length() - "200_2018".length());
                }

                try {
                    populateGovernmentDataTable(database, file, countyToIDMap.get(countyName));
                } catch (Exception e) {
                    logger.error(String.format("County %s threw and error and was not completed. %s", countyName), e);
                }
            }, Throwable::printStackTrace);

            logger.info("Closing database...");
        }
        logger.info("Success! Program will now exit.");
    }

    public static void createFiles() throws FileNotFoundException, UnsupportedEncodingException {
        final ImmutableBiMap<String, Integer> countyMap = createCountyByNameMap();

        final PrintWriter govDataWriter = new PrintWriter("govData.csv", "UTF-8");
        final PrintWriter tempWriter = new PrintWriter("temperature.csv", "UTF-8");
        final PrintWriter precipWriter = new PrintWriter("precipitation.csv", "UTF-8");
        final PrintWriter weatherWriter = new PrintWriter("weather.csv", "UTF-8");

        final Flowable<File> files = Flowable.fromArray(
                Objects.requireNonNull(
                        new File(PopulateDatabase.class.getResource(
                                "/edu/dbgroup/dbutils/weather-data-flat").getPath()).listFiles()
                )
        ).filter(file -> file.getName().endsWith(".csv"));

        { // first line
            final List<String> precipRow =
                    Lists.newArrayList("Precipitation_ID", "PRCP", "SNOW");

            final List<String> tempRow =
                    Lists.newArrayList("Temperature_ID", "TAVG", "TMAX", "TMIN");

            final String[] wts = new String[22];
            for (int i = 0; i < wts.length; i++) {
                final int val = i + 1;
                if (val < 10) {
                    wts[i] = "WT0" + val;
                } else {
                    wts[i] = "WT" + val;
                }
            }

            final List<String> weatherRow = Lists.asList("WeatherType_ID", wts);

            final List<String> govDataRow =
                    Lists.newArrayList("GovernmentData_ID", "Precipitation_ID", "Temperature_ID", "WeatherType_ID",
                            "County_ID", "Date", "LATITUDE", "LONGITUDE", "Station", "Elevation");

            precipWriter.println("\"" + String.join("\",\"", precipRow) + "\"");
            tempWriter.println("\"" + String.join("\",\"", tempRow) + "\"");
            weatherWriter.println("\"" + String.join("\",\"", weatherRow) + "\"");
            govDataWriter.println("\"" + String.join("\",\"", govDataRow) + "\"");
        }

        files.forEach(file -> {
            logger.info(String.format(" => Populating GovernmentData with %s", file.getName()));

            String countyName = file.getName().substring(0, file.getName().length()-".csv".length());
            if (countyName.endsWith("20072018") || countyName.endsWith("20022018")) {
                countyName = countyName.substring(0, countyName.length() - "200_2018".length());
            }

            final int countyID = countyMap.get(countyName);

            try(final CSVReader csvReader = new CSVReaderBuilder(new FileReader(file))
                    .withCSVParser(getDataParser())
                    .withSkipLines(1)
                    .build()) {

                final int[] rowIndiciesMapping = getRowIndicesMapping(getFirstLine(file));

                csvReader.forEach(row -> {

                    final GovDataRow govData =
                            GovDataRow.createFromArray(formatWeatherTypeStrings(rowIndiciesMapping, row));

                    final int UID = nextUID();

                    final List<String> precipRow =
                            Lists.newArrayList(Integer.toString(UID), getOrNull(govData.precipitation), getOrNull(govData.snow));

                    final List<String> tempRow =
                            Lists.newArrayList(Integer.toString(UID), getOrNull(govData.tavg), getOrNull(govData.tmax), getOrNull(govData.tmin));

                    final List<String> weatherRow = Lists.asList(Integer.toString(UID), govData.wts);

                    final List<String> govDataRow =
                            Lists.newArrayList(Integer.toString(UID), Integer.toString(UID), Integer.toString(UID),
                                    Integer.toString(UID), Integer.toString(countyID), govData.date.toString(),
                                    getOrNull(govData.latitude), getOrNull(govData.longitude),
                                    govData.station, govData.elevation);

                    precipWriter.println("\"" + String.join("\",\"", precipRow) + "\"");
                    tempWriter.println("\"" + String.join("\",\"", tempRow) + "\"");
                    weatherWriter.println("\"" + String.join("\",\"", weatherRow) + "\"");
                    govDataWriter.println("\"" + String.join("\",\"", govDataRow) + "\"");

                });

            }
        });

        weatherWriter.close();
        precipWriter.close();
        tempWriter.close();
        govDataWriter.close();
    }

    static String getOrNull(Optional n) {
        if (n.isPresent()) {
            return n.get().toString();
        } else {
            return "";
        }
    }


    static int _UID = 0;

    static int nextUID() {
        return _UID++;
    }

    public static void run() throws IOException {

        final ImmutableBiMap<Integer, String> countyMap = createCountyByNameMap().inverse();

        blockUntilUserTypesDelete();

        logger.info("Connecting to database...");
        try (final Database db = getDatabase()) {

            dropTables(db);
            createTables(db);

            final LocalDate startDate = LocalDate.of(2000, 1, 1);
            final LocalDate endDateExclusive = LocalDate.of(2018, 1, 1);

            final Flowable<Date> dates =
                    Flowable.rangeLong(0L, ChronoUnit.DAYS.between(startDate, endDateExclusive))
                            .map(daysSince -> Date.valueOf(startDate.plusDays(daysSince)));

            final Flowable<Tuple2<Integer, Flowable<GovDataRow>>> govData = govDataRowFlowable(countyMap.inverse());


            populateCountyTable(db);

            db
                    .select("SELECT County_ID FROM County")
                    .transactedValuesOnly()
                    .getAs(Integer.class)
                    .onBackpressureBuffer()
//                    .doOnEach(System.out::println)
                    .doOnError(Throwable::printStackTrace)
                    .subscribeOn(Schedulers.computation())
                    .flatMap(tx -> tx.update("INSERT INTO GovernmentData VALUES(?,?,?)")
                            .parameterListStream(Flowable.zip(

                                    tx.update("INSERT INTO Log VALUES(?,?)")
                                            .parameterListStream(dates.map(date -> list(tx.value(), date)))
                                            .transactedValuesOnly()
                                            .returnGeneratedKeys()
                                            .getAs(Integer.class)
                                            .onBackpressureBuffer()
                                            .subscribeOn(Schedulers.computation())
//                                            .doOnEach(System.out::println)
                                            .doOnError(Throwable::printStackTrace),

                                    tx.update("INSERT INTO Temperature VALUES(?,?,?)")
                                            .parameterListStream(govData.flatMap(Tuple2::_2)
                                                    .map(row -> list(row.tavg.orElse(null), row.tmin.orElse(null), row.tmax.orElse(null))))
                                            .transactedValuesOnly()
                                            .returnGeneratedKeys()
                                            .getAs(Integer.class)
                                            .onBackpressureBuffer()
                                            .subscribeOn(Schedulers.computation())
//                                            .doOnEach(System.out::println)
                                            .doOnError(Throwable::printStackTrace),

                                    tx.update("INSERT INTO Precipitation VALUES(?,?)")
                                            .parameterListStream(govData.flatMap(Tuple2::_2)
                                                    .map(row -> list(row.precipitation.orElse(null), row.snow.orElse(null))))
                                            .transactedValuesOnly()
                                            .returnGeneratedKeys()
                                            .getAs(Integer.class)
                                            .onBackpressureBuffer()
                                            .subscribeOn(Schedulers.computation())
//                                            .doOnEach(System.out::println)
                                            .doOnError(Throwable::printStackTrace),

                                    (a, b, c) -> list(a.value(), b.value(), c.value())

                            ))
                            .transactedValuesOnly()
                            .returnGeneratedKeys()
                            .getAs(Integer.class)
                            .onBackpressureBuffer()
                            .subscribeOn(Schedulers.computation())
//                            .doOnEach(System.out::println)
                            .doOnError(Throwable::printStackTrace))
                    .blockingSubscribe(x -> {  }, Throwable::printStackTrace);
        }
    }

    private static Flowable<String[]> countyGeocodeFlowable() throws FileNotFoundException {
        return Flowable.fromIterable(new CSVReaderBuilder(new FileReader(PopulateDatabase.class.getResource(
                    "/edu/dbgroup/dbutils/ks-county-geocodes.csv").getFile())).withCSVParser(getDataParser()).build());
    }

    private static Flowable<File> weatherDataFilesFlowable() {
        return Flowable.fromArray(
                Objects.requireNonNull(
                        new File(PopulateDatabase.class.getResource(
                                "/edu/dbgroup/dbutils/weather-data-flat").getPath()).listFiles()
                )).filter(file -> file.getName().endsWith(".csv"));
    }

    private static Flowable<Tuple2<Integer, Flowable<GovDataRow>>> govDataRowFlowable(Map<String, Integer> countyMap) {
        return weatherDataFilesFlowable()
                .map(file -> Tuple2.create(
                        countyMap.get(file.getName().substring(0, ".csv".length())),
                        Flowable.fromIterable(getCSVReaderOfFile(file, 1)).map(row ->
                                GovDataRow.createFromArray(
                                        formatWeatherTypeStrings(getRowIndicesMapping(getFirstLine(file)), row)
                                )
                        ))
                );
    }

    private static CSVReader getCSVReaderOfFile(File file, int skipLines) throws FileNotFoundException {
        return new CSVReaderBuilder(new FileReader(file))
                .withCSVParser(getDataParser())
                .withSkipLines(skipLines)
                .build();
    }

    private static void blockUntilUserTypesDelete() {
        System.out.println("WARNING! This will delete any existing database. Type \"DELETE\" to continue.");

        try (final Scanner scanner = new Scanner(System.in)) {
            boolean confirmed = false;
            while (!confirmed) {
                confirmed = scanner.nextLine().equalsIgnoreCase("DELETE");
            }
            System.out.println();
        }
    }

    private static Database getDatabase() {
        return createDatabase();
    }

    private static Database createDatabase() {
        return Database
                .nonBlocking()
                .url(DATABASE_URL)
                .healthCheck(DatabaseType.SQL_SERVER)
                .idleTimeBeforeHealthCheck(IDLE_TIME_BEFORE_HEALTH_CHECK.getSeconds(), TimeUnit.SECONDS)
                .maxIdleTime(MAX_IDLE_TIME.getSeconds(), TimeUnit.SECONDS)
                .connectionRetryInterval(CONNECTION_RETRY_INTERVAL.getSeconds(), TimeUnit.SECONDS)
                .maxPoolSize(MAX_POOL_SIZE)
                .properties(DATABASE_PROPERTIES)
                .connnectionListener(connection ->
                        logger.debug(String.format("Database recieved new connection: %s", connection.toString())))
                .build();
    }

    private static void dropTables(Database database) {
        dropTable("GovernmentData", database);
        dropTable("Precipitation", database);
        dropTable("Temperature", database);
        dropTable("WeatherType", database);
        dropTable("Log", database);
        dropTable("County", database);
    }

    private static void createTables(Database database) {
        createTable(CREATE_TABLE_COUNTY, database);
        createTable(CREATE_TABLE_LOG, database);
        createTable(CREATE_TABLE_PRECIPITATION, database);
        createTable(CREATE_TABLE_TEMPERATURE, database);
        createTable(CREATE_TABLE_WEATHER_TYPE, database);
        createTable(CREATE_TABLE_GOVERNMENT_DATA, database);
    }

    private static void dropTable(String tableName, Database database) {
        database.update(String.format("DROP TABLE IF EXISTS %s", tableName))
                .counts()
                .onBackpressureBuffer()
                .doOnError(Throwable::printStackTrace)
                .blockingSubscribe();
    }

    private static UpdateBuilder dropTable2(String tableName, Database database) {
        return database.update(String.format("DROP TABLE IF EXISTS %s", tableName));
    }

    private static void createTable(String createString, Database database) {
        database.update(createString)
                .counts()
                .onBackpressureBuffer()
                .doOnError(Throwable::printStackTrace)
                .blockingSubscribe();
    }

    private static UpdateBuilder createTable2(String createString, Database database) {
        return database.update(createString);
    }

    private static void populateCountyTable(Database database) throws FileNotFoundException {

        // create reader to iterate county and geocode information in ks-county-geocodes.csv
        final CSVReaderBuilder builder = new CSVReaderBuilder(new FileReader(PopulateDatabase.class.getResource(
                "/edu/dbgroup/dbutils/ks-county-geocodes.csv"
        ).getFile()));

        builder.withCSVParser(getDataParser());

        final CSVReader csvReader = builder.build();

        // send update command with reader's values, then block until this is done
        database.update("INSERT INTO County(County_ID, Name) VALUES(?,?)")
                .parameterListStream(Flowable.fromIterable(csvReader)
                        .map(strings -> Arrays.asList(strings[0], strings[1])))
                .counts()
                .onBackpressureBuffer()
                .doOnError(Throwable::printStackTrace) // todo move to subscribe
                .blockingSubscribe();
    }

    private static void populateLogTable(@NonNull LocalDate startDate, @NonNull LocalDate endDateExclusive,
                                         @NonNull Database database) {
        // every day from startDate to endDate
        final Flowable<Date> dates =
                Flowable.rangeLong(0L, ChronoUnit.DAYS.between(startDate, endDateExclusive))
                        .map(daysSince -> Date.valueOf(startDate.plusDays(daysSince)));

        // every county in the county table
        final Flowable<County> counties =
                database.select("SELECT County_ID, Name FROM County")
                        .autoMap(County.class);

        // all possible pairs between dates and counties
        final Flowable<List<?>> cartesianProduct =
                counties.flatMap(county -> dates.map(date -> Lists.newArrayList(county.countyID(), date)));

        // insert all pairs as rows into Log table
        database.update("INSERT INTO Log(County_ID, Date) VALUES(?,?)")
                .parameterListStream(cartesianProduct)
                .batchSize(MAX_BATCH_SIZE)
                .counts()
                .onBackpressureBuffer()
                .doOnError(Throwable::printStackTrace)
                .blockingSubscribe();
    }

    private static String[] getFirstLine(File file) throws IOException {
        try (final CSVReader oneLineOnly =
                new CSVReaderBuilder(new FileReader(file)).withCSVParser(getDataParser()).build()) {
            return oneLineOnly.peek();
        }
    }

    private static void populateGovernmentDataTable(Database database, File file, int countyID) throws IOException {

        final String[] columns = getFirstLine(file);

        try(final CSVReader csvReader = new CSVReaderBuilder(new FileReader(file))
                .withCSVParser(getDataParser())
                .withSkipLines(1)
                .build()) {

            final int[] rowIndiciesMapping = getRowIndicesMapping(columns);

            // all rows of data taken from the read csv file
            final Flowable<GovDataRow> governmentData =
                    Flowable.fromIterable(csvReader).map(row ->
                            GovDataRow.createFromArray(formatWeatherTypeStrings(rowIndiciesMapping, row))
                    );


            final Flowable<Tuple2<Integer, GovDataRow>> flattenedGovData =
                    governmentData.map(govData -> Tuple2.create(countyID, govData));

            // Precipitation
            final Flowable<List<?>> flatPrecipitation =
                    flattenedGovData.map(pair -> list(pair.value2().precipitation, pair.value2().snow));

            final Flowable<Optional<Integer>> precipKeys = database.update("INSERT INTO Precipitation(Water, Snow) VALUES(?,?)")
                    .parameterListStream(flatPrecipitation)
                    .returnGeneratedKeys()
                    .getAsOptional(Integer.class);


            // Temperature
            final Flowable<List<?>> flatTemperature =
                    flattenedGovData.map(pair -> Lists.newArrayList(pair.value2().tavg, pair.value2().tmin, pair.value2().tmax));

            final Flowable<Optional<Integer>> tempKeys = database.update("INSERT INTO Temperature(Average, Low, High) VALUES(?,?,?)")
                    .parameterListStream(flatTemperature)
                    .returnGeneratedKeys()
                    .getAsOptional(Integer.class);


            // Weather todo

            final Flowable<List<?>> flatLog =
                    flattenedGovData.map(pair -> list(pair.value1(), pair.value2().date));

            final Flowable<Optional<Integer>> logKeys = database
                    .select("SELECT Log_ID FROM Log WHERE (County_ID = ?) AND (Date = ?)")
                    .parameterListStream(flatLog)
                    .getAsOptional(Integer.class);

            final Flowable<List<?>> zippedKeys = logKeys
                    .zipWith(tempKeys, Tuple2::create)
                    .zipWith(precipKeys, (pair, precip) -> list(pair.value1(), pair.value2(), precip));

            zippedKeys.blockingForEach(x -> System.out.println(Arrays.toString(x.toArray())));

            database.update("INSERT INTO GovernmentData(Log_ID, Temperature_ID, Precipitation_ID) VALUES(?,?,?)")
                    .parameterListStream(zippedKeys)
                    .batchSize(MAX_BATCH_SIZE)
                    .counts()
                    .onBackpressureBuffer()
                    .doOnError(Throwable::printStackTrace)
                    .blockingSubscribe();
        }
    }

    private static ICSVParser getDataParser() {
        return new RFC4180Parser();
    }

    private static ImmutableBiMap<String, Integer> createCountyByNameMap() throws FileNotFoundException {
        final ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder();

        final CSVReader reader = new CSVReaderBuilder(new FileReader(PopulateDatabase.class.getResource(
                "/edu/dbgroup/dbutils/ks-county-geocodes.csv"
        ).getFile())).build();

        Flowable.fromIterable(reader).blockingSubscribe(line -> {
            final int geocode = Integer.parseInt(line[0]);
            final String countyName = line[1];
            builder.put(countyName, geocode);
        });

        return builder.build();
    }

    private static Integer getCountyByCoordinate(@NonNull Coordinate coordinate,
                                                 @NonNull FileDataStore kansasShapeData) throws IOException {

        final SpatialIndexFeatureCollection counties =
                new SpatialIndexFeatureCollection(kansasShapeData.getFeatureSource().getFeatures());

        final GeometryFactory geometryFactory = new GeometryFactory();
        final Point point = geometryFactory.createPoint(coordinate);

        final Iterator<SimpleFeature> iterator = counties.iterator();

        while (iterator.hasNext()) {

            final SimpleFeature next = iterator.next();

//            final MultiPolygon polygon = (MultiPolygon) next.getAttribute(0);
//            if (polygon.contains(point)) {
//                final String id = next.getID();
//                return Integer.parseInt(id.substring("tl_2018_us_county.".length()));
//            }

            // above or below works

            if (((Geometry)next.getDefaultGeometry()).contains(point)) {
                final String id = next.getID();
                return Integer.parseInt(id.substring("tl_2018_us_county.".length()));
            }
        }

        throw new RuntimeException("Unable to get county from point");

//        final SimpleFeatureType schema = kansasShapeData.getFeatureSource().getSchema();
//        final String geomName = schema.getGeometryDescriptor().getLocalName();
//
//        final GeometryFactory factory = new GeometryFactory();
//        final Point point = factory.createPoint(coordinate);
//
//        final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
//        final Filter filter = ff.contains(ff.property(geomName), ff.literal(point));
//
//        final SimpleFeatureCollection features = kansasShapeData.getFeatureSource().getFeatures(filter);
////            System.out.println("FEATURE INFO: " + features.getID() + ", " + Arrays.toString(features.toArray()));
////
////            if (features.size() != 1) {
////                throw new RuntimeException(
////                        String.format(
////                                "A coordinate %s was given which returned %d counties",
////                                coordinate.toString(),
////                                features.size()
////                        )
////                );
////            }
//        final SimpleFeatureIterator iterator = features.features();
//        try {
//            final String id = features.features().next().getID();
//            return Integer.parseInt(id.substring("tl_2018_us_county.".length()));
//        } finally {
//            kansasShapeData.dispose();
//            iterator.close();
//        }
    }

    private static List<?> list(Object ... objects) {
        return Lists.newArrayList(objects);
    }


//    /**
//     *
//     * @param bits must not have a nonzero bit greater than 21
//     * @return
//     */
//    private static String[] formatWeatherTypeStrings(int bits, @NonNull String[] values) {
//        final int msb = 21;
//        Preconditions.checkArgument(bits == (bits & (IntMath.pow(2, msb + 1) - 1)));
//        final int offset = 10; // NOT ALWAYS TRUE
//
//
//        final String[] strings = new String[msb];
//
//        int currVal = 0;
//        for (int i=0; i < strings.length; i++) {
//            if (i < offset) {
//                strings[i] = values[i];
//            } else {
//                if ((bits | (1 << (i - offset))) == bits) {
//                    strings[i] = values[offset + currVal++];
//                } else {
//                    strings[i] = null;
//                }
//            }
//        }
//
//        return strings;
//    }

    private static String[] formatWeatherTypeStrings(int[] rowIndicesMapping, String[] values) {
        Preconditions.checkArgument(rowIndicesMapping.length == values.length);
        final String[] formattedStrings = new String[32];
        for (int i=0; i < rowIndicesMapping.length; i++) {
            formattedStrings[rowIndicesMapping[i]] = values[i];
        }

        return formattedStrings;
    }

    private static int[] getRowIndicesMapping(String[] columnDefs) {
        final int[] mapping = new int[columnDefs.length];

        final String[] possibleCols = new String[] {
                "STATION", "LATITUDE", "LONGITUDE", "ELEVATION", "DATE", "PRCP", "SNOW", "TAVG", "TMAX", "TMIN"
        };

        for (int i=0; i < columnDefs.length; i++) {
            boolean wasChanged = false;

            for (int j=0; j < possibleCols.length; j++) {
                if (columnDefs[i].equals(possibleCols[j])) {
                    mapping[i] = j;
                    wasChanged = true;
                    break;
                }
            }

            if (!wasChanged && columnDefs[i].startsWith("WT")) {
                final int wt = Integer.parseInt(columnDefs[i].substring(2));
                mapping[i] = possibleCols.length + wt - 1;
            }
        }

        return mapping;
    }



    private static int getBitsFromWeatherTypeStrings(@NonNull String[] values) {
        int bits = 0;
        for (int i=0; i < values.length; i++) {
            if (values[i].startsWith("WT")) {
                final int val = Integer.parseInt(values[i].substring(2)) - 1; // minus one to be zero based
                bits |= (1 << val);
            }
        }
        return bits;
    }


    /**
     * Helper class which stores a row of government data (from csv) as a class with all elements named.
     */
    private static class GovDataRow {

        final String station, /*latitude, longitude,*/
                elevation; /*date,*/ /*precipitation, snow, tavg, tmax, tmin,*/
        //            wt01, wt02, wt03, wt04, wt05, wt06, wt07, wt08, wt09, wt11, wt15, wt16, wt17, wt18, wt19, wt20, wt21, wt22;
        final String[] wts;

        final Optional<Double> latitude, longitude, precipitation, snow;
        final Optional<Integer> tavg, tmax, tmin;

        final Date date;

        GovDataRow(String station, String latitude, String longitude, String elevation, String date,
                   String precipitation, String snow, String tavg, String tmax, String tmin, String[] wts) {

            this.station = station;
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
            this.tavg = parseIntegerOrReturnNull(tavg);
            this.tmax = parseIntegerOrReturnNull(tmax);
            this.tmin = parseIntegerOrReturnNull(tmin);

            this.wts = wts;
        }

        @Nullable
        Optional<Double> parseDoubleOrReturnNull(String potentialDouble) {
            try {
                return Optional.of(Double.parseDouble(potentialDouble));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        @Nullable
        Optional<Integer> parseIntegerOrReturnNull(String potentialDouble) {
            try {
                return Optional.of(Integer.parseInt(potentialDouble));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        static GovDataRow createFromArray(String[] strings) {
            Preconditions.checkArgument(strings.length == 32);

            for (int i=0; i < strings.length; i++) {
                strings[i] = Strings.nullToEmpty(strings[i]);
            }

            return new GovDataRow(strings[0], strings[1], strings[2], strings[3],
                    strings[4], strings[5], strings[6], strings[7], strings[8],
                    strings[9], Arrays.copyOfRange(strings, 10, strings.length));
        }

        @Override
        public String toString() {
            return "GovDataRow{" +
                    "station='" + station + '\'' +
                    ", elevation='" + elevation + '\'' +
                    ", wts=" + Arrays.toString(wts) +
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
