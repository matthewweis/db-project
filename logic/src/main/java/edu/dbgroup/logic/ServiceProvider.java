package edu.dbgroup.logic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import edu.dbgroup.logic.database.County;
import edu.dbgroup.logic.database.GovernmentData;
import edu.dbgroup.logic.models.Models;
import edu.dbgroup.logic.queries.Queries;
import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
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
//    private static final String DATABASE_URL = "jdbc:jtds:sqlserver://mssql.cs.ksu.edu:1433/cis560_team15";
    private static final String DATABASE_URL = "jdbc:sqlserver://mssql.cs.ksu.edu:1433";

    private final static Properties DATABASE_PROPERTIES = new Properties();
    static {
        //DATABASE_PROPERTIES.setProperty("Server", ".");
        DATABASE_PROPERTIES.setProperty("databaseName", "cis560_team15");
//        DATABASE_PROPERTIES.setProperty("Integrated Security", "SSPI");
//        DATABASE_PROPERTIES.setProperty("authenticationScheme", "JavaKerberos");
        DATABASE_PROPERTIES.setProperty("integratedSecurity", "true");
//        DATABASE_PROPERTIES.setProperty("encrypt", "true");
//        DATABASE_PROPERTIES.setProperty("hostNameInCertificate", "*.cs.ksu.edu");
//        DATABASE_PROPERTIES.setProperty("loginTimeout", "30");
//        DATABASE_PROPERTIES.setProperty("user", "WIN2\\matthewweis");
//        DATABASE_PROPERTIES.setProperty("password", "");
    }

    /*
     * String to load driver for Microsoft sqlserver. This is only here as a contingency for legacy servers.
     */
//    private static final String JDBC_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

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
            System.out.println(java.lang.System.getProperty("java.library.path"));

//                Class.forName(JDBC_DRIVER);
//            try {
//                Class.forName("net.sourceforge.jtds.jdbc.Driver");
//                System.load("C:\\Users\\matthewweis\\Downloads\\jtds-1.3.1-dist\\x64\\SSO\\ntlmauth.dll");
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
            //-Djava.library.path="C:\Users\matthewweis\Downloads\Microsoft JDBC Driver 6.4 for SQL Server\sqljdbc_6.4\enu\auth\x64\sqljdbc_auth.dll"
//                System.load("C:\\Users\\matthewweis\\Downloads\\Microsoft JDBC Driver 6.4 for SQL Server\\sqljdbc_6.4\\enu\\auth\\x64\\sqljdbc_auth.dll");
//                System.load("U:\\IdeaProjects\\db-project\\logic\\src\\main\\resources\\x64\\sqljdbc_auth.dll");
//                System.loadLibrary("sqljdbc_auth");
//            System.out.flush();
//            System.err.flush();
//            System.out.println("ASDJ");
//            System.err.println("ASDJ");
//            System.out.flush();
//            System.err.flush();
//                System.load("U:/sqljdbc_auth.dll");
//            System.loadLibrary("sqljdbc_auth");

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

    private final List<String> counties = ImmutableList.<String>builder().add(
            "Allen", "Anderson", "Atchison", "Barber", "Barton",
            "Bourbon", "Brown", "Butler", "Chase", "Chautauqua", "Cherokee", "Cheyenne", "Clark", "Clay", "Cloud",
            "Coffey", "Comanche", "Cowley", "Crawford", "Decatur", "Dickinson", "Doniphan", "Douglas", "Edwards",
            "Elk", "Ellis", "Ellsworth", "Finney", "Ford", "Franklin", "Geary", "Gove", "Graham", "Grant", "Gray",
            "Greeley", "Greenwood", "Hamilton", "Harper", "Harvey", "Haskell", "Hodgeman", "Jackson", "Jefferson",
            "Jewell", "Johnson", "Kearny", "Kingman", "Kiowa", "Labette", "Lane", "Leavenworth", "Lincoln", "Linn",
            "Logan", "Lyon", "McPherson", "Marion", "Marshall",
            "Meade", "Miami", "Mitchell", "Montgomery", "Morris", "Morton", "Nemaha", "Neosho", "Ness", "Norton",
            "Osage", "Osborne", "Ottawa", "Pawnee", "Phillips", "Pottawatomie", "Pratt", "Rawlins", "Reno",
            "Republic", "Rice", "Riley", "Rooks", "Rush", "Russell", "Saline", "Scott", "Sedgwick", "Seward",
            "Shawnee", "Sheridan", "Sherman", "Smith", "Stafford", "Stanton", "Stevens", "Sumner", "Thomas",
            "Trego", "Wabaunsee", "Wallace", "Washington", "Wichita", "Wilson", "Woodson", "Wyandotte"
    ).build();

    public List<String> getCountiesAsList() {
        return counties;
    }

    public Flowable<String> getCountiesAsFlowable() {
        return Flowable.fromIterable(counties);
    }

    public int getCountyIdByName(String countyName) {
        return (getCountiesAsList().indexOf(countyName) * 2) + 1;
    }

}
