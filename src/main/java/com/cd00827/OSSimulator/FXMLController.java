package com.cd00827.OSSimulator;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;

public class FXMLController implements Initializable {

    @FXML
    private ListView input;
    @FXML
    private ListView output;
    @FXML
    private ListView execTrace;
    @FXML
    private Button add;
    @FXML
    private Button remove;
    @FXML
    private Button execute;
    @FXML
    private Button clear;

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
        output.getItems().add("test");
        output.getItems().add("test");
        output.getItems().add("test");
        output.getItems().add("test");
        output.getItems().add("test");
        execTrace.getItems().add("cheese");
        execTrace.getItems().add("cheese");
        execTrace.getItems().add("cheese");
        execTrace.getItems().add("cheese");
        execTrace.getItems().add("cheese");
        execTrace.getItems().add("cheese");
    }
}
