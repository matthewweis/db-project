package edu.dbgroup.dbutils;

import com.github.davidmoten.guavamini.Preconditions;
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
import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.flowables.GroupedFlowable;
import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.DatabaseType;
import org.davidmoten.rx.jdbc.tuple.Tuple2;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
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
    }

    // Durations should be in seconds or above
    private final static Duration IDLE_TIME_BEFORE_HEALTH_CHECK = Duration.ofSeconds(5);
    private final static Duration MAX_IDLE_TIME = Duration.ofDays(2); // max time for any (batch) operation to run
    private final static Duration CONNECTION_RETRY_INTERVAL = Duration.ofDays(2); // must also be very high

    private final static int MAX_POOL_SIZE = 8;

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
        logger.info("Connecting to database...");
        try (final Database database = getDatabase()) {

            logger.info("Dropping existing tables in database...");
            dropTables(database);

            logger.info("Creating tables in database...");
            createTables(database);

            logger.info("Populating table: County...");
            populateCountyTable(database);

            logger.info("Populating table: Log...");
            populateLogTable(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 7, 1), database);

            logger.info("Populating table GovernmentData...");
            populateGovernmentDataTable(database);

            logger.info("Closing database...");
        }
        logger.info("Success! Program will now exit.");
    }
//
    private static Database getDatabase() {
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

    private static void createTable(String createString, Database database) {
        database.update(createString)
                .counts()
                .onBackpressureBuffer()
                .doOnError(Throwable::printStackTrace)
                .blockingSubscribe();
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
            database.update("INSERT INTO Log(County_ID, Date) VALUES(?, ?)")
                    .parameterListStream(cartesianProduct)
                    .counts()
                    .onBackpressureBuffer()
                    .doOnError(Throwable::printStackTrace)
                    .blockingSubscribe();
    }

    private static void populateGovernmentDataTable(Database database) throws IOException {

            // load county border data
            final FileDataStore countyBorderDefs = FileDataStoreFinder.getDataStore(
                    PopulateDatabase.class.getResource("/edu/dbgroup/dbutils/data/counties/tl_2018_us_county.shp")
            );

            final CSVReaderBuilder builder = new CSVReaderBuilder(new FileReader(PopulateDatabase.class.getResource(
                    "/edu/dbgroup/dbutils/1-1-2018_6-30-2018-ks.csv"
            ).getFile()));

            builder.withCSVParser(getDataParser());

            final CSVReader csvReader = builder.build();

            // all rows of data taken from the read csv file
            final Flowable<GovDataRow> governmentData =
                    Flowable.fromIterable(csvReader).map(GovDataRow::createFromArray);

            // sort gov data into groups based on their county which is determined by their latitude/longitude
            final Flowable<GroupedFlowable<Integer, GovDataRow>> govDataGroupedByCounty =
                    governmentData.groupBy(govData -> getCountyByCoordinate(
                            new Coordinate(govData.longitude, govData.latitude), countyBorderDefs
                    ));

            final Flowable<Tuple2<Integer, GovDataRow>> flattenedGovData = govDataGroupedByCounty
                    .flatMap(grouped -> grouped.map(govDataRow -> Tuple2.create(grouped.getKey(), govDataRow)));

            // Precipitation
            final Flowable<List<?>> flatPrecipitation =
                    flattenedGovData.map(pair -> list(pair.value2().precipitation, pair.value2().snow));

            final Flowable<Integer> precipKeys = database.update("INSERT INTO Precipitation(Water, Snow) VALUES(?,?)")
                    .parameterListStream(flatPrecipitation)
                    .returnGeneratedKeys()
                    .getAs(Integer.class);


            // Temperature
            final Flowable<List<?>> flatTemperature =
                    flattenedGovData.map(pair -> Lists.newArrayList(pair.value2().tavg, pair.value2().tmin, pair.value2().tmax));

            final Flowable<Integer> tempKeys = database.update("INSERT INTO Temperature(Average, Low, High) VALUES(?,?,?)")
                    .parameterListStream(flatTemperature)
                    .returnGeneratedKeys()
                    .getAs(Integer.class);

            // Weather todo

            final Flowable<List<?>> flatLog =
                    flattenedGovData.map(pair -> list(pair.value1(), pair.value2().date));

            final Flowable<Integer> logKeys = database
                    .select("SELECT Log_ID FROM Log WHERE (County_ID = ?) AND (Date = ?)")
                    .parameterListStream(flatLog)
                    .getAs(Integer.class);

            final Flowable<List<?>> zippedKeys = logKeys
                    .zipWith(tempKeys, Tuple2::create)
                    .zipWith(precipKeys, (pair, precip) -> list(pair.value1(), pair.value2(), precip));

            database.update("INSERT INTO GovernmentData(Log_ID, Temperature_ID, Precipitation_ID) VALUES(?,?,?)")
                    .parameterListStream(zippedKeys)
                    .counts()
                    .onBackpressureBuffer()
                    .doOnError(Throwable::printStackTrace)
                    .blockingSubscribe();
    }

    private static ICSVParser getDataParser() {
        return new RFC4180Parser();
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

    /**
     * Helper class which stores a row of government data (from csv) as a class with all elements named.
     */
    private static class GovDataRow {

        final String station, name, /*latitude, longitude,*/ elevation, /*date,*/ /*precipitation, snow, tavg, tmax, tmin,*/
            wt01, wt02, wt03, wt04, wt05, wt06, wt07, wt08, wt09, wt11, wt15;

        final Double latitude, longitude, precipitation, snow;
        final Integer tavg, tmax, tmin;

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
            this.tavg = parseIntegerOrReturnNull(tavg);
            this.tmax = parseIntegerOrReturnNull(tmax);
            this.tmin = parseIntegerOrReturnNull(tmin);
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
        Integer parseIntegerOrReturnNull(String potentialDouble) {
            try {
                return Integer.parseInt(potentialDouble);
            } catch (Exception e) {
                return null;
            }
        }

        static GovDataRow createFromArray(String[] strings) {
//            try {
//                Preconditions.checkArgument(strings.length == 22);
//            } catch (Exception e) {
//                e.printStackTrace();
//                System.err.println(strings.length + ", " + Arrays.toString(strings));
//            }

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
