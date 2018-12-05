package edu.dbgroup.gui.components;

import com.google.common.collect.Lists;
import com.sun.javafx.collections.ImmutableObservableList;
import com.sun.javafx.collections.ObservableListWrapper;
import edu.dbgroup.logic.ServiceProvider;
import io.reactivex.Flowable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.ListSelectionView;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.dialog.ExceptionDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GraphSetupForm extends Stage {

    private final static Logger logger = LoggerFactory.getLogger(GraphSetupForm.class);

    private final ListSelectionView<String> countyListSelectionView;

    private final ToggleButton precip = new ToggleButton("Precipitation");
    private final ToggleButton temp = new ToggleButton("Temperature");
    private final ToggleButton wt = new ToggleButton("Weather Types");
    private final SegmentedButton dataTypeSegButton = new SegmentedButton(precip, temp, wt);
    {
        dataTypeSegButton.getButtons().get(0).setSelected(true);
    }

    private final ToggleButton day = new ToggleButton("Day");
    private final ToggleButton week = new ToggleButton("Week");
    private final ToggleButton month = new ToggleButton("Month");
    private final ToggleButton year = new ToggleButton("Year");
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
    private final DatePicker endDate = new DatePicker(LocalDate.of(2017, 12, 31));

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
        submitButton.setOnAction(e -> launch());
        vbox.getChildren().add(submitButton);


        final Scene scene = new Scene(vbox, 800, 600);
        setScene(scene);
        show();
    }

    private void createGraphOrErrorDialog() {
//        final boolean atLeastOneCounty = countyListSelectionView.getTargetItems().isEmpty();

        launch();
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

    private void launch() {
        final LocalDate start = LocalDate.of(2018, 1, 1);
        final CategoryAxis x =
                getAxisBetweenDates(start, start.plusWeeks(1), start.plusMonths(6), ChronoUnit.WEEKS);

        final NumberAxis y = new NumberAxis(0.0, 110.0, 5.0);
        final Random random = new Random();


        final ObservableList<XYChart.Data<String, Number>> fakeData1 =
                new ImmutableObservableList<XYChart.Data<String, Number>>(
                        Flowable.fromIterable(x.getCategories()).zipWith(
                                Flowable.fromIterable(x.getCategories()).map(v -> random.nextDouble() * 110.0),
                                XYChart.Data::new
                        ).toList().blockingGet().toArray(new XYChart.Data[0]));

        final ObservableList<XYChart.Data<String, Number>> fakeData2 =
                new ImmutableObservableList<XYChart.Data<String, Number>>(
                        Flowable.fromIterable(x.getCategories()).zipWith(
                                Flowable.fromIterable(x.getCategories()).map(v -> 10 + random.nextDouble() * 40.0),
                                XYChart.Data::new
                        ).toList().blockingGet().toArray(new XYChart.Data[0]));

        final ObservableList<XYChart.Data<String, Number>> fakeData3 =
                new ImmutableObservableList<XYChart.Data<String, Number>>(
                        Flowable.fromIterable(x.getCategories()).zipWith(
                                Flowable.fromIterable(x.getCategories()).map(v -> 20 + random.nextDouble() * 90.0),
                                XYChart.Data::new
                        ).toList().blockingGet().toArray(new XYChart.Data[0]));

        XYChart.Series<String, Number> series1 = new XYChart.Series<String, Number>(fakeData1);
        XYChart.Series<String, Number> series2 = new XYChart.Series<String, Number>(fakeData2);
        XYChart.Series<String, Number> series3 = new XYChart.Series<String, Number>(fakeData3);

        series1.setName("county1");
        series2.setName("county2");
        series3.setName("county3");

        final PopupGraphView<String, Number> graph = new PopupGraphView<>(x, y, "test graph", "test window", series1,
                series2, series3);


    }

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
