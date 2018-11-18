package edu.dbgroup.gui.logic;

import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.annotations.Column;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Using a local database designed for testing, this test shows how data can be queried from said database.
 *
 * I am currently using rxjava2-jdbc for handling connections to the database. This probably looks confusing but don't
 * worry because I will handle everything as when it comes to connecting the database to the application. This setup
 * allows database transactions to occur through non-blocking event-based streams which basically means the
 * application's GUI doesn't have to freeze every time a GET query is requested. (This could be done without a library
 * but rxjava2-jdbc makes it easy).
 *
 * See: https://github.com/davidmoten/rxjava2-jdbc#readme
 */
public class DBQueryTest {

    private final static Logger logger = LoggerFactory.getLogger(DBQueryTest.class);

    /*
     * These examples are almost entirely from https://github.com/davidmoten/rxjava2-jdbc#readme
     *
     * These are meant to give a general idea of the syntax, not give particular examples for the weather db.
     */
    public static void main(String[] args) throws SQLException {

        logger.info("testing basic get...");
        testBasicGet();
        logger.info("success!");

        logger.info("testing automap...");
        testAutoMap();
        logger.info("success...");
    }

    // adapted from rxjava2-jdbc#readme
    private static void testBasicGet() {
        try (final Database db = Database.test()) {
            db.select("select name, score from person")
                    .getAs(String.class, Integer.class)
                    .blockingForEach(tuple -> logger.info(tuple.toString()));
        }
    }

    // adapted from rxjava2-jdbc#readme
    private static void testAutoMap() {
        final Database db = Database.test();
        db.select("SELECT name, score FROM person ORDER BY name")
                .autoMap(Person.class)
                .doOnNext(p -> logger.debug(
                        String.format("name: %s, score: %d", p.name(), p.score())
                ))
                .firstOrError()
                .map(Person::score)
                .test()
                .assertValue(21)
                .assertComplete();
    }


    // from rxjava2-jdbc#readme
    private interface Person {
        @Column("name") // "name" arg is good for when interface method doesn't match column
        String name();

        @Column("score")
        int score();
    }

}
