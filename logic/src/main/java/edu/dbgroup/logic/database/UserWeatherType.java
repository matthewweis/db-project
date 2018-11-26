package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

@Query("SELECT UserData_ID, WeatherType_ID FROM UserWeatherType")
public interface UserWeatherType {

    @Column("UserData_ID")
    Integer userDataID();

    @Column("WeatherType_ID")
    Integer weatherTypeID();

}
