package com.cd00827.OSSimulator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;

public class Scheduler implements Runnable {
    private Queue<PCB> mainQueue;
    private Queue<PCB> priorityQueue;
    private Queue<PCB> blockedQueue;
    private Queue<PCB> swapQueue;
    private Mailbox mailbox;
    private int clockSpeed;
    private int quantum;

    public Scheduler(int clockSpeed, Mailbox mailbox, int quantum) {
        this.clockSpeed = clockSpeed;
        this.mailbox = mailbox;
        this.quantum = quantum;
        this.mainQueue = new ArrayDeque<>();
        this.priorityQueue = new ArrayDeque<>();
        this.blockedQueue = new ArrayDeque<>();
        this.swapQueue = new ArrayDeque<>();
    }

    @Override
    public void run() {

    }
}
