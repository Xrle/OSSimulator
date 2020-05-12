package com.cd00827.OSSimulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
/**
 * Produces:<br>
 *     MMU => write [pid] [address] [type] [data] [final]<br>
 * Consumes:<br>
 *     SCHEDULER => new [path]<br>
 *     SCHEDULER => block [pid]<br>
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
    private Queue<PCB> mainQueue;
    private Queue<PCB> priorityQueue;
    private Queue<PCB> blockedQueue;
    private Queue<PCB> swapQueue;
    private Queue<PCB> loadingQueue;
    private PCB running;
    private Map<Integer, PCB> processes;

    private Mailbox mailbox;
    private double clockSpeed;
    private int quantum;

    public Scheduler(double clockSpeed, Mailbox mailbox, int quantum) {
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
    }

    @Override
    public void run() {
        while (true) {
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
                        this.mainQueue.add(process);
                    }
                    break;

                    //allocated [pid]
                    case "allocated": {
                        int pid = Integer.parseInt(command[1]);
                        PCB process = this.processes.get(pid);
                        try {
                            BufferedReader reader = new BufferedReader(new FileReader(new File(String.valueOf(process.getCodePath()))));
                            for (int i = 0; i < process.getCodeLength(); i++) {
                                if (i == process.getCodeLength() - 1) {
                                    //Signal final write operation
                                    this.mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "write " + i + " " + Address.STRING + " " + reader.readLine() + " true");
                                }
                                else {
                                    this.mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "write " + i + " " + Address.STRING + " " + reader.readLine() + " false");
                                }
                            }
                            //Move process from loading queue to blocked queue
                            this.loadingQueue.remove(process);
                            this.blockedQueue.add(process);
                            process.setLoaded();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
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
                    switchProcess();
                }
            }
            //Otherwise attempt to run a process
            else {
                switchProcess();
            }


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
            this.priorityQueue.add(this.mainQueue.poll());
        }
        //Check priority queue has a process
        if (!this.priorityQueue.isEmpty()) {
            PCB process = this.priorityQueue.poll();
            //Check process has been loaded from it's file
            if (process.isLoaded()) {
                //Check process is not swapped out
                if (!process.isSwapped()) {
                    this.running = process;
                }
                //Swap in process
                else {
                    this.mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "swapIn " + process.getPid());
                    this.swapQueue.add(process);
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
                    this.mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "allocate " + process.getPid() + " " + blocks + " true " + this.getSwapOrder());
                    this.loadingQueue.add(process);

                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                //Write commands will be sent after MMU notifies of successful allocation
            }
        }
    }

    private String getSwapOrder() {
        StringBuilder order = new StringBuilder();
        for (PCB process : this.mainQueue) {
            if (process.isLoaded() && !process.isSwapped()) {
                if (order.toString().equals("")) {
                    order.append(process.getPid());
                }
                else {
                    order.append(":").append(process.getPid());
                }
            }
        }
        return order.toString();
    }
}
