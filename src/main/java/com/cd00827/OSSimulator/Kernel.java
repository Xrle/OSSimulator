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

public class Kernel implements Initializable {

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

    }

    public void boot() {
        Mailbox mailbox = new Mailbox();
        int pageSize = 0;
        int pageNumber = 0;
        int memoryClock = 0;

        Thread mmu = new Thread(new MMU(pageSize, pageNumber, memoryClock, mailbox));
        mmu.start();

        try {

            Thread.sleep(1500);
            mmu.join();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

}
