package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

import java.sql.Date;

@Query("SELECT Log_ID, County_ID, Date FROM Log")
public interface Log {

    @Column("Log_ID")
    Integer logID();

    @Column("County_ID")
    Integer countyID();

    @Column("Date")
    Date date();

}
