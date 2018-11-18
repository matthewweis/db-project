package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;

import java.sql.Timestamp;

public interface GovernmentWeatherType {

    @Column("GovernmentData_ID")
    Integer governmentDataID();

    @Column("WeatherType_ID")
    Integer weatherTypeID();

}
