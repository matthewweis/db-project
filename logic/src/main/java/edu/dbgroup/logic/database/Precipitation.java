package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

@Query("SELECT PhysioRegion_ID, Water, Snow FROM Precipitation")
public interface Precipitation {

    @Column("Precipitation_ID")
    Integer precipitationID();

    @Column("Water")
    Double water(); // use Double instead of double since the column is Nullable

    @Column("Snow")
    Double snow();

}
