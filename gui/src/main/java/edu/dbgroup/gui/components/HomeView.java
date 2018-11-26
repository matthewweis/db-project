package edu.dbgroup.gui.components;

import edu.dbgroup.logic.ServiceProvider;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.sources.Change;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private TableRow countyTableRow;

    @FXML
    private TableRow dateTableRow;

    @FXML
    private void initialize() {
        initPropertyBindings();
    }

    private void initPropertyBindings() {
        countyTableRow.valueProperty()
                .bind(ServiceProvider.INSTANCE.MODELS.getKansasMapModel().selectedCountyProperty());

        dateTableRow.valueProperty()
                .bind(ServiceProvider.INSTANCE.MODELS.getHomeViewModel().selectedDateProperty().asString());

        ServiceProvider.INSTANCE.MODELS.getHomeViewModel().selectedDateProperty().bind(datePicker.valueProperty());
    }
}
