package edu.dbgroup.gui.components;

import com.google.common.base.Objects;
import javafx.beans.NamedArg;
import javafx.beans.property.SimpleStringProperty;

/**
 * Simple class which holds two {@link javafx.beans.property.StringProperty}s. These properties are displayed in a
 * {@link javafx.scene.control.TableView} in the {@link HomeView}.
 */
public class StringPropertyPair {

    private SimpleStringProperty name = new SimpleStringProperty();

    private SimpleStringProperty val = new SimpleStringProperty();

    public StringPropertyPair() {
        this("<none>", "<none>");
    }

    public StringPropertyPair(@NamedArg("name") String name, @NamedArg("val") String val) {
        setName(name);
        setVal(val);
    }

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getVal() {
        return val.get();
    }

    public SimpleStringProperty valProperty() {
        return val;
    }

    public void setVal(String val) {
        this.val.set(val);
    }

    @Override
    public String toString() {
        return "StringPropertyPair{" +
                "name=" + name +
                ", val=" + val +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringPropertyPair)) return false;
        StringPropertyPair that = (StringPropertyPair) o;
        return Objects.equal(getName(), that.getName()) &&
                Objects.equal(getVal(), that.getVal());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName(), getVal());
    }
}
