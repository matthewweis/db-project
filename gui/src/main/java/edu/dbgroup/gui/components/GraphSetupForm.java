package edu.dbgroup.gui.components;

import com.google.common.collect.Lists;
import com.sun.javafx.collections.ImmutableObservableList;
import com.sun.javafx.collections.ObservableListWrapper;
import edu.dbgroup.logic.ServiceProvider;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.ListSelectionView;
import org.controlsfx.control.SegmentedButton;
import org.davidmoten.rx.jdbc.tuple.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class GraphSetupForm extends Stage {

    private final static Logger logger = LoggerFactory.getLogger(GraphSetupForm.class);

    private final ListSelectionView<String> countyListSelectionView;

    private final ToggleButton rain = new ToggleButton("Rain");
    private final ToggleButton snow = new ToggleButton("Snow");
    private final ToggleButton temp = new ToggleButton("Temperature");
    private final SegmentedButton dataTypeSegButton = new SegmentedButton(rain, snow, temp);
    {
        dataTypeSegButton.getButtons().get(0).setSelected(true);
    }

    private final ToggleButton day = new ToggleButton("Day");
    private final ToggleButton week = new ToggleButton("Week");
    private final ToggleButton month = new ToggleButton("Month");
    private final ToggleButton year = new ToggleButton("Year");
    {
        day.setUserData(ChronoUnit.DAYS);
        week.setUserData(ChronoUnit.WEEKS);
        month.setUserData(ChronoUnit.MONTHS);
        year.setUserData(ChronoUnit.YEARS);
    }
    private final SegmentedButton resolutionToggleButton = new SegmentedButton(day, week, month, year);
    {
        resolutionToggleButton.getButtons().get(0).setSelected(true);
    }

    final Spinner<Integer> valSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999));
    final ComboBox<ChronoUnit> unitSpinner = new ComboBox<>(FXCollections.observableArrayList(
            ChronoUnit.DAYS, ChronoUnit.WEEKS, ChronoUnit.MONTHS, ChronoUnit.YEARS
    ));
    {
        unitSpinner.setValue(ChronoUnit.DAYS);
    }

    private final DatePicker startDate = new DatePicker(LocalDate.of(2000, 1, 1));
    private final DatePicker endDate = new DatePicker(LocalDate.of(2017, 12, 30));

    public GraphSetupForm() {
        setTitle("Graph Options");

        final VBox vbox = new VBox();

        countyListSelectionView = new ListSelectionView<>();
        countyListSelectionView.setSourceItems(new ObservableListWrapper<>(new ArrayList<>(ServiceProvider.INSTANCE.getCountiesAsList())));

        vbox.getChildren().add(countyListSelectionView);
        vbox.getChildren().add(createSeperator());

//        Button selectedDataType = new Button("Choose Data Type");
//        PopOver popOver = new PopOver(dataTypeSegButton);
//        selectedDataType.setOnAction(e -> popOver.show(selectedDataType));
//        popOver.setOnHiding(e -> selectedDataType.setText(popOver.getTitle()));
        vbox.getChildren().add(dataTypeSegButton);
        vbox.getChildren().add(createSeperator());

        vbox.getChildren().add(createResTglBtn());
        vbox.getChildren().add(createSeperator());

        vbox.getChildren().add(createDateSelectingHbox());
        vbox.getChildren().add(createSeperator());

        vbox.getChildren().add(createPeriodSelectingHbox());
        vbox.getChildren().add(createSeperator());

        final Button submitButton = new Button("Create");
        submitButton.setOnAction(e -> actuallyLaunch());
        vbox.getChildren().add(submitButton);


        final Scene scene = new Scene(vbox, 800, 600);
        setScene(scene);
        show();
    }

    private void createGraphOrErrorDialog() {
//        final boolean atLeastOneCounty = countyListSelectionView.getTargetItems().isEmpty();

        actuallyLaunch();
    }

    private Separator createSeperator() {
        final Separator separator = new Separator(Orientation.HORIZONTAL);
        separator.setPadding(new Insets(10, 0, 10, 0));
        return separator;
    }

    private HBox createResTglBtn() {
        final HBox hbox = new HBox();
        hbox.getChildren().add(new Label("Units: "));
        hbox.getChildren().add(resolutionToggleButton);
        return hbox;
    }

    private HBox createDateSelectingHbox() {
        final HBox dateHBox = new HBox(); // for dates
        dateHBox.getChildren().add(new Label("Start Date: "));
        dateHBox.getChildren().add(startDate);
        dateHBox.getChildren().add(new Label(" "));
        dateHBox.getChildren().add(new Label("End Date: "));
        dateHBox.getChildren().add(endDate);
        return dateHBox;
    }

    private HBox createPeriodSelectingHbox() {
        final HBox periodHbox = new HBox(); // for dates
        periodHbox.getChildren().add(new Label("Count by "));

        periodHbox.getChildren().add(valSpinner);
        periodHbox.getChildren().add(new Label(" "));
        periodHbox.getChildren().add(unitSpinner);
        return periodHbox;
    }

    private PopupGraphView<String, Number> actuallyLaunch()  {
        final LocalDate startDate = this.startDate.getValue();
        final LocalDate endDate = this.endDate.getValue();

        final CategoryAxis x = getAxisBetweenDates(startDate, startDate.plus(valSpinner.getValue().longValue(),
                unitSpinner.getValue()), endDate,
                ((ChronoUnit)resolutionToggleButton.getButtons().stream()
                        .filter(ToggleButton::isSelected).findFirst().get().getUserData()));

        final List<String> counties = new ArrayList<>(countyListSelectionView.getTargetItems());

        final List<XYChart.Series<String, Number>> allSeries = new ArrayList<>();


        Double biggest = null;
        Double smallest = null;
        for (String county : counties) {

            List<XYChart.Data<String, Number>> list = new ArrayList<>();

            for (String date : x.getCategories()) {
                Double data = getData(county, date);
                if (data == null) {
                    data = 0.0;
                }
                if (biggest == null) {
                    biggest = data;
                }
                if (smallest == null) {
                    smallest = data;
                }

                if (data > biggest) {
                    biggest = data;
                } else if (data < smallest) {
                    smallest = data;
                }

                list.add(new XYChart.Data<>(date, data));
            }

            final XYChart.Series<String, Number> series = new XYChart.Series<>(new ObservableListWrapper<>(list));
            series.setName(county);
            allSeries.add(series);

        }


        final NumberAxis y = new NumberAxis(smallest, biggest, (int)((biggest - smallest) / 25));

//        x.setAccessibleText(getXText());
//        y.setAccessibleText(getYText());

        final PopupGraphView<String, Number> graph = new PopupGraphView<>(x, y, getCategory() + " Graph", startDate + " to " + endDate, allSeries);

        return graph;
    }

    private String getCategory() {
        return dataTypeSegButton.getButtons().stream().filter(ToggleButton::isSelected).findFirst().get().getText();
    }

    private String getXText() {
        return resolutionToggleButton.getButtons().stream().filter(ToggleButton::isSelected).findFirst().get().getText();
//        return valSpinner.getValue() + " " + unitSpinner.getValue();
    }

    private String getYText() {
        return "";
    }

    private Double getData(String county, String dateString) {
        final LocalDate localDate = LocalDate.parse(dateString);
        if (rain.isSelected()) {
                try {
                    return ServiceProvider.INSTANCE.QUERIES.averagePrecip(county, Date.valueOf(localDate),
                            Date.valueOf(localDate.plus(valSpinner.getValue().longValue(), unitSpinner.getValue())));
                } catch (SQLException e) { // todo remove exception swallow
                    return 0.0;
                }
        } else if (snow.isSelected()) {
                try {
                    return ServiceProvider.INSTANCE.QUERIES.averageSnow(county, Date.valueOf(localDate),
                            Date.valueOf(localDate.plus(valSpinner.getValue().longValue(), unitSpinner.getValue())));
                } catch (Exception e) { // todo remove exception swallow
                    return 0.0;
                }
        } else { // otherwise: temp
                try {
                    Tuple3<Integer, Integer, Integer> tuple3 =
                            ServiceProvider.INSTANCE.QUERIES.averageTemp(county, Date.valueOf(localDate),
                                    Date.valueOf(localDate.plus(valSpinner.getValue().longValue(), unitSpinner.getValue())));
                    return (double) ( tuple3._3() - tuple3._2());
                } catch (Exception e) { // todo remove exception swallow
                    return 0.0;
                }
        }
    }

