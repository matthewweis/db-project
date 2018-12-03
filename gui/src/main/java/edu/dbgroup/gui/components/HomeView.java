package edu.dbgroup.gui.components;

import com.google.common.collect.Lists;
import com.sun.javafx.collections.ImmutableObservableList;
import edu.dbgroup.logic.ServiceProvider;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.sources.Change;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableRow;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Home view (a.k.a. application's home screen) which shows all the base info on a shallow level. Clicking different
 * components can lead the users to different views. Alternatively, some components (like map) can zoom in without
 * leaving the home screen.
 *
 * See resource: /edu/dbgroup/gui/components/home_view.fxml
 */
public class HomeView extends VBox {

    private final static Logger logger = LoggerFactory.getLogger(HomeView.class);

    private final Observable<Change<String>> countyChangedObservable =
            JavaFxObservable.changesOf(ServiceProvider.INSTANCE.MODELS.getKansasMapModel().selectedCountyProperty());

    @FXML
    private DatePicker datePicker;

    @FXML
    private StringPropertyPair countyTablePropertyPair;

    @FXML
    private StringPropertyPair dateTablePropertyPair;

    @FXML
    private void initialize() {
        initPropertyBindings();
    }

    private void initPropertyBindings() {

        countyTablePropertyPair.valProperty()
            .bind(ServiceProvider.INSTANCE.MODELS.getKansasMapModel().selectedCountyProperty());

        dateTablePropertyPair.valProperty()
                .bind(ServiceProvider.INSTANCE.MODELS.getHomeViewModel().selectedDateProperty().asString());

        ServiceProvider.INSTANCE.MODELS.getHomeViewModel().selectedDateProperty().bind(datePicker.valueProperty());
    }

    @FXML
    private void createPopupChart() {
        new GraphSetupForm();
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
//                Flowable.fromIterable(x.getCategories()).zipWith(
//                        Flowable.fromIterable(x.getCategories()).map(v -> random.nextDouble() * 110.0),
//                        XYChart.Data::new
//                ).toList().blockingGet().toArray(new XYChart.Data[0]));
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
    }

//    private CategoryAxis getAxisBetweenDates(LocalDate firstDate, LocalDate secondDate, LocalDate endDateExcl,
//                                             ChronoUnit units) {
//
//            final long stride = firstDate.until(secondDate, units);
//            final List<String> dates = Lists.newArrayList();
//
//            LocalDate dateIter = firstDate;
//            while (dateIter.isBefore(endDateExcl)) {
//                dates.add(dateIter.toString());
//                dateIter = dateIter.plus(stride, units);
//            }
//
//            ObservableList<String> observableList = new ImmutableObservableList<>(dates.toArray(new String[0]));
//            return new CategoryAxis(observableList);
//    }

    private CategoryAxis getTestDataBetweemDates(LocalDate firstDate, LocalDate secondDate, LocalDate endDateExcl,
                                             ChronoUnit units) {

        final long stride = firstDate.until(secondDate).get(units);
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
