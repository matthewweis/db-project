package edu.dbgroup.logic.models;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.LocalDate;

/**
 * Model representing data for the "home" view screen, which is the base screen where the date, map
 * (see {@link KansasMapModel}, info, etc. is all displayed to the user.
 *
 * To access an instance of this class, see {@link ServiceProvider#getHomeViewModel()} ()}.
 */
public class HomeViewModel {

    HomeViewModel() { }

    private ObjectProperty<LocalDate> selectedDate = new SimpleObjectProperty<>(LocalDate.now());

    public LocalDate getSelectedDate() {
        return selectedDate.get();
    }

    public ObjectProperty<LocalDate> selectedDateProperty() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate.set(selectedDate);
    }
}
