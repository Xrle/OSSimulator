package com.cd00827.OSSimulator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReentrantLock;

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
    private Thread cpu;
    private boolean booted = false;
    private FileChooser fileChooser;
    private ReentrantLock swapLock;
    private List<PCB> swappable;
    private PCB running;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.mailbox = new Mailbox(this.mailboxLog.getItems());
        this.fileChooser = new FileChooser();
        this.fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        this.input.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.swapLock = new ReentrantLock();
        this.swappable = new ArrayList<>();

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
    }

    @FXML
    private void addFile() {
        List<File> files = this.fileChooser.showOpenMultipleDialog(this.stage);
        if (files != null) {
            for (File file : files) {
                this.input.getItems().add(new InputFile(file));
            }
        }
    }

    @FXML
    private void removeFile() {
        this.input.getItems().removeAll(this.input.getSelectionModel().getSelectedItems());
    }

    @FXML
    private void execute() {
        for (InputFile file : this.input.getItems()) {
            this.mailbox.put(Mailbox.KERNEL, Mailbox.SCHEDULER, "new|" + file.getPath());
        }
        this.input.getItems().clear();
    }

    @FXML
    private void boot() {
        int pageSize = 5;
        int pageNumber = 5;
        double memoryClock = 1;

        this.mmu = new Thread(new MMU(pageSize, pageNumber, memoryClock, this.mailbox, this.output.getItems(), this.swapLock, this.swappable));
        this.mmu.start();
        this.output.getItems().add("[KERNEL] Started MMU with " + pageNumber + " " + pageSize + " block pages (" + pageNumber*pageSize + " blocks physical RAM) at clock speed " + memoryClock + "ops/s");

        double schedulerClock = 1.5;
        int quantum = 5;

        Scheduler schedulerInstance = new Scheduler(schedulerClock, this.mailbox, quantum, this.output.getItems(), this.swapLock, this.swappable);
        this.scheduler = new Thread(schedulerInstance);
        this.scheduler.start();
        this.output.getItems().add("[KERNEL] Started scheduler with quantum " + quantum + " at " + schedulerClock + "op/s");

        double cpuClock = 1.2;

        this.cpu = new Thread(new CPU(schedulerInstance, this.mailbox, cpuClock, this.execTrace.getItems(), this.output.getItems()));
        this.cpu.start();
        this.output.getItems().add("[KERNEL] Started CPU at " + cpuClock + "op/s");

        this.booted = true;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void shutdown() {
        if (this.booted) {
            this.mmu.interrupt();
            this.output.getItems().add("[KERNEL] Stopped MMU");
            this.scheduler.interrupt();
            this.output.getItems().add("[KERNEL] Stopped scheduler");
            this.cpu.interrupt();
            this.output.getItems().add("[KERNEL] Stopped CPU");
        }
    }
}
