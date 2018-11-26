package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

@Query("SELECT County_ID, PhysioRegion_ID FROM CountyPhysioRegion")
public interface CountyPhysioRegion {

    @Column("County_ID")
    Integer countyID();

    @Column("PhysioRegion_ID")
    Integer physioRegionID();

}
