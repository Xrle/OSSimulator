package com.cd00827.OSSimulator;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Mailbox used for synchronised communication between threads.<br>
 * Any thread may send and receive messages to/from an unlimited other number of threads using this. Messages will
 * always be received in the order they were sent.
 * @author cd00827
 */
public class Mailbox {
    //Predefined mailbox labels
    public static final String MMU = "MMU";
    public static final String SCHEDULER = "SCHEDULER";
    public static final String KERNEL = "KERNEL";
    public static final String CPU = "CPU";

    private final ObservableList<Message> log;
    public List<Message> queue;

    /**
     * Constructor
     * @param log Log to keep track of the contents of this mailbox with
     */
    public Mailbox(ObservableList<Message> log) {
        this.log = log;
        this.queue = new ArrayList<>();
    }

    /**
     * Add a message to the mailbox
     * @param sender Sender of this message
     * @param target Target recipient of this message
     * @param command Command in this message
     */
    synchronized void put(String sender, String target, String command) {
        Message message = new Message(sender, target, command);
        this.queue.add(message);
        Platform.runLater(() -> this.log.add(message));
    }

    /**
     * Return and remove from the mailbox the next message for the given target
     * @param target Target to get a message for
     * @return Message or null if no messages for given target
     */
    synchronized Message get(String target) {
        Message message = null;
        for (Message i : this.queue) {
            if (i.getTarget().equals(target)) {
                message = i;
                this.queue.remove(i);
                Platform.runLater(() -> this.log.remove(i));
                break;
            }
        }
        return message;
    }

    /**
     * Clear this mailbox
     */
    synchronized void clear() {
        this.queue.clear();
        Platform.runLater(this.log::clear);
    }
}
