package edu.dbgroup.gui;

import edu.dbgroup.gui.components.HomeView;
import edu.dbgroup.logic.ServiceProvider;
import io.reactivex.annotations.NonNull;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.LocalDate;

/**
 * Launches the application by loading the {@link edu.dbgroup.gui.components.HomeView}'s fxml file located:
 *          in this module's resources folder at: /edu/dbgroup/gui/components/home_view.fxml
 *          and controlled by the class {@link edu.dbgroup.gui.components.HomeView}
 */
public class ApplicationLauncher extends Application {

    private final static Logger logger = LoggerFactory.getLogger(ApplicationLauncher.class);

    @NonNull @Initialized
    private HomeView homeView;

    @Override
    public void start(Stage stage) throws Exception {

        ServiceProvider.INSTANCE.QUERIES.getKansasMapQueries().averageAll(1,
                Date.valueOf(LocalDate.of(2016, 1, 1)),
                Date.valueOf(LocalDate.of(2016, 6, 1))
        );

        boolean quit = true;
        if (quit) {
            Thread.sleep(30000);
            System.exit(0);
        }

        homeView = new HomeView();

        final FXMLLoader loader =
                new FXMLLoader(homeView.getClass().getResource("/edu/dbgroup/gui/components/home_view.fxml"));

//        loader.setRoot(homeView);
//        loader.setController(homeView);
        loader.setClassLoader(homeView.getClass().getClassLoader());
        final Parent root = loader.load();

//        final Parent root = FXMLLoader.load(this.getClass().getResource("/edu/dbgroup/gui/components/home_view.fxml"));

        stage.setScene(new Scene(root, 900, 500));
        stage.show();
    }

    @Override
    public void init() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    public static void main(String[] args) {
        launch(args);
    }
}
