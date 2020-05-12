package com.cd00827.OSSimulator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Kernel implements Initializable {

    @FXML
    private ListView<InputFile> input;
    @FXML
    private ListView<String> output;
    @FXML
    private ListView<String> execTrace;
    @FXML
    private ListView<Message> mailboxLog;
    @FXML
    private Button add;
    @FXML
    private Button remove;
    @FXML
    private Button execute;

    private Stage stage;
    private Mailbox mailbox;
    private Thread mmu;
    private Thread scheduler;
    private boolean booted = false;
    private FileChooser fileChooser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.mailbox = new Mailbox(this.mailboxLog.getItems());
        this.fileChooser = new FileChooser();
        this.fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        this.input.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //Set up input directory
        File dir = new File("input");
        if (!dir.exists()) {
            try {
                Files.createDirectory(dir.toPath());
                this.fileChooser.setInitialDirectory(dir);
            }
            catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Could not create input directory automatically");
                alert.show();
            }
        }
        else {
            this.fileChooser.setInitialDirectory(dir);
        }

        this.boot();
    }

    @FXML
    private void addFile() {
        for (File file : this.fileChooser.showOpenMultipleDialog(this.stage)) {
            this.input.getItems().add(new InputFile(file));
        }
    }

    @FXML
    private void removeFile() {
        this.input.getItems().removeAll(this.input.getSelectionModel().getSelectedItems());
    }

    @FXML
    private void execute() {
        for (InputFile file : this.input.getItems()) {
            this.mailbox.put(Mailbox.KERNEL, Mailbox.SCHEDULER, "new " + file.getPath());
        }
        this.input.getItems().clear();
    }

    private void boot() {
        int pageSize = 5;
        int pageNumber = 5;
        double memoryClock = 1;

        this.mmu = new Thread(new MMU(pageSize, pageNumber, memoryClock, this.mailbox));
        this.mmu.start();
        this.output.getItems().add("[KERNEL] Started MMU with " + pageNumber + " " + pageSize + " block pages (" + pageNumber*pageSize + " blocks physical RAM) at clock speed " + memoryClock + "ops/s");

        double schedulerClock = 1;
        int quantum = 5;

        this.scheduler = new Thread(new Scheduler(schedulerClock, this.mailbox, quantum));
        this.scheduler.start();
        this.output.getItems().add("[KERNEL] Started scheduler with quantum " + quantum + " at " + schedulerClock + "op/s");
        this.booted = true;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void shutdown() {
        if (this.booted) {
            this.mmu.interrupt();
            this.scheduler.interrupt();
        }
    }
}
