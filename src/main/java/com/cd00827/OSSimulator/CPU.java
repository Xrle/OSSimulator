package com.cd00827.OSSimulator;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class CPU implements Runnable{
    private PCB process;
    private Scheduler scheduler;
    private double clockSpeed;
    private Mailbox mailbox;
    private ObservableList<String> trace;
    private ObservableList<String> output;
    private Deque<String[]> dataBuffer;
    private Map<Integer, String> instructionCache;

    public CPU(Scheduler scheduler, Mailbox mailbox, double clockSpeed, ObservableList<String> trace, ObservableList<String> output) {
        this.scheduler = scheduler;
        this.mailbox = mailbox;
        this.clockSpeed = clockSpeed;
        this.trace = trace;
        this.output = output;
        this.process = null;
        this.dataBuffer = new ArrayDeque<>();
        this.instructionCache = new HashMap<>();
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
            if (this.process != null) {
                int pid = this.process.getPid();
                this.dataBuffer.clear();

                //If there is no instruction cached, try and pull one from the mailbox, otherwise request a new one
                if (!this.instructionCache.containsKey(pid)) {
                    Message message = this.mailbox.get(String.valueOf(pid));
                    if (message == null) {
                        this.mailbox.put(String.valueOf(pid), Mailbox.MMU, "read|" + pid + "|" + this.process.pc + "|true");
                        this.log("[" + pid + "] Fetch " + this.process.pc);
                        this.scheduler.block(this.process);
                        this.process = null;
                    }
                    else {
                        this.instructionCache.put(pid, message.getCommand()[2]);
                    }
                }

                //Check that the process wasn't just blocked
                if (this.process != null) {
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

                    //If data was provided execute instruction with it. If not, execution will determine the data needed
                    if (this.dataBuffer.isEmpty()) {
                        this.exec(this.instructionCache.get(pid));
                    }
                    else {
                        this.execData(this.instructionCache.get(pid), this.dataBuffer);
                    }
                }
            }

            //Wait for next clock cycle
            try {
                Thread.sleep((long) (clockSpeed * 1000));
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }

    private void exec(String instruction) {

    }

    private void execData(String instruction, Deque<String[]> data) {

    }
}
