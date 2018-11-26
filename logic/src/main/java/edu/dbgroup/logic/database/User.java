package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

import java.sql.Timestamp;

@Query("SELECT User_ID, FirstName, LastName, Username, CreatedOn, UpdatedOn FROM User")
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
