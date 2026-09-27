package org.example.module03assignmentgroup3;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.Group;

public class Car extends Pane {

    private String direction = "RIGHT";

    public Car() {

        Polygon body = new Polygon(
                0.0, 8.0,
                4.0, 2.0,
                14.0, 1.0,
                16.0, 4.0,
                22.0, 5.0,
                24.0, 8.0
        );
        body.setFill(Color.BLUEVIOLET);

        Rectangle window1 = new Rectangle(
                5.5,
                2.5,
                3,
                3
        );
        window1.setFill(Color.YELLOWGREEN);

        Rectangle window2 = new Rectangle(
                11.0,
                2.5,
                4.5,
                3
        );
        window2.setFill(Color.YELLOWGREEN);

        Ellipse wheel1 = new Ellipse(
                6.5,
                8.0,
                2.3,
                2.3
        );
        wheel1.setFill(Color.BLACK);

        Ellipse wheel2 = new Ellipse(
                18.5,
                8.0,
                2.3,
                2.3
        );
        wheel2.setFill(Color.BLACK);

        getChildren().addAll(
                body,
                window1,
                window2,
                wheel1,
                wheel2
        );


        setPrefSize(24, 11);
    }


    public void setDirection(String direction) {

        this.direction = direction;

        switch (direction) {

            case "RIGHT" -> {
                setRotate(0);
                setScaleX(1);
                setScaleY(1);
            }

            case "LEFT" -> {
                setRotate(0);
                setScaleX(-1);
                setScaleY(1);
            }

            case "UP" -> {
                setRotate(-90);
                setScaleX(1);
                setScaleY(1);
            }

            case "DOWN" -> {
                setRotate(90);
                setScaleX(1);
                setScaleY(1);
            }
        }
    }
    public String getDirection(){
        return direction;
    }
}