package com.cd00827.OSSimulator;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class FXMLController implements Initializable {

    @FXML
    private ListView input;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        input.getItems().add("HI");
        input.getItems().add("banana");
        input.getItems().add(":))");
        input.getItems().add(":))");
        input.getItems().add(":))");
        input.getItems().add(":))");
        input.getItems().add(":))");
        input.getItems().add(":))");
        input.getItems().add(":))");
        input.getItems().add(":))");
        input.getItems().add(":))");
        input.getItems().add(":))");
        input.getItems().add(":))");
        input.getItems().add(":))");
        input.getItems().add(":))");
        input.getItems().add(":))");

    }
}
