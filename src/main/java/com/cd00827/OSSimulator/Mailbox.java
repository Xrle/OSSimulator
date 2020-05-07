package com.cd00827.OSSimulator;

import java.util.ArrayList;
import java.util.List;

public class Mailbox {
    //Predefined mailbox labels
    public static final String MMU = "MMU";
    public static final String SCHEDULER = "SCHEDULER";

    public List<Message> queue;

    public Mailbox() {
        this.queue = new ArrayList<>();
    }

    synchronized void put(String sender, String target, String command) {
        this.queue.add(new Message(sender, target, command));
    }

    synchronized Message get(String target) {
        Message msg = null;
        for (Message i : this.queue) {
            if (i.getTarget().equals(target)) {
                msg = i;
                this.queue.remove(i);
                break;
            }
        }
        return msg;
    }
}
