package edu.dbgroup.logic.queries;

import edu.dbgroup.logic.ServiceProvider;
import edu.dbgroup.logic.database.*;
import io.reactivex.Flowable;
import io.reactivex.flowables.GroupedFlowable;
import org.davidmoten.rx.jdbc.Database;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;

public class KansasMapQueries {

    public Double averageAll(int county_ID, Date startDate, Date endDate) {
        return ServiceProvider.INSTANCE.connectToDatabase().apply(connection -> {
            CallableStatement stmt = connection.prepareCall("{call AveragesAll(?, ?, ?)}");
            stmt.setInt(1, county_ID);
            stmt.setDate(2, startDate);
            stmt.setDate(3, endDate);
            final ResultSet resultSet = stmt.executeQuery();
            return resultSet.getDouble(1);
        }).blockingGet();
    }

    public Flowable<GroupedFlowable<County, Flowable<GovernmentData>>> queryGovernmentData(LocalDate date) {
        final Database database = ServiceProvider.INSTANCE.connectToDatabase();

        final Flowable<Log> logs =
                database.select("SELECT Log_ID, County_ID, Date FROM Log WHERE Date = ?")
                        .parameter(Date.valueOf(date))
                .getAs(Log.class);
//                .map(tuple -> createLog(tuple.value1(), tuple.value2(), tuple.value3()));

        return logs.groupBy(
                log -> database.select("SELECT County_ID, Name FROM County WHERE County_ID = ?")
                .parameter(log.countyID())
                .dependsOn(logs)
                .getAs(County.class)
                .singleOrError()
//                .map(tuple -> Queries.countyOf(tuple.value1(), tuple.value2()))
                .blockingGet(),

                log -> database.select("SELECT GovernmentData_ID, Log_ID, Temperature_ID, Precipitation_ID, " +
                /*"WeatherType_ID,*/ "CreatedOn, UpdatedOn FROM GovernmentData " +
                "WHERE Log_ID = ?")
                .parameter(log.logID())
                .dependsOn(logs)
                .autoMap(GovernmentData.class));
//                .getAs(Integer.class, Integer.class, Integer.class,
//                        Integer.class, Timestamp.class, Timestamp.class)
//                .map(tuple -> Queries.governmentDataOf(tuple.value1(), tuple.value2(),
//                        tuple.value3(), tuple.value4(), tuple.value5(), tuple.value6()))
//);
    }

    public Precipitation queryPrecipitation(GovernmentData govData) {
//        ServiceProvider.INSTANCE.connectToDatabase().apply(con -> {
//            // call proc
//        });
        return ServiceProvider.INSTANCE.connectToDatabase()
                .select("SELECT Precipitation_ID, Water, Snow FROM Precipitation WHERE Precipitation_ID = ?")
                .parameter(govData.precipitationID())
                .getAs(Integer.class, Double.class, Double.class)
                .singleOrError()
                .map(precip -> Queries.precipitationOf(precip.value1(), precip.value2(), precip.value3()))
                .blockingGet();
    }

    public Temperature queryTemperature(GovernmentData govData) {
        return ServiceProvider.INSTANCE.connectToDatabase()
                .select("SELECT Temperature_ID, Average, High, Low FROM Temperature WHERE Temperature_ID = ?")
                .parameter(govData.temperatureID())
                .getAs(Integer.class, Double.class, Double.class, Double.class)
                .singleOrError()
                .map(temp -> Queries.temperatureOf(temp.value1(), temp.value2(), temp.value3(), temp.value4()))
                .blockingGet();
    }

}
