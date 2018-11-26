package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

@Query("SELECT County_ID, Name FROM County")
public interface County {

    @Column("County_ID")
    Integer countyID();

//    @Column("Log_ID")
//    Integer logID();

    // this was listed in database diagram but may have been a mistake see CountyPhysioRegion table
//    @Column("PhysioRegion_ID")
//    Integer physioRegionID();

    @Column("Name")
    String name();

}
