package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;

public interface PhysioRegion {

    @Column("PhysioRegion_ID")
    Integer physioRegionID();

    @Column("Name")
    String name();

}
