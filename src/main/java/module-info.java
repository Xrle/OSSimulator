module OSSimulator.main {
    requires javafx.fxml;
    requires javafx.controls;

    opens com.cd00827.OSSimulator to javafx.fxml;

    exports com.cd00827.OSSimulator;
}