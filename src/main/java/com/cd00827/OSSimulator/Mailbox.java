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
        Message message = new Message(sender, target, command);
        this.queue.add(message);
        System.out.println("Produced: " + message.toString());
    }

    synchronized Message get(String target) {
        Message message = null;
        for (Message i : this.queue) {
            if (i.getTarget().equals(target)) {
                message = i;
                this.queue.remove(i);
                System.out.println("Consumed: " + message.toString());
                break;
            }
        }
        return message;
    }
}
