package com.cd00827.OSSimulator;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.stream.Stream;
/**
 * Produces:<br>
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
    }

    @Override
    public void run() {
        while (true) {
            //Get next command

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
