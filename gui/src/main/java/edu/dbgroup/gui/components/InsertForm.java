package edu.dbgroup.gui.components;

import com.sun.javafx.collections.ObservableListWrapper;
import edu.dbgroup.logic.ServiceProvider;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Date;
import java.time.LocalDate;

public class InsertForm extends Stage {

    public InsertForm() {
        TextArea usernameForm = new TextArea();
        TextArea warningText = new TextArea();
        ChoiceBox choiceBox = new ChoiceBox(new ObservableListWrapper(ServiceProvider.INSTANCE.getCountiesAsList()));
        DatePicker datePicker = new DatePicker(LocalDate.of(2010, 1, 1));

        setTitle("Insert Warning");

        final VBox vbox = new VBox();
        vbox.getChildren().add(usernameForm);
        vbox.getChildren().add(warningText);
        vbox.getChildren().add(choiceBox);
        vbox.getChildren().add(datePicker);

        final Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> {
            try {
                int id = ServiceProvider.INSTANCE.QUERIES.getUserId(usernameForm.getText());
                ServiceProvider.INSTANCE.QUERIES.insertData
                        (id,
                        Date.valueOf(datePicker.getValue()),
                        ServiceProvider.INSTANCE.getCountyIdByName((String)choiceBox.getValue()),
                                0.0, 0.0, 0, 0, 0, "", warningText.getText()
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        vbox.getChildren().add(submitButton);


        final Scene scene = new Scene(vbox, 800, 600);
        setScene(scene);
        show();
    }



}
