package edu.dbgroup.gui.components;

import javafx.beans.property.SimpleStringProperty;

/**
 * Simple class which holds two {@link javafx.beans.property.StringProperty}s. These properties are displayed in a
 * {@link javafx.scene.control.TableView} in the {@link HomeView}.
 */
public class TableRow {

    private final SimpleStringProperty property = new SimpleStringProperty("");

    private final SimpleStringProperty value = new SimpleStringProperty("");

    public TableRow() { }

    public TableRow(String property, String value) {
        setProperty(property);
        setValue(value);
    }

    public String getProperty() {
        return property.get();
    }

    public SimpleStringProperty propertyProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property.set(property);
    }

    public String getValue() {
        return value.get();
    }

    public SimpleStringProperty valueProperty() {
        return value;
    }

    public void setValue(String value) {
        this.value.set(value);
    }

}
