package com.cd00827.OSSimulator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;

public class Scheduler implements Runnable {
    private Queue<PCB> mainQueue;
    private Queue<PCB> priorityQueue;
    private Queue<PCB> blockedQueue;
    private Queue<PCB> swapQueue;
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
        this.running = null;
    }

    @Override
    public void run() {
        while (true) {
            //Get next command

            //Round robin
            //Move a process from main queue to priority queue if priority queue is empty
            if (this.priorityQueue.isEmpty() && !this.mainQueue.isEmpty()) {
                this.priorityQueue.add(this.mainQueue.poll());
            }
            //If there's a running process, decrement it's quantum and switch process if needed
            if (this.running != null) {
                //Decrement quantum
                if(this.running.decrement()) {
                    //Send previous to back of queue
                    this.mainQueue.add(this.running);
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
                                this.priorityQueue.remove(process);
                                this.swapQueue.add(process);
                            }
                        }
                        //Load process file
                        else {

                        }
                    }
                }
            }
            //Otherwise attempt to run a process
            if (!this.priorityQueue.isEmpty()) {
                this.running = this.priorityQueue.poll();
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
}
