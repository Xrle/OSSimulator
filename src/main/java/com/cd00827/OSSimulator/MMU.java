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

    public Address read(int pid, int address) {
        int page = address / this.pageSize;
        int offset = address % this.pageSize;
        return this.ram[this.pageTable.get(pid).get(page) + offset];
    }

    private boolean isAllocated(int pid, int page) {
        if (this.pageTable.get(pid).containsKey(page)) {
            return true;
        }
        else {
            return false;
        }
    }

    public void write(int pid, int address, String data) {
        int page = address / this.pageSize;
        int offset = address % this.pageSize;
        this.ram[this.pageTable.get(pid).get(page) + offset] = new Address(data);
    }

    public void write(int pid, int address, int data) {
        int page = address / this.pageSize;
        int offset = address % this.pageSize;
        this.ram[this.pageTable.get(pid).get(page) + offset] = new Address(data);
    }

    public void write(int pid, int address, double data) {
        int page = address / this.pageSize;
        int offset = address % this.pageSize;
        this.ram[this.pageTable.get(pid).get(page) + offset] = new Address(data);
    }

    public void write(int pid, int address, boolean data) {
        int page = address / this.pageSize;
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
