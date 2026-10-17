module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // Allow javafx.fxml to instantiate controllers and App via reflection
    opens com.example.app to javafx.fxml;
    opens com.example.ui  to javafx.fxml;

    // Public API
    exports com.example.anim;
    exports com.example.app;
    exports com.example.db;
    exports com.example.model;
    exports com.example.svg;
}
