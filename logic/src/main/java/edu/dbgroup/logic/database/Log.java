package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;

import java.sql.Timestamp;

public interface Log {

    @Column("Log_ID")
    Integer logID();

    @Column("Name")
    Timestamp date();

}
