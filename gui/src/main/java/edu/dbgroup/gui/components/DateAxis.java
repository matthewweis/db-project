package edu.dbgroup.gui.components;

import com.google.common.collect.Lists;
import com.sun.javafx.collections.ImmutableObservableList;
import io.reactivex.Flowable;
import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DateAxis extends Axis<String> {

    private CategoryAxis delegate;

    public DateAxis(LocalDate firstDate, LocalDate secondDate, LocalDate endDateExcl, ChronoUnit units) {

        final long stride = firstDate.until(secondDate).get(units);

        final List<String> dates = Lists.newArrayList();

        LocalDate dateIter = firstDate;

        while (dateIter.isBefore(endDateExcl)) {
            dates.add(dateIter.toString());
            dateIter = dateIter.plus(stride, units);
        }

        ObservableList<String> observableList = new ImmutableObservableList<String>(dates.toArray(new String[0]));
        delegate = new CategoryAxis(observableList);
    }


    @Override
    protected Object autoRange(double length) {
        return delegate.autoRangingProperty().getBean();
    }

    @Override
    protected void setRange(Object range, boolean animate) {
//        delegate.set
    }

    @Override
    protected Object getRange() {
        return null;
    }

    @Override
    public double getZeroPosition() {
        return 0;
    }

    @Override
    public double getDisplayPosition(String value) {
        return 0;
    }

    @Override
    public String getValueForDisplay(double displayPosition) {
        return null;
    }

    @Override
    public boolean isValueOnAxis(String value) {
        return false;
    }

    @Override
    public double toNumericValue(String value) {
        return 0;
    }

    @Override
    public String toRealValue(double value) {
        return null;
    }

    @Override
    protected List<String> calculateTickValues(double length, Object range) {
        return null;
    }

    @Override
    protected String getTickMarkLabel(String value) {
        return null;
    }
}
