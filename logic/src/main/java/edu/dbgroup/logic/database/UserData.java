package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;

import java.sql.Timestamp;

public interface UserData {

    @Column("UserData_ID")
    Integer userDataID();

    @Column("User_ID")
    Integer userID();

    @Column("Log_ID")
    Integer logID();

    @Column("Temperature_ID")
    Integer temperatureID();

    @Column("Precipitation_ID")
    Integer precipitationID();

    @Column("CreatedOn")
    Timestamp createdOn();

    @Column("UpdatedOn")
    Timestamp updatedOn();

}
