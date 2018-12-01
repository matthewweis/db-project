package edu.dbgroup.logic.database;

import io.reactivex.annotations.NonNull;
import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

@Query("SELECT County_ID, Name FROM County")
public interface County {

    @NonNull
    @Column("County_ID")
    Integer countyID();

//    @Column("Log_ID")
//    Integer logID();

    // this was listed in database diagram but may have been a mistake see CountyPhysioRegion table
//    @Column("PhysioRegion_ID")
//    Integer physioRegionID();

    @NonNull
    @Column("Name")
    String name();

}
