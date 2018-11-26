package edu.dbgroup.logic;

import edu.dbgroup.logic.models.Models;
import edu.dbgroup.logic.queries.Queries;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Singleton class which contains instances of the GUI's local model (not to be confused with the databases model).
 *
 * This GUI can use these models to "bind" values. In other words, when a bound value changes in the Model, the GUI
 * automatically updates to reflect this change without any additional calls.
 */
public enum ServiceProvider {

    INSTANCE;

    private final static Logger logger = LoggerFactory.getLogger(ServiceProvider.class);

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

    @Nullable
    private Database database = null;

    public final Models MODELS = new Models();
    public final Queries QUERIES = new Queries();

    @NonNull
    public final Database connectToDatabase() {
        if (database == null) {
            database = Database
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

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (database != null) {
                    database.close();
                }
            }));
        }
        return database;
    }

}
