package com.cd00827.OSSimulator;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.util.Deque;
import java.util.Map;

public class CPU implements Runnable{
    private PCB process;
    private Scheduler scheduler;
    private double clockSpeed;
    private Mailbox mailbox;
    private ObservableList<String> trace;
    private ObservableList<String> output;
    private Deque<String[]> dataBuffer;

    public CPU(Scheduler scheduler, Mailbox mailbox, double clockSpeed, ObservableList<String> trace, ObservableList<String> output) {
        this.scheduler = scheduler;
        this.mailbox = mailbox;
        this.clockSpeed = clockSpeed;
        this.trace = trace;
        this.output = output;
        this.process = null;
    }

    private void output(String message) {
        Platform.runLater(() -> this.output.add(message));
    }
    private void log(String message) {
        Platform.runLater(() -> this.trace.add(message));
    }


    @Override
    public void run() {
        while (true) {
            //Get a reference to running process
            this.process = this.scheduler.getRunning();
            int pid = this.process.getPid();
            this.dataBuffer.clear();

            //Get instruction
            String instruction = null;

            //Load requested data into buffer
            boolean done = false;
            while(!done) {
                Message message = this.mailbox.get(String.valueOf(pid));
                if (message != null) {
                    String[] command = message.getCommand();
                    if (command[0].equals("data")) {
                        this.dataBuffer.add(new String[] {command[1], command[2]});
                        if (command[3].equals("true")) {
                            done = true;
                        }
                    }
                }
                else {
                    done = true;
                }
            }
            if (!this.dataBuffer.isEmpty()) {
                this.exec(instruction);
            }
            else {
                this.execData(instruction, this.dataBuffer);
            }



            //Wait for next clock cycle
            try {
                Thread.sleep((long) (clockSpeed * 1000));
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void exec(String instruction) {

    }

    private void execData(String instruction, Deque<String[]> data) {

    }
}
