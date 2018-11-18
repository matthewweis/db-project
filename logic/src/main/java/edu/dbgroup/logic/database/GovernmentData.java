package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;

import java.sql.Timestamp;

public interface GovernmentData {

    @Column("GovernmentData_ID")
    Integer governmentDataID();

    @Column("Log_ID")
    Integer logID();

    @Column("Temperature_ID")
    Integer temperatureID();

    @Column("Precipitation_ID")
    Integer precipitationID();

//    @Column("WeatherType_ID") // may be incorrect in diagram, instead use GovernmentWeatherType table for 0..N link?
//    Integer weatherTypeID();

    @Column("CreatedOn")
    Timestamp createdOn();

    @Column("UpdatedOn")
    Timestamp updatedOn();

}
