package edu.dbgroup.logic.models;

import edu.dbgroup.logic.ServiceProvider;
import javafx.beans.property.*;

/**
 * Model representing data for the map of kansas which is displayed in the "home" view (see {@link HomeViewModel}).
 *
 * To access an instance of this class, see {@link ServiceProvider#MODELS} -> {@link Models#getKansasMapModel()}
 */
public final class KansasMapModel {

    KansasMapModel() { }

    private final StringProperty selectedCounty = new SimpleStringProperty("<none>");

    private final BooleanProperty isZoomed = new SimpleBooleanProperty(false);

    private final ObjectProperty<Double> rain = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Double> snow = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Integer> avgTemp = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Integer> maxTemp = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Integer> minTemp = new SimpleObjectProperty<>(null);

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

    public Double getRain() {
        return rain.get();
    }

    public ObjectProperty<Double> rainProperty() {
        return rain;
    }

    public void setRain(Double rain) {
        this.rain.set(rain);
    }

    public Double getSnow() {
        return snow.get();
    }

    public ObjectProperty<Double> snowProperty() {
        return snow;
    }

    public void setSnow(Double snow) {
        this.snow.set(snow);
    }

    public Integer getAvgTemp() {
        return avgTemp.get();
    }

    public ObjectProperty<Integer> avgTempProperty() {
        return avgTemp;
    }

    public void setAvgTemp(Integer avgTemp) {
        this.avgTemp.set(avgTemp);
    }

    public Integer getMaxTemp() {
        return maxTemp.get();
    }

    public ObjectProperty<Integer> maxTempProperty() {
        return maxTemp;
    }

    public void setMaxTemp(Integer maxTemp) {
        this.maxTemp.set(maxTemp);
    }

    public Integer getMinTemp() {
        return minTemp.get();
    }

    public ObjectProperty<Integer> minTempProperty() {
        return minTemp;
    }

    public void setMinTemp(Integer minTemp) {
        this.minTemp.set(minTemp);
    }
}
