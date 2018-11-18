package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;

public interface Zip {

    @Column("Zip_Code")
    int zipCode();

    @Column("County_ID")
    int countyID();

}
