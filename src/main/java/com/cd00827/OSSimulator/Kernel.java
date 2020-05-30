package com.cd00827.OSSimulator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReentrantLock;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Kernel, handles GUI events and configures/runs the simulator
 * @author cd00827
 */
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
    private Button boot;
    @FXML
    private Button shutdown;
    @FXML
    private TextField pageSize;
    @FXML
    private TextField pageNumber;
    @FXML
    private TextField memoryClock;
    @FXML
    private TextField quantum;
    @FXML
    private TextField schedulerClock;
    @FXML
    private TextField cpuClock;

    private Stage stage;
    private Mailbox mailbox;
    private Thread mmu;
    private Thread scheduler;
    private Thread cpu;
    private boolean booted = false;
    private FileChooser fileChooser;
    private ReentrantLock swapLock;
    private List<PCB> swappable;

    /**
     * Called when the JavaFX application loads the kernel
     * @param url URL
     * @param rb ResourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.shutdown.setDisable(true);
        this.mailbox = new Mailbox(this.mailboxLog.getItems());
        this.fileChooser = new FileChooser();
        this.fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        this.input.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.swapLock = new ReentrantLock();
        this.swappable = new ArrayList<>();
        this.pageSize.textProperty().addListener(new IntChecker(this.pageSize.textProperty()));
        this.pageNumber.textProperty().addListener(new IntChecker(this.pageNumber.textProperty()));
        this.memoryClock.textProperty().addListener(new DoubleChecker(this.memoryClock.textProperty()));
        this.quantum.textProperty().addListener(new IntChecker(this.quantum.textProperty()));
        this.schedulerClock.textProperty().addListener(new DoubleChecker(this.schedulerClock.textProperty()));
        this.cpuClock.textProperty().addListener(new DoubleChecker(this.cpuClock.textProperty()));

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

    /**
     * Add an input file to the simulator
     */
    @FXML
    private void addFile() {
        List<File> files = this.fileChooser.showOpenMultipleDialog(this.stage);
        if (files != null) {
            for (File file : files) {
                this.input.getItems().add(new InputFile(file));
            }
        }
    }

    /**
     * Remove an input file from the simulator
     */
    @FXML
    private void removeFile() {
        this.input.getItems().removeAll(this.input.getSelectionModel().getSelectedItems());
    }

    /**
     * Execute selected input files
     */
    @FXML
    private void execute() {
        for (InputFile file : this.input.getItems()) {
            this.mailbox.put(Mailbox.KERNEL, Mailbox.SCHEDULER, "new|" + file.getPath());
        }
        this.input.getItems().clear();
    }

    /**
     * Validate the given boot parameters
     * @return True if valid parameters have been given
     */
    private boolean bootValidator() {
        if (this.pageSize.getText().equals("")) {
            return false;
        }
        if (this.pageNumber.getText().equals("")) {
            return false;
        }
        if (this.memoryClock.getText().equals("")) {
            return false;
        }
        if (this.quantum.getText().equals("")) {
            return false;
        }
        if (this.schedulerClock.getText().equals("")) {
            return false;
        }
        if (this.cpuClock.getText().equals("")) {
            return false;
        }
        return true;
    }

    /**
     * Start the simulator
     */
    @FXML
    private void boot() {
        if (this.bootValidator()) {
            int pageSize = Integer.parseInt(this.pageSize.getText());
            int pageNumber = Integer.parseInt(this.pageNumber.getText());
            double memoryClock = Double.parseDouble(this.memoryClock.getText());

            this.mmu = new Thread(new MMU(pageSize, pageNumber, memoryClock, this.mailbox, this.output.getItems(), this.swapLock, this.swappable));
            this.mmu.start();
            this.output.getItems().add("[KERNEL] Started MMU with " + pageNumber + " " + pageSize + " block pages (" + pageNumber * pageSize + " blocks physical RAM) at clock speed " + memoryClock + "ops/s");

            double schedulerClock = Double.parseDouble(this.schedulerClock.getText());
            int quantum = Integer.parseInt(this.quantum.getText());

            Scheduler schedulerInstance = new Scheduler(schedulerClock, this.mailbox, quantum, this.output.getItems(), this.swapLock, this.swappable);
            this.scheduler = new Thread(schedulerInstance);
            this.scheduler.start();
            this.output.getItems().add("[KERNEL] Started scheduler with quantum " + quantum + " at " + schedulerClock + "ops/s");

            double cpuClock = Double.parseDouble(this.cpuClock.getText());

            this.cpu = new Thread(new CPU(schedulerInstance, this.mailbox, cpuClock, this.execTrace.getItems(), this.output.getItems()));
            this.cpu.start();
            this.output.getItems().add("[KERNEL] Started CPU at " + cpuClock + "ops/s");

            this.booted = true;
            this.boot.setDisable(true);
            this.shutdown.setDisable(false);
        }
        else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "One or more fields left empty");
            alert.show();
        }
    }

    /**
     * Set the stage that initialized this kernel, required for passing to the file dialog window
     * @param stage Stage
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Stop the simulator
     */
    @FXML
    public void shutdown() {
        if (this.booted) {
            this.mmu.interrupt();
            this.output.getItems().add("[KERNEL] Stopped MMU");
            this.scheduler.interrupt();
            this.output.getItems().add("[KERNEL] Stopped scheduler");
            this.cpu.interrupt();
            this.output.getItems().add("[KERNEL] Stopped CPU");
            this.mailbox.clear();
            this.boot.setDisable(false);
            this.shutdown.setDisable(true);
        }
    }

    /**
     * Scroll to the bottom of all the logs, and enable them to autoscroll
     */
    @FXML
    private void autoscroll() {
        this.output.scrollTo(this.output.getItems().size() - 1);
        this.mailboxLog.scrollTo(this.mailboxLog.getItems().size() - 1);
        this.execTrace.scrollTo(this.execTrace.getItems().size() - 1);
    }
}
