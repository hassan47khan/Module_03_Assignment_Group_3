package org.example.module03assignmentgroup3;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Load the layout that contains the maze and robot images.
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));

        // Create a window large enough to display the maze image.
        Scene scene = new Scene(fxmlLoader.load());

        // Put the scene in a window and show it.
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }
}
