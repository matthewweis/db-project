package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

@Query("SELECT PhysioRegion_ID, Name FROM PhysioRegion")
public interface PhysioRegion {

    @Column("PhysioRegion_ID")
    Integer physioRegionID();

    @Column("Name")
    String name();

}
