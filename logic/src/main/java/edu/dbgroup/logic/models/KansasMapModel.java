package edu.dbgroup.logic.models;

import edu.dbgroup.logic.ServiceProvider;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model representing data for the map of kansas which is displayed in the "home" view (see {@link HomeViewModel}).
 *
 * To access an instance of this class, see {@link ServiceProvider#MODELS} -> {@link Models#getKansasMapModel()}
 */
public final class KansasMapModel {

    KansasMapModel() { }

    private final StringProperty selectedCounty = new SimpleStringProperty("<none>");

    private final BooleanProperty isZoomed = new SimpleBooleanProperty(false);

    public String getSelectedCounty() {
        return selectedCounty.get();
    }

    public StringProperty selectedCountyProperty() {
        return selectedCounty;
    }

    public boolean getIsZoomed() {
        return isZoomed.get();
    }

    public BooleanProperty isZoomedProperty() {
        return isZoomed;
    }

    public void setSelectedCounty(String selectedCounty) {
        this.selectedCounty.set(selectedCounty);
    }

    public void setIsZoomed(boolean isZoomed) {
        this.isZoomed.set(isZoomed);
    }
}
