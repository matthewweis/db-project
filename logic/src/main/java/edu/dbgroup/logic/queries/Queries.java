package edu.dbgroup.logic.queries;

import edu.dbgroup.logic.ServiceProvider;
import edu.dbgroup.logic.database.County;
import edu.dbgroup.logic.database.GovernmentData;
import edu.dbgroup.logic.database.Precipitation;
import edu.dbgroup.logic.database.Temperature;
import io.reactivex.annotations.NonNull;
import org.davidmoten.rx.jdbc.tuple.Tuple3;
import org.davidmoten.rx.jdbc.tuple.Tuple5;

import javax.annotation.Nullable;
import java.sql.*;

public final class Queries {

//    private final KansasMapQueries kansasMapQueries = new KansasMapQueries();
//
//    public KansasMapQueries getKansasMapQueries() {
//        return kansasMapQueries;
//    }

    /**
     *
     * @param countyName
     * @param startDate
     * @param endDate
     * @return Tuple5 of double rainFall, double snowFall, int avgTemp, int avgHiTemp, int avgLoTemp (all nullable)
     * @throws SQLException
     */
    public @Nullable
    Tuple5<Double, Double, Integer, Integer, Integer> averageAll(String countyName, Date startDate, Date endDate) throws SQLException {
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

    /**
     *
     * @param countyName
     * @param startDate
     * @param endDate
     * @return avg, lo, hi
     * @throws SQLException
     */
    public @Nullable
    Tuple3<Integer, Integer, Integer> averageTemp(String countyName, Date startDate, Date endDate) throws SQLException {
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

    public void insertData(int id, Date date, int countyID, double precip, double snowfall, int avgTemp, int highTemp, int lowTemp, String weatherType, String desc) {
        final ResultSet rs = ServiceProvider.INSTANCE.connectToDatabase().apply(connection -> {
            CallableStatement stmt = connection.prepareCall("{call InsertData(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            stmt.setInt(1, id);
            stmt.setDate(2, date);
            stmt.setInt(3, countyID);
            stmt.setDouble(4, precip);
            stmt.setDouble(5, snowfall);
            stmt.setInt(6, avgTemp);
            stmt.setInt(7, highTemp);
            stmt.setInt(8, lowTemp);
            stmt.setString(9, weatherType);
            stmt.setString(10, desc);

            final ResultSet resultSet = stmt.executeQuery();
            return resultSet;
        }).blockingGet();
    }

    public int getUserId(String username) throws SQLException {
        final ResultSet rs = ServiceProvider.INSTANCE.connectToDatabase().apply(connection -> {
            CallableStatement stmt = connection.prepareCall("{call GetUserId(?)}");
            stmt.setString(1, username);
            final ResultSet resultSet = stmt.executeQuery();
            return resultSet;
        }).blockingGet();
        while (rs.next()) {
            final int ret = rs.getInt(1);
            return ret;
        }
        return 0;
    }

    public String getWarnings(String county, Date start, Date end) throws SQLException {
        final ResultSet rs = ServiceProvider.INSTANCE.connectToDatabase().apply(connection -> {
            CallableStatement stmt = connection.prepareCall("{call SubmittedWarnings(?, ?, ?)}");
            stmt.setString(1, county);
            stmt.setDate(2, start);
            stmt.setDate(3, end);
            final ResultSet resultSet = stmt.executeQuery();
            return resultSet;
        }).blockingGet();
        try {
            while (rs.next()) {
                final String ret = rs.getString(1);
                return ret;
            }
        } catch (Exception e) {

        }

        return "";
    }


}
