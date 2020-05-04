package com.cd00827.OSSimulator;

import java.util.HashMap;
import java.util.Map;

public class MMU {
    private Address[] ram;
    private int pageSize;
    private int pageNumber;
    //Map pid to a map of page number to frame offset
    private Map<Integer, Map<Integer, Integer>> pageTable;
    //Keep a record of allocated frames
    private Map<Integer, Boolean> frameAllocationRecord;

    public MMU(int pageSize, int pageNumber) {
        this.ram = new Address[pageSize * pageNumber];
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
        this.pageTable = new HashMap<Integer, Map<Integer, Integer>>();
        this.frameAllocationRecord = new HashMap<Integer, Boolean>();
        for (int page = 0; page < pageNumber; page++) {
            frameAllocationRecord.put(page, false);
        }
    }

    private void checkAllocated(int pid, int page) throws AddressingException {
        if (!this.pageTable.get(pid).containsKey(page)) {
            throw new AddressingException("PID " + pid + " attempted to access memory it hasn't been allocated");
        }
    }

    public String readString(int pid, int address) throws AddressingException {
        int page = address / this.pageSize;
        this.checkAllocated(pid, page);
        int offset = address % this.pageSize;
        return this.ram[this.pageTable.get(pid).get(page) + offset].readString();
    }

    public int readInt(int pid, int address) throws AddressingException {
        int page = address / this.pageSize;
        this.checkAllocated(pid, page);
        int offset = address % this.pageSize;
        return this.ram[this.pageTable.get(pid).get(page) + offset].readInt();
    }

    public double readDouble(int pid, int address) throws AddressingException {
        int page = address / this.pageSize;
        this.checkAllocated(pid, page);
        int offset = address % this.pageSize;
        return this.ram[this.pageTable.get(pid).get(page) + offset].readDouble();
    }

    public boolean readBool(int pid, int address) throws AddressingException {
        int page = address / this.pageSize;
        this.checkAllocated(pid, page);
        int offset = address % this.pageSize;
        return this.ram[this.pageTable.get(pid).get(page) + offset].readBool();
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

    public void allocate(int pid, int blocks) throws AddressingException {
        int pages = (int)Math.ceil((double)blocks / this.pageSize);
        int freePages = 0;
        int currentPages = 0;

        //Find out how much memory the process is already using
        if (this.pageTable.containsKey(pid)) {
            for (Map.Entry<Integer, Integer> entry : this.pageTable.get(pid).entrySet()) {
                currentPages++;
            }
        }

        //Check that the system has enough memory
        if (pages + currentPages > this.pageNumber) {
            throw new AddressingException("PID " + pid + "attempted to allocate more memory than available to the system");
        }

        //Check that there are enough free pages, swap out other processes if not
        for (Map.Entry<Integer, Boolean> entry : this.frameAllocationRecord.entrySet()) {
            if (entry.getValue() == false) {
                freePages++;
            }
        }
        if (freePages < pages) {
            //TODO: Swap out processes
        }

        //Allocate pages
        for (int i = 0; i < pages; i++) {
            for (Map.Entry<Integer, Boolean> entry : this.frameAllocationRecord.entrySet()) {
                if (entry.getValue() == false) {
                    this.frameAllocationRecord.put(entry.getKey(), true);
                    if (!this.pageTable.containsKey(pid)) {
                        this.pageTable.put(pid, new HashMap<Integer, Integer>());
                    }
                    this.pageTable.get(pid).put(currentPages + i, entry.getKey());
                }
            }
        }
    }

    public void free(int pid, int blocks) {

    }

    public void flushProcess(int pid) {

    }

    public void swapOut(PCB pcb) {

    }

    public void swapIn(PCB pcb) {

    }
}
