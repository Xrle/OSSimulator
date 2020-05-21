package com.cd00827.OSSimulator;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
/**
 * Produces:<br>
 *     MMU => write [pid] [address] [type] [data] [final]<br>
 * Consumes:<br>
 *     SCHEDULER => new [path]<br>
 *     SCHEDULER => unblock [pid]<br>
 *     SCHEDULER => swappedOut [pid]<br>
 *     SCHEDULER => swappedIn [pid]<br>
 *     SCHEDULER => skip [pid]<br>
 *     SCHEDULER => drop [pid]<br>
 *     SCHEDULER => allocated [pid]<br>
 *
 * @author cd00827
 **/
public class Scheduler implements Runnable {
    private Deque<PCB> mainQueue;
    private Deque<PCB> priorityQueue;
    private Deque<PCB> blockedQueue;
    private Deque<PCB> swapQueue;
    private Deque<PCB> loadingQueue;
    private PCB running;
    private Map<Integer, PCB> processes;

    private Mailbox mailbox;
    private double clockSpeed;
    private int quantum;
    private final ObservableList<String> log;
    private ReentrantLock swapLock;
    private ReentrantLock blockLock;
    private List<PCB> swappable;

    public Scheduler(double clockSpeed, Mailbox mailbox, int quantum, ObservableList<String> log, ReentrantLock swapLock, List<PCB> swappable) {
        this.clockSpeed = clockSpeed;
        this.mailbox = mailbox;
        this.quantum = quantum;
        this.mainQueue = new ArrayDeque<>();
        this.priorityQueue = new ArrayDeque<>();
        this.blockedQueue = new ArrayDeque<>();
        this.swapQueue = new ArrayDeque<>();
        this.loadingQueue = new ArrayDeque<>();
        this.running = null;
        this.processes = new HashMap<>();
        this.log = log;
        this.swapLock = swapLock;
        this.swappable = swappable;
        this.blockLock = new ReentrantLock();
    }

    private void log(String message) {
        Platform.runLater(() -> this.log.add(message));
    }

    public PCB getRunning() {
        return this.running;
    }

    public void block(PCB process) {
        this.blockLock.lock();
        if (this.running == process) {
            this.running = null;
        }
        this.mainQueue.remove(process);
        this.priorityQueue.remove(process);
        this.blockedQueue.add(process);
        this.log("[SCHEDULER] Blocked PID " + process.getPid());
        this.blockLock.unlock();
    }

