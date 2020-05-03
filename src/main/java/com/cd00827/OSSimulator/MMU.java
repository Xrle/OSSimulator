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

    public Address read(PCB pcb, int address) {
        int page = address / this.pageSize;
        int offset = address % this.pageSize;
        return this.ram[pageTable.get(pcb.getPid()).get(page) + offset];
    }

    public void write(PCB pcb, int address, String data) {

    }

    public void write(PCB pcb, int address, String data) {

    }

    public void write(PCB pcb, int address, String data) {

    }

    public void write(PCB pcb, int address, String data) {

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
