package com.cd00827.OSSimulator;

public class Address {
    //Static type references;
    public static String STRING = "STRING";
    public static String INT = "INT";
    public static String DOUBLE = "DOUBLE";
    public static String BOOL = "BOOL";

    private final String type;
    private final String data;

    public Address(String type, String data) {
        this.type = type;
        this.data = data;
    }

    public String[] read() {
        return new String[] {this.type, this.data};
    }

    @Override
    public String toString() {
        return this.type + "::" + this.data;
    }
}
