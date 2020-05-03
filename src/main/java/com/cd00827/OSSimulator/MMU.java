package com.cd00827.OSSimulator;

import java.util.HashMap;
import java.util.Map;

public class MMU {
    private Address[] ram;
    private int pageSize;
    //Map pid to a map of page number to frame offset
    private Map<Integer, Map<Integer, Integer>> pageTable;

    public MMU(int pageSize, int ramMultiplier) {
        this.ram = new Address[pageSize * ramMultiplier];
        this.pageSize = pageSize;
        this.pageTable = new HashMap<Integer, Map<Integer, Integer>>();
    }

    public Address read(int pid, int address) throws AddressingException {
        int page = address / this.pageSize;
        this.checkAllocated(pid, page);
        int offset = address % this.pageSize;
        return this.ram[this.pageTable.get(pid).get(page) + offset];
    }

    private void checkAllocated(int pid, int page) throws AddressingException {
        if (!this.pageTable.get(pid).containsKey(page)) {
            throw new AddressingException("PID " + pid + " attempted to access memory it hasn't been allocated");
        }
    }

    public void write(int pid, int address, String data) throws AddressingException {
        int page = address / this.pageSize;
        this.checkAllocated(pid, page);
        int offset = address % this.pageSize;
        this.ram[this.pageTable.get(pid).get(page) + offset] = new Address(data);
    }

    public void write(int pid, int address, int data) throws AddressingException {
        int page = address / this.pageSize;
        this.checkAllocated(pid, page);
        int offset = address % this.pageSize;
        this.ram[this.pageTable.get(pid).get(page) + offset] = new Address(data);
    }

    public void write(int pid, int address, double data) throws AddressingException {
        int page = address / this.pageSize;
        this.checkAllocated(pid, page);
        int offset = address % this.pageSize;
        this.ram[this.pageTable.get(pid).get(page) + offset] = new Address(data);
    }

    public void write(int pid, int address, boolean data) throws AddressingException {
        int page = address / this.pageSize;
        this.checkAllocated(pid, page);
        int offset = address % this.pageSize;
        this.ram[this.pageTable.get(pid).get(page) + offset] = new Address(data);
    }

    public void allocate(PCB pcb, int blocks) {

    }

    public void free(PCB pcb) {

    }

    public void swapOut(PCB pcb) {

    }

    public void swapIn(PCB pcb) {

    }
}
