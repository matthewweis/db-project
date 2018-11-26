package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

@Query("SELECT Zip_Code, County_ID FROM Zip")
public interface Zip {

    @Column("Zip_Code")
    Integer zipCode();

    @Column("County_ID")
    Integer countyID();

}
