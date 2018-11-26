package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;
import org.davidmoten.rx.jdbc.annotations.Query;

@Query("SELECT Temperature_ID, Average, High, Low FROM Temperature")
public interface Temperature {

    @Column("Temperature_ID")
    Integer temperatureID();

    @Column("Average")
    Double average();

    @Column("High")
    Double high();

    @Column("Low")
    Double low();

}
