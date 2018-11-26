package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

import java.sql.Timestamp;

@Query("SELECT Warning_ID, UserData_ID, Severity, Category, Description, CreatedOn, UpdatedOn FROM Warning")
public interface Warning {

    @Column("Warning_ID")
    Integer warningID();

    @Column("UserData_ID")
    Integer userDataID();

    @Column("Severity")
    Integer severity();

    @Column("Category")
    Integer category();

    @Column("Description")
    String description();

    @Column("CreatedOn")
    Timestamp createdOn();

    @Column("UpdatedOn")
    Timestamp updatedOn();

}
