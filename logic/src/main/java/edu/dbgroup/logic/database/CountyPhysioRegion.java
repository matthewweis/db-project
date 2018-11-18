package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;

public interface CountyPhysioRegion {

    @Column("County_ID")
    Integer countyID();

    @Column("PhysioRegion_ID")
    Integer physioRegionID();

}
