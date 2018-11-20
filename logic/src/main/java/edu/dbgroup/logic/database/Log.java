package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;

import java.sql.Date;

public interface Log {

    @Column("Log_ID")
    Integer logID();

    @Column("County_ID")
    Integer countyID();

    @Column("Date")
    Date date();

}