//    private void launch() {
//        final LocalDate start = LocalDate.of(2018, 1, 1);
//        final CategoryAxis x =
//                getAxisBetweenDates(start, start.plusWeeks(1), start.plusMonths(6), ChronoUnit.WEEKS);
//
//        final NumberAxis y = new NumberAxis(0.0, 110.0, 5.0);
//        final Random random = new Random();
//
//
//        final ObservableList<XYChart.Data<String, Number>> fakeData1 =
//                new ImmutableObservableList<XYChart.Data<String, Number>>(
//                        Flowable.fromIterable(x.getCategories()).zipWith(
//                                Flowable.fromIterable(x.getCategories()).map(v -> random.nextDouble() * 110.0),
//                                XYChart.Data::new
//                        ).toList().blockingGet().toArray(new XYChart.Data[0]));
//
//        final ObservableList<XYChart.Data<String, Number>> fakeData2 =
//                new ImmutableObservableList<XYChart.Data<String, Number>>(
//                        Flowable.fromIterable(x.getCategories()).zipWith(
//                                Flowable.fromIterable(x.getCategories()).map(v -> 10 + random.nextDouble() * 40.0),
//                                XYChart.Data::new
//                        ).toList().blockingGet().toArray(new XYChart.Data[0]));
//
//        final ObservableList<XYChart.Data<String, Number>> fakeData3 =
//                new ImmutableObservableList<XYChart.Data<String, Number>>(
//                        Flowable.fromIterable(x.getCategories()).zipWith(
//                                Flowable.fromIterable(x.getCategories()).map(v -> 20 + random.nextDouble() * 90.0),
//                                XYChart.Data::new
//                        ).toList().blockingGet().toArray(new XYChart.Data[0]));
//
//        XYChart.Series<String, Number> series1 = new XYChart.Series<String, Number>(fakeData1);
//        XYChart.Series<String, Number> series2 = new XYChart.Series<String, Number>(fakeData2);
//        XYChart.Series<String, Number> series3 = new XYChart.Series<String, Number>(fakeData3);
//
//        series1.setName("county1");
//        series2.setName("county2");
//        series3.setName("county3");
//
//        final PopupGraphView<String, Number> graph = new PopupGraphView<>(x, y, "test graph", "test window", series1,
//                series2, series3);
//
//
//    }

    private CategoryAxis getAxisBetweenDates(LocalDate firstDate, LocalDate secondDate, LocalDate endDateExcl,
                                                 ChronoUnit units) {

        final long stride = firstDate.until(secondDate, units);
        final List<String> dates = Lists.newArrayList();

        LocalDate dateIter = firstDate;
        while (dateIter.isBefore(endDateExcl)) {
            dates.add(dateIter.toString());
            dateIter = dateIter.plus(stride, units);
        }

        ObservableList<String> observableList = new ImmutableObservableList<>(dates.toArray(new String[0]));
        return new CategoryAxis(observableList);
    }

}
