module org.example.module03assignmentgroup3 {
    // JavaFX modules used by this application.
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // Allow FXML to create and connect the controller.
    opens org.example.module03assignmentgroup3 to javafx.fxml;

    // Make the application package available to the Java module system.
    exports org.example.module03assignmentgroup3;
}