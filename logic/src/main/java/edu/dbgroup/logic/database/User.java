package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;

import java.sql.Timestamp;

public interface User {

    @Column("User_ID")
    Integer userID();

    @Column("FirstName")
    String firstName();

    @Column("LastName")
    String lastName();

    @Column("Username")
    String username();

    @Column("CreatedOn")
    Timestamp createdOn();

    @Column("UpdatedOn")
    Timestamp updatedOn();

}