    @Override
    public void run() {
        while (true) {
            //Acquire swap lock - MMU cannot swap out processes until lock is released
            //If MMU is currently swapping out processes, wait for it to complete
            this.swapLock.lock();

            //Get next command
            Message message = this.mailbox.get(Mailbox.SCHEDULER);
            if (message != null) {
                String[] command = message.getCommand();
                switch(command[0]) {
                    //new [path]
                    case "new": {
                        Path path = Path.of(command[1]);
                        int pid = 0;
                        while(this.processes.containsKey(pid)) {
                            pid++;
                        }
                        PCB process = new PCB(pid, path, this.quantum);
                        this.processes.put(pid, process);
                        this.mainQueue.add(process);
                        this.log("[SCHEDULER] Created PID " + pid + " from " + path);
                    }
                    break;

                    //allocated [pid]
                    case "allocated": {
                        int pid = Integer.parseInt(command[1]);
                        PCB process = this.processes.get(pid);
                        try {
                            BufferedReader reader = new BufferedReader(new FileReader(new File(String.valueOf(process.getCodePath()))));
                            for (int i = 0; i < process.getCodeLength(); i++) {
                                String line = reader.readLine();
                                if (!line.equals("")) {
                                    if (i == process.getCodeLength() - 1) {
                                        //Signal final write operation
                                        this.mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "write|" + pid + "|" + i + "|" + Address.STRING + "|" + line + "|true");
                                    }
                                    else {
                                        this.mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "write|" + pid + "|" + i + "|" + Address.STRING + "|" + line + "|false");
                                    }
                                }
                            }
                            reader.close();
                            //Move process from loading queue to blocked queue
                            this.loadingQueue.remove(process);
                            this.blockedQueue.add(process);
                            process.setLoaded();
                            this.log("[SCHEDULER] Successfully loaded PID "+ pid);
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                    //unblock [pid]
                    case "unblock": {
                        int pid = Integer.parseInt(command[1]);
                        PCB process = this.processes.get(pid);
                        if (this.blockedQueue.contains(process)) {
                            this.blockedQueue.remove(process);
                            this.priorityQueue.add(process);
                            this.log("[SCHEDULER] Unblocked PID " + pid);
                        }
                        else {
                            this.log("[SCHEDULER/ERROR] Attempted to unblock PID " + pid + ", but it wasn't blocked");
                        }
                    }
                    break;

                    //swappedIn [pid]
                    case "swappedIn": {
                        int pid = Integer.parseInt(command[1]);
                        PCB process = this.processes.get(pid);
                        process.setSwapped(false);
                        this.swapQueue.remove(process);
                        this.priorityQueue.add(process);
                        this.log("[SCHEDULER] Marked PID " + pid + " as swapped in");
                    }
                    break;

                    //swappedOut [pid]
                    case "swappedOut": {
                        int pid = Integer.parseInt(command[1]);
                        this.processes.get(pid).setSwapped(true);
                        this.log("[SCHEDULER] Marked PID " + pid + " as swapped out");
                    }
                    break;

                    //drop [pid]
                    case "drop": {
                        int pid = Integer.parseInt(command[1]);
                        PCB process = this.processes.get(pid);
                        if (this.running == process) {
                            this.running = null;
                        }
                        this.mainQueue.remove(process);
                        this.priorityQueue.remove(process);
                        this.blockedQueue.remove(process);
                        this.swapQueue.remove(process);
                        this.processes.remove(pid);
                        this.mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "drop|" + pid);
                        this.log("[SCHEDULER] Dropped PID " + pid);
                    }
                    break;

                    //skip [pid]
                    case "skip": {
                        int pid = Integer.parseInt(command[1]);
                        PCB process = this.processes.get(pid);
                        this.blockedQueue.remove(process);
                        this.swapQueue.remove(process);
                        this.loadingQueue.remove(process);
                        this.mainQueue.add(process);
                        this.log("[SCHEDULER] Skipped PID " + pid);
                    }
                    break;
                }
            }

            //Round robin
            //If there's a running process, decrement it's quantum and switch process if needed
            if (this.running != null) {
                //Decrement quantum
                if(this.running.decrement()) {
                    //Send previous to back of queue
                    this.mainQueue.add(this.running);
                    //Set running to null to prevent processes duplicating
                    this.running = null;
                    switchProcess();
                }
            }
            //Otherwise attempt to run a process
            else {
                switchProcess();
            }

            //If the CPU wants to block it's process, it must happen after this scheduler cycle but before swappable is updated
            this.blockLock.unlock();
            this.blockLock.lock();

            //Update list of swappable processes
            this.swappable.clear();
            List<PCB> bothQueues = new ArrayList<>();
            bothQueues.addAll(this.mainQueue);
            bothQueues.addAll(this.priorityQueue);

            for (PCB process : bothQueues) {
                if (process.isLoaded() && !process.isSwapped()) {
                    this.swappable.add(process);
                }
            }

            //Release swap lock, allowing MMU a window to swap out processes
            this.swapLock.unlock();

            //Wait for next clock cycle
            try {
                Thread.sleep((long)(clockSpeed * 1000));
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }

    private void switchProcess() {
        //Move a process from main queue to priority queue if priority queue is empty
        if (this.priorityQueue.isEmpty() && !this.mainQueue.isEmpty()) {
            PCB process = this.mainQueue.poll();
            this.log("[SCHEDULER] Moved PID " + process.getPid() + " to priority queue");
            this.priorityQueue.add(process);
        }
        //Check priority queue has a process
        if (!this.priorityQueue.isEmpty()) {
            PCB process = this.priorityQueue.poll();
            //Check process has been loaded from it's file
            if (process.isLoaded()) {
                //Check process is not swapped out
                if (!process.isSwapped()) {
                    this.running = process;
                    this.log("[SCHEDULER] Switched to running PID "+ process.getPid());
                }
                //Swap in process
                else {
                    this.mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "swapIn|" + process.getPid());
                    this.swapQueue.add(process);
                    this.log("[SCHEDULER] Waiting for PID " + process.getPid() + " to be swapped in");
                }
            }
            //Load process file
            else {
                //Attempt to allocate memory
                //Get required memory
                try {
                    Stream<String> stream = Files.lines(process.getCodePath());
                    int blocks = (int) stream.count();
                    stream.close();
                    process.setCodeLength(blocks);
                    this.mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "allocate|" + process.getPid() + "|" + blocks + "|true");
                    this.loadingQueue.add(process);
                    this.log("[SCHEDULER] Waiting for PID " + process.getPid() + " to be loaded from file");

                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                //Write commands will be sent after MMU notifies of successful allocation
            }
        }
    }
}
