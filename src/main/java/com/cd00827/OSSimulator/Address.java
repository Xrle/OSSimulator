package com.cd00827.OSSimulator;

public class Address {
    private enum Type {STRING, INT, DOUBLE, BOOL};
    private Type type;
    private String stringData;
    private int intData;
    private double doubleData;
    private boolean booleanData;

    public Address(String data) {
        this.type = Type.STRING;
        this.stringData = data;
    }

    public Address(int data) {
        this.type = Type.INT;
        this.intData = data;
    }

    public Address(double data) {
        this.type = Type.DOUBLE;
        this.doubleData = data;
    }

    public Address(boolean data) {
        this.type = Type.BOOL;
        this.booleanData = data;
    }

    public String readString() throws AddressingException {
        if (this.type != Type.STRING) {
            throw new AddressingException("Address does not contain a string type");
        }
        return this.stringData;
    }

    public int readInt() throws AddressingException {
        if (this.type != Type.INT) {
            throw new AddressingException("Address does not contain an int type");
        }
        return this.intData;
    }

    public double readDouble() throws AddressingException {
        if (this.type != Type.DOUBLE) {
            throw new AddressingException("Address does not contain a double type");
        }
        return this.doubleData;
    }

    public boolean readBool() throws AddressingException {
        if (this.type != Type.BOOL) {
            throw new AddressingException("Address does not contain a boolean type");
        }
        return this.booleanData;
    }
}
