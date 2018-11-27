package edu.dbgroup.logic.queries;

import edu.dbgroup.logic.database.County;
import edu.dbgroup.logic.database.GovernmentData;
import edu.dbgroup.logic.database.Precipitation;
import edu.dbgroup.logic.database.Temperature;
import io.reactivex.annotations.NonNull;

import java.sql.Timestamp;

public final class Queries {

    private final KansasMapQueries kansasMapQueries = new KansasMapQueries();

    public KansasMapQueries getKansasMapQueries() {
        return kansasMapQueries;
    }

    static County countyOf(@NonNull Integer countyID, @NonNull String name) {
        return new County() {
            @Override
            public Integer countyID() {
                return countyID;
            }

            @Override
            public String name() {
                return name;
            }
        };
    }

    static GovernmentData governmentDataOf(@NonNull Integer governmentDataID, @NonNull Integer logID,
                                           @NonNull Integer temperatureID, @NonNull Integer precipitationID,
                                           /*@NonNull Integer weatherTypeID,*/ @NonNull Timestamp createdOn,
                                           @NonNull Timestamp updatedOn) {
        return new GovernmentData() {
            @Override
            public Integer governmentDataID() {
                return governmentDataID;
            }

            @Override
            public Integer logID() {
                return logID;
            }

            @Override
            public Integer temperatureID() {
                return temperatureID;
            }

            @Override
            public Integer precipitationID() {
                return precipitationID;
            }

//            @Override
//            public Integer weatherTypeID() {
//                return weatherTypeID;
//            }

            @Override
            public Timestamp createdOn() {
                return createdOn;
            }

            @Override
            public Timestamp updatedOn() {
                return updatedOn;
            }
        };
    }

    static Precipitation precipitationOf(@NonNull Integer precipitationID, @NonNull Double water, @NonNull Double snow) {
        return new Precipitation() {
            @Override
            public Integer precipitationID() {
                return precipitationID;
            }

            @Override
            public Double water() {
                return water;
            }

            @Override
            public Double snow() {
                return snow;
            }
        };
    }

    static Temperature temperatureOf(@NonNull Integer temperatureID, @NonNull Double average,
                                     @NonNull Double high, @NonNull Double low) {
        return new Temperature() {
            @Override
            public Integer temperatureID() {
                return temperatureID;
            }

            @Override
            public Double average() {
                return average;
            }

            @Override
            public Double high() {
                return high;
            }

            @Override
            public Double low() {
                return low;
            }
        };
    }

}
