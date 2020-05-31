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
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Process scheduler, uses a combination of round robin and priority scheduling to schedule the execution of
 * multiple processes concurrently
 * @author cd00827
 **/
public class Scheduler implements Runnable {
    private final Deque<PCB> mainQueue;
    private final Deque<PCB> priorityQueue;
    private final Deque<PCB> blockedQueue;
    private final Deque<PCB> swapQueue;
    private final Deque<PCB> loadingQueue;
    private PCB running;
    private final Map<Integer, PCB> processes;

    private final Mailbox mailbox;
    private final double clockSpeed;
    private final int quantum;
    private final ObservableList<String> log;
    private final ReentrantLock swapLock;
    private final SynchronousQueue<PCB> blockRendezvous;
    private final List<PCB> swappable;

    /**
     * Constructor
     * @param clockSpeed Number of operations this scheduler should perform per second
     * @param mailbox The mailbox to control this scheduler with
     * @param quantum Number of cycles before switching to another process
     * @param log Log to output messages to
     * @param swapLock Lock used for synchronising the scheduler and MMU when swapping
     * @param swappable List to store currently swappable processes in
     */
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
        this.blockRendezvous = new SynchronousQueue<>();
    }

    /**
     * Write a message to the log
     * @param message Message
     */
    private void log(String message) {
        Platform.runLater(() -> this.log.add(message));
    }

    /**
     * Get a reference to the currently running process
     * @return Currently running process
     */
    public PCB getRunning() {
        return this.running;
    }

    /**
     * Block a specified process, used by the CPU to block it's current process.
     * This is the only scheduler operation not accessed through the mailbox, as it must be executed immediately,
     * or the CPU must wait for it to execute so that it doesn't reacquire a process it has blocked if the scheduler has
     * a backlog of commands to execute.
     * @param process Process to block
     */
    public void block(PCB process) {
        try {
            this.blockRendezvous.put(process);
        }
        catch (InterruptedException e) {
            this.log("[SCHEDULER/ERROR] Block operation interrupted");
        }
    }

    /**
     * Entry point when starting the scheduler thread
     */
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
                            //Load process code into memory
                            BufferedReader reader = new BufferedReader(new FileReader(new File(String.valueOf(process.getCodePath()))));
                            for (int i = 0; i < process.getCodeLength(); i++) {
                                String line = reader.readLine();
                                if (!line.equals("")) {
                                    if (i == process.getCodeLength() - 1) {
                                        //Signal final write operation
                                        this.mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "write|" + pid + "|" + i +  "|" + line + "|true");
                                    }
                                    else {
                                        this.mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "write|" + pid + "|" + i +  "|" + line + "|false");
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
                    this.switchProcess();
                }
            }
            //Otherwise attempt to run a process
            else {
                this.switchProcess();
            }

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

            //Allow the CPU to block it's process, must happen after scheduling but before swapping otherwise MMU may attempt
            //to swap out a process that the scheduler has just switched from, but that the CPU still wants to block
            {
                PCB process = this.blockRendezvous.poll();
                if (process != null) {
                    if (this.running == process) {
                        this.running = null;
                    }
                    this.mainQueue.remove(process);
                    this.priorityQueue.remove(process);
                    this.blockedQueue.add(process);
                    this.log("[SCHEDULER] Blocked PID " + process.getPid());
                }
            }

            //Unblock, drop and skip can all be responses to CPU operations, so must execute after it blocks the process
            //to prevent errors
            if (message != null) {
                String[] command = message.getCommand();
                switch (command[0]) {
                    //unblock [pid]
                    case "unblock": {
                        int pid = Integer.parseInt(command[1]);
                        PCB process = this.processes.get(pid);
                        if (this.blockedQueue.contains(process)) {
                            this.blockedQueue.remove(process);
                            this.priorityQueue.add(process);
                            this.log("[SCHEDULER] Unblocked PID " + pid);
                        } else {
                            this.log("[SCHEDULER/ERROR] Attempted to unblock PID " + pid + ", but it wasn't blocked");
                        }
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
                        this.mailbox.put(Mailbox.SCHEDULER, Mailbox.CPU, "drop|" + pid);
                        this.log("[SCHEDULER] Dropped PID " + pid);
                    }
                    break;

                    //skip [pid]
                    case "skip": {
                        int pid = Integer.parseInt(command[1]);
                        PCB process = this.processes.get(pid);
                        if (this.running == process) {
                            this.running = null;
                        }
                        this.blockedQueue.remove(process);
                        this.swapQueue.remove(process);
                        this.loadingQueue.remove(process);
                        this.mainQueue.add(process);
                        this.log("[SCHEDULER] Skipped PID " + pid);
                    }
                    break;
                }
            }

            //Release swap lock, allowing MMU to swap processes
            this.swapLock.unlock();

            //Wait for next clock cycle
            try {
                Thread.sleep((long)((1/clockSpeed) * 1000));
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * Switch to the next process
     */
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
                try {
                    //Get required memory
                    Stream<String> stream = Files.lines(process.getCodePath());
                    int blocks = (int) stream.count();
                    stream.close();
                    process.setCodeLength(blocks);
                    //Allocate
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
