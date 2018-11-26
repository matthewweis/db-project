package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

@Query("SELECT WeatherType_ID, Value FROM WeatherType")
public interface WeatherType {

    @Column("WeatherType_ID")
    Integer weatherTypeID();

    @Column("Value")
    String value();

}
