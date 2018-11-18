package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;

public interface WeatherType {

    @Column("WeatherType_ID")
    Integer weatherTypeID();

    @Column("Value")
    String value();

}
