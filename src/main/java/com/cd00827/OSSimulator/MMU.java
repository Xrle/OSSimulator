package com.cd00827.OSSimulator;

import java.util.Map;
import java.util.TreeMap;

public class MMU {
    private final Address[] ram;
    private final int pageSize;
    private final int pageNumber;
    //Map pid to a map of page number to frame offset
    private final Map<Integer, Map<Integer, Integer>> pageTable;
    //Keep a record of allocated frames
    private final Map<Integer, Boolean> frameAllocationRecord;

    public MMU(int pageSize, int pageNumber) {
        this.ram = new Address[pageSize * pageNumber];
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
        this.pageTable = new TreeMap<>();
        this.frameAllocationRecord = new TreeMap<>();
        for (int page = 0; page < pageNumber; page++) {
            frameAllocationRecord.put(page * pageSize, false);
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
            throw new AddressingException("PID " + pid + " attempted to allocate more memory than available to the system");
        }

        //Check that there are enough free pages, swap out other processes if not
        for (Map.Entry<Integer, Boolean> entry : this.frameAllocationRecord.entrySet()) {
            if (!entry.getValue()) {
                freePages++;
            }
        }
        if (freePages < pages) {
            //TODO: Swap out processes
        }

        //Allocate pages
        int allocatedPages = 0;
        //Iterate over all frames
        for (Map.Entry<Integer, Boolean> entry : this.frameAllocationRecord.entrySet()) {
            //Check if frame is free
            if (!entry.getValue()) {
                //Allocate frame
                this.frameAllocationRecord.put(entry.getKey(), true);
                //Check a map for this process exists
                if (!this.pageTable.containsKey(pid)) {
                    this.pageTable.put(pid, new TreeMap<>());
                }
                //Add mapping to page table
                this.pageTable.get(pid).put(currentPages + allocatedPages, entry.getKey());
                //Break out of loop if done allocating
                allocatedPages++;
                if (allocatedPages == pages) {
                    break;
                }
            }
        }
    }

    public void free(int pid, int blocks) throws AddressingException {
        int pages = (int)Math.ceil((double)blocks / this.pageSize);

        //Check that process has enough pages allocated
        if (this.pageTable.get(pid) == null) {
            throw new AddressingException("PID "+ pid +" attempted to free more memory than it has allocated");
        }
        else if (this.pageTable.get(pid).size() < pages) {
            throw new AddressingException("PID "+ pid +" attempted to free more memory than it has allocated");
        }

        //Free pages from most to least recently allocated
        int size = this.pageTable.get(pid).size();
        for (int i = 1; i <= pages; i++) {
            this.frameAllocationRecord.put(this.pageTable.get(pid).get(size - i), false);
            this.pageTable.get(pid).remove(size - i);
        }
    }

    public void flushProcess(int pid) throws AddressingException {
        int blocks = this.pageTable.get(pid).size() * this.pageSize;
        this.free(pid, blocks);
    }

    public void swapOut(PCB pcb) {

    }

    public void swapIn(PCB pcb) {

    }
}
