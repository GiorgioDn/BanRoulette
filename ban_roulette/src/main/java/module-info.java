module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;        // needed by com.example.db stubs (full use in Step 3)

    // Allow javafx.fxml to instantiate controllers and App via reflection
    opens com.example.app to javafx.fxml;
    opens com.example.ui  to javafx.fxml;

    exports com.example.app;
}
