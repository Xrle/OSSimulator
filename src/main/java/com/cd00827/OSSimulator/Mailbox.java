package com.cd00827.OSSimulator;

import javafx.collections.ObservableList;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.List;

public class Mailbox {
    //Predefined mailbox labels
    public static final String MMU = "MMU";
    public static final String SCHEDULER = "SCHEDULER";
    public static final String KERNEL = "KERNEL";

    private final ObservableList<Message> log;
    public List<Message> queue;

    public Mailbox(ObservableList<Message> log) {
        this.log = log;
        this.queue = new ArrayList<>();
    }

    synchronized void put(String sender, String target, String command) {
        Message message = new Message(sender, target, command);
        this.queue.add(message);
        this.log.add(message);
    }

    synchronized Message get(String target) {
        Message message = null;
        for (Message i : this.queue) {
            if (i.getTarget().equals(target)) {
                message = i;
                this.queue.remove(i);
                this.log.remove(message);
                break;
            }
        }
        return message;
    }
}
