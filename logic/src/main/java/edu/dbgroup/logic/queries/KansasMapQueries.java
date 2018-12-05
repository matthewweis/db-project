package edu.dbgroup.logic.queries;

import ch.qos.logback.core.db.dialect.DBUtil;
import edu.dbgroup.logic.ServiceProvider;
import edu.dbgroup.logic.database.*;
import io.reactivex.Flowable;
import io.reactivex.flowables.GroupedFlowable;
import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.tuple.Tuple3;
import org.davidmoten.rx.jdbc.tuple.Tuple5;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.LocalDate;

public class KansasMapQueries {

//    public static void main(String[] args) throws SQLException {
//        System.out.println("\n\n\n\n\n\n" + ServiceProvider.INSTANCE.QUERIES.getKansasMapQueries().averageAll("Johnson",
//                Date.valueOf(LocalDate.of(2016, 1, 1)),
//                Date.valueOf(LocalDate.of(2016, 6, 1))
//        ));
//    }

    public @Nullable Tuple5<Double, Double, Integer, Integer, Integer> averageAll(String countyName, Date startDate, Date endDate) throws SQLException {
        final ResultSet rs = ServiceProvider.INSTANCE.connectToDatabase().apply(connection -> {
            CallableStatement stmt = connection.prepareCall("{call AveragesAll(?, ?, ?)}");
            stmt.setString(1, countyName);
            stmt.setDate(2, startDate);
            stmt.setDate(3, endDate);
            final ResultSet resultSet = stmt.executeQuery();
            return resultSet;
        }).blockingGet();

        while (rs.next()) {
            final Double rainFall = rs.getDouble(1);
            final Double snowFall = rs.getDouble(2);
            final Integer avgTemp = rs.getInt(3);
            final Integer avgHiTemp = rs.getInt(4);
            final Integer avgLoTemp = rs.getInt(5);
            return Tuple5.create(rainFall, snowFall, avgTemp, avgHiTemp, avgLoTemp);
        }
        return null;
    }

    public @Nullable Tuple3<Integer, Integer, Integer> averageTemp(String countyName, Date startDate, Date endDate) throws SQLException {
        final ResultSet rs = ServiceProvider.INSTANCE.connectToDatabase().apply(connection -> {
            CallableStatement stmt = connection.prepareCall("{call AverageTemp(?, ?, ?)}");
            stmt.setString(1, countyName);
            stmt.setDate(2, startDate);
            stmt.setDate(3, endDate);
            final ResultSet resultSet = stmt.executeQuery();
            return resultSet;
        }).blockingGet();

        while (rs.next()) {
            final Integer avgTemp = rs.getInt(1);
            final Integer avgHiTemp = rs.getInt(2);
            final Integer avgLoTemp = rs.getInt(3);
            return Tuple3.create(avgTemp, avgHiTemp, avgLoTemp);
        }
        return null;
    }

    public @Nullable Double averagePrecip(String countyName, Date startDate, Date endDate) throws SQLException {
        final ResultSet rs = ServiceProvider.INSTANCE.connectToDatabase().apply(connection -> {
            CallableStatement stmt = connection.prepareCall("{call AveragePrecip(?, ?, ?)}");
            stmt.setString(1, countyName);
            stmt.setDate(2, startDate);
            stmt.setDate(3, endDate);
            final ResultSet resultSet = stmt.executeQuery();
            return resultSet;
        }).blockingGet();

        while (rs.next()) {
            final Double rainFall = rs.getDouble(1);
            return rainFall;
        }
        return null;
    }

    public @Nullable Double averageSnow(String countyName, Date startDate, Date endDate) throws SQLException {
        final ResultSet rs = ServiceProvider.INSTANCE.connectToDatabase().apply(connection -> {
            CallableStatement stmt = connection.prepareCall("{call AverageSnow(?, ?, ?)}");
            stmt.setString(1, countyName);
            stmt.setDate(2, startDate);
            stmt.setDate(3, endDate);
            final ResultSet resultSet = stmt.executeQuery();
            return resultSet;
        }).blockingGet();

        while (rs.next()) {
            final Double snowFall = rs.getDouble(1);
            return snowFall;
        }
        return null;
    }



//    public Flowable<GroupedFlowable<County, Flowable<GovernmentData>>> queryGovernmentData(LocalDate date) {
//        final Database database = ServiceProvider.INSTANCE.connectToDatabase();
//
//        final Flowable<Log> logs =
//                database.select("SELECT Log_ID, County_ID, Date FROM Log WHERE Date = ?")
//                        .parameter(Date.valueOf(date))
//                .getAs(Log.class)
//                .doOnError(Throwable::printStackTrace);
////                .map(tuple -> createLog(tuple.value1(), tuple.value2(), tuple.value3()));
//
//        return logs.groupBy(
//                log -> database.select("SELECT County_ID, Name FROM County WHERE County_ID = ?")
//                .parameter(log.countyID())
//                .dependsOn(logs)
//                .getAs(County.class)
//                .singleOrError().doOnError(Throwable::printStackTrace)
////                .map(tuple -> Queries.countyOf(tuple.value1(), tuple.value2()))
//                .blockingGet(),
//
//                log -> database.select("SELECT GovernmentData_ID, Log_ID, Temperature_ID, Precipitation_ID, " +
//                /*"WeatherType_ID,*/ "CreatedOn, UpdatedOn FROM GovernmentData " +
//                "WHERE Log_ID = ?")
//                .parameter(log.logID())
//                .dependsOn(logs)
//                .autoMap(GovernmentData.class))
//                .doOnError(Throwable::printStackTrace);
////                .getAs(Integer.class, Integer.class, Integer.class,
////                        Integer.class, Timestamp.class, Timestamp.class)
////                .map(tuple -> Queries.governmentDataOf(tuple.value1(), tuple.value2(),
////                        tuple.value3(), tuple.value4(), tuple.value5(), tuple.value6()))
////);
//    }
//
//    public Precipitation queryPrecipitation(GovernmentData govData) {
////        ServiceProvider.INSTANCE.connectToDatabase().apply(con -> {
////            // call proc
////        });
//        return ServiceProvider.INSTANCE.connectToDatabase()
//                .select("SELECT Precipitation_ID, Water, Snow FROM Precipitation WHERE Precipitation_ID = ?")
//                .parameter(govData.precipitationID())
//                .getAs(Integer.class, Double.class, Double.class)
//                .singleOrError()
//                .map(precip -> Queries.precipitationOf(precip.value1(), precip.value2(), precip.value3()))
//                .blockingGet();
//    }
//
//    public Temperature queryTemperature(GovernmentData govData) {
//        return ServiceProvider.INSTANCE.connectToDatabase()
//                .select("SELECT Temperature_ID, Average, High, Low FROM Temperature WHERE Temperature_ID = ?")
//                .parameter(govData.temperatureID())
//                .getAs(Integer.class, Double.class, Double.class, Double.class)
//                .singleOrError()
//                .map(temp -> Queries.temperatureOf(temp.value1(), temp.value2(), temp.value3(), temp.value4()))
//                .blockingGet();
//    }

}
