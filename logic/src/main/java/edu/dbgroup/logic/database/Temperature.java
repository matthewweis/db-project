package edu.dbgroup.logic.database;

import org.davidmoten.rx.jdbc.annotations.Column;

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
