module org.example.module03assignmentgroup3 {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.module03assignmentgroup3 to javafx.fxml;
    exports org.example.module03assignmentgroup3;
}