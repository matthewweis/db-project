package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

import java.sql.Timestamp;

@Query("SELECT GovernmentData_ID, WeatherType_ID FROM GovernmentWeatherType")
public interface GovernmentWeatherType {

    @Column("GovernmentData_ID")
    Integer governmentDataID();

    @Column("WeatherType_ID")
    Integer weatherTypeID();

}
