package com.cd00827.OSSimulator;

/**
 * A message to be handled by a Mailbox
 * @author cd00827
 */
public class Message {
    private final String sender;
    private final String target;
    private final String command;

    /**
     * Constructor
     * @param sender Sender of this message
     * @param target Target receiver of this message
     * @param command Command to be executed by target
     */
    public Message(String sender, String target, String command) {
        this.sender = sender;
        this.target = target;
        this.command = command;
    }

    /**
     * Get the sender of this message
     * @return Sender
     */
    public String getSender() {
        return this.sender;
    }

    /**
     * Get the target of this message
     * @return Target
     */
    public String getTarget() {
        return this.target;
    }

    /**
     * Get the command contained in this message
     * @return Command
     */
    public String[] getCommand() {
        return this.command.split("\\|");
    }

    /**
     * Represent this message as a string
     * @return String representation
     */
    @Override
    public String toString() {
        return "[" + this.sender + " => " + this.target + "] " + this.command.replace("|", " | ");
    }
}
