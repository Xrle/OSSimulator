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
    private Deque<String> dataBuffer;
    private Map<Integer, String> instructionCache;
    private Map<Integer, Map<String, Integer>> varCache;
    private Map<Integer, Map<String, Integer>> labelCache;

    public CPU(Scheduler scheduler, Mailbox mailbox, double clockSpeed, ObservableList<String> trace, ObservableList<String> output) {
        this.scheduler = scheduler;
        this.mailbox = mailbox;
        this.clockSpeed = clockSpeed;
        this.trace = trace;
        this.output = output;
        this.process = null;
        this.dataBuffer = new ArrayDeque<>();
        this.instructionCache = new HashMap<>();
        this.varCache = new HashMap<>();
        this.labelCache = new HashMap<>();
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
            //Remove data for any dropped processes
            {
                boolean done = false;
                while (!done) {
                    Message message = this.mailbox.get(Mailbox.CPU);
                    if (message != null) {
                        String[] command = message.getCommand();
                        if (command[0].equals("drop")) {
                            int pid = Integer.parseInt(command[1]);
                            this.instructionCache.remove(pid);
                            this.varCache.remove(pid);
                            this.labelCache.remove(pid);
                            this.output("[CPU] Dropped PID " + pid);
                        }
                    }
                    else{
                        done = true;
                    }
                }
            }

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
                        this.block();
                    }
                    else {
                        this.instructionCache.put(pid, message.getCommand()[1]);
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
                                this.dataBuffer.add(command[1]);
                                if (command[2].equals("true")) {
                                    done = true;
                                }
                            }
                        }
                        else {
                            done = true;
                        }
                    }

                    //Split label from instruction
                    String instruction;
                    String[] split = this.instructionCache.get(pid).split(":", 2);
                    if (!this.labelCache.containsKey(pid)) {
                        this.labelCache.put(pid, new HashMap<>());
                    }
                    try {
                        //Will throw exception if there is no label on this line
                        instruction = split[1];
                        this.labelCache.get(pid).put(split[0], this.process.pc);
                    }
                    catch (ArrayIndexOutOfBoundsException e) {
                        instruction = split[0];
                    }

                    //If data was provided execute instruction with it. If not, execution will determine the data needed
                    if (this.dataBuffer.isEmpty()) {
                        this.exec(instruction);
                        this.log("[" + pid + "/NODATA] " + instruction);
                    }
                    else {
                        this.execData(instruction, this.dataBuffer);
                        this.log("[" + pid + "] " + instruction);
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

    /**
     * Convert an address referenced by a process to the actual virtual address of that data
     * @param address Address as seen by the process
     * @return The corresponding virtual address
     */
    private int getRealAddress(int address) {
        return address + this.process.getCodeLength();
    }

    private void block() {
        this.scheduler.block(this.process);
        this.process = null;
    }

    private void drop() {
        this.mailbox.put(Mailbox.CPU, Mailbox.SCHEDULER, "drop|" + this.process.getPid());
        this.block();
    }

    private void exec(String instruction) {
        try {
            int pid = this.process.getPid();
            String[] tokens = instruction.split("\\s");
            switch (tokens[0]) {
                //var [name] [address]
                case "var": {
                    //Create a varCache entry for this process
                    if (!this.varCache.containsKey(pid)) {
                        this.varCache.put(pid, new HashMap<>());
                    }
                    this.varCache.get(pid).put(tokens[1], this.getRealAddress(Integer.parseInt(tokens[2])));
                    this.instructionCache.remove(pid);
                    this.process.pc++;
                }
                break;

                //alloc [blocks]
                case "alloc": {
                    this.mailbox.put(Mailbox.CPU, Mailbox.MMU, "allocate|" + pid + "|" + tokens[1] + "|false");
                    this.instructionCache.remove(pid);
                    this.process.pc++;
                    this.block();
                }
                break;

                //free [blocks]
                case "free": {
                    this.mailbox.put(Mailbox.CPU, Mailbox.MMU, "free|" + pid + "|" + tokens[1] + "|false");
                    this.instructionCache.remove(pid);
                    this.process.pc++;
                }
                break;

                //exit
                case "exit": {
                    this.drop();
                }
                break;

                //jump [label]
                case "jump": {
                    if (this.labelCache.containsKey(pid)) {
                        if (this.labelCache.get(pid).containsKey(tokens[1])) {
                            this.process.pc = this.labelCache.get(pid).get(tokens[1]);
                            this.instructionCache.remove(pid);
                        }
                        else {
                            throw new IllegalArgumentException("Label not defined");
                        }
                    }
                    else {
                        throw new IllegalArgumentException("Label not defined");
                    }
                }
                break;

                //set [var] [value]
                case "set": {
                    if (this.varCache.containsKey(pid)) {
                        if (this.varCache.get(pid).containsKey(tokens[1])) {
                            this.mailbox.put(Mailbox.CPU, Mailbox.MMU, "write|" + pid + "|" + this.varCache.get(pid).get(tokens[1]) +  "|" + tokens[2] + "|true");
                        }
                        else {
                            throw new IllegalArgumentException("Variable not defined");
                        }
                    }
                    else {
                        throw new IllegalArgumentException("Variable not defined");
                    }
                }
                break;

                default: {
                    throw new IllegalArgumentException("Invalid instruction");
                }
            }
        }
        catch (Exception e) {
            //Output exception caused by process and drop it
            this.output("[CPU/ERROR] " + e.getClass().getSimpleName() + " in PID " + this.process.getPid() + " at '" + instruction + "': " + e.getMessage());
            this.drop();
        }
    }

    private void execData(String instruction, Deque<String> data) {
        try {

        }
        catch (Exception e) {
            //Output exception caused by process and drop it
            this.output("[CPU/ERROR] " + e.getClass().getSimpleName() + " in PID " + this.process.getPid() + " at '" + instruction + "': " + e.getMessage());
            this.drop();
        }
    }
}
