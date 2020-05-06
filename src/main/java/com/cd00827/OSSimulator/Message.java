package com.cd00827.OSSimulator;

public class Message {
    private final String sender;
    private final String target;
    private final String command;

    public Message(String sender, String target, String command) {
        this.sender = sender;
        this.target = target;
        this.command = command;
    }

    public String getSender() {
        return this.sender;
    }

    public String getTarget() {
        return this.target;
    }

    public String[] getCommand() {
        return this.command.split("\\s");
    }
}
