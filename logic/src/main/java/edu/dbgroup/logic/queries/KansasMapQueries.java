package edu.dbgroup.logic.queries;

import edu.dbgroup.logic.ServiceProvider;
import edu.dbgroup.logic.database.County;
import edu.dbgroup.logic.database.GovernmentData;
import edu.dbgroup.logic.database.Precipitation;
import edu.dbgroup.logic.database.Temperature;
import io.reactivex.Flowable;

import java.time.LocalDate;
import java.util.Map;

public class KansasMapQueries {

    public Map<County, GovernmentData> queryGovernmentData(LocalDate date) {

        final Flowable<County> counties =
                ServiceProvider.INSTANCE.connectToDatabase().select(County.class).get();

        ServiceProvider.INSTANCE.connectToDatabase()
                .select("SELECT ")

        final Flowable<GovernmentData> govData =
                ServiceProvider.INSTANCE.connectToDatabase()
                        .select(GovernmentData.class).get();

        govData.groupBy(data -> data.logID())
    }

    public Precipitation queryPrecipitation(GovernmentData govData) {

    }

    public Temperature queryTemperature(GovernmentData govData) {

    }

}
