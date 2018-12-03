package edu.dbgroup.gui.components;

import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PopupGraphView<X, Y> extends Stage {


    public PopupGraphView(Axis<X> xAxis, Axis<Y> yAxis, String chartTitle, String name, XYChart.Series<X, Y> ... series) {
        setTitle(name);

        final AreaChart<X, Y> areaChart = new AreaChart<>(xAxis, yAxis);
        areaChart.setTitle(chartTitle);

        for (XYChart.Series<X, Y> xySeries : series) {
            areaChart.getData().add(xySeries);
        }

        final Scene scene = new Scene(areaChart, 800, 600);
        setScene(scene);
        show();
    }

}