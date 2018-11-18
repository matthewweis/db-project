package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;

public interface UserWeatherType {

    @Column("UserData_ID")
    Integer userDataID();

    @Column("WeatherType_ID")
    Integer weatherTypeID();

}
