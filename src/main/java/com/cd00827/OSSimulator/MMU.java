package com.cd00827.OSSimulator;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class MMU implements Runnable {
    private static final String MAILBOX_LABEL = "MMU";
    private final Address[] ram;
    private final int pageSize;
    private final int pageNumber;
    //Map pid to a map of page number to frame offset
    private final Map<Integer, Map<Integer, Integer>> pageTable;
    //Keep a record of allocated frames
    private final Map<Integer, Boolean> frameAllocationRecord;
    private Mailbox mailbox;
    private final double clockSpeed;

    public MMU(int pageSize, int pageNumber, double clockSpeed, Mailbox mailbox) {
        this.ram = new Address[pageSize * pageNumber];
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
        this.clockSpeed = clockSpeed;
        this.pageTable = new TreeMap<>();
        this.frameAllocationRecord = new TreeMap<>();
        for (int page = 0; page < pageNumber; page++) {
            frameAllocationRecord.put(page * pageSize, false);
        }
    }

    @Override
    public void run() {
        while (true) {
            //Get next command
            Message message = this.mailbox.get(MAILBOX_LABEL);
            String[] command = message.getCommand();
            switch (command[0]) {
                //allocate [pid] [swap order separated by :]
                case "":
            }

            //Wait for next clock cycle
            try {
                Thread.sleep((long) (clockSpeed / 1000));
            }
            catch (InterruptedException e) {
                return;
            }
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
            throw new AddressingException("Not enough free memory to complete allocation request for PID " + pid);
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

    public void swapOut(int pid) throws AddressingException {
        File dir = new File("swap");
        File file = new File("swap", pid + ".txt");
        try {
            if (!dir.exists()) {
                Files.createDirectory(dir.toPath());
            }
            Files.deleteIfExists(file.toPath());
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            for (Map.Entry<Integer, Integer> page : this.pageTable.get(pid).entrySet()) {
                for (int i = 0; i < this.pageSize; i++) {
                    if (this.ram[page.getValue() + i] != null) {
                        writer.write(this.ram[page.getValue() + i].toString());
                    }
                    writer.newLine();
                }
            }
            writer.close();
            this.flushProcess(pid);
        }
        catch (Exception e){
            throw new AddressingException("An error occurred swapping out PID " + pid + ": " + e.getMessage());
        }
    }

    public void swapIn(int pid) throws AddressingException {
        File file = new File("swap", pid + ".txt");
        try {
            //Get required memory
            Stream<String> stream = Files.lines(file.toPath());
            int blocks = (int)stream.count();
            stream.close();
            this.allocate(pid, blocks);

            //Load data into memory
            BufferedReader reader = new BufferedReader(new FileReader(file));
            for (int i = 0; i < blocks; i++) {
                String line = reader.readLine();
                if (!line.trim().isEmpty()) {
                    String[] split = line.split("::", 2);
                    switch (split[0]) {
                        case "STRING":
                            this.write(pid, i, split[1]);
                            break;

                        case "INT":
                            this.write(pid, i, Integer.parseInt(split[1]));
                            break;

                        case "DOUBLE":
                            this.write(pid, i, Double.parseDouble(split[1]));
                            break;

                        case "BOOL":
                            this.write(pid, i, Boolean.parseBoolean(split[1]));
                            break;
                    }
                }
            }
            reader.close();
        }
        catch (Exception e) {
            throw new AddressingException("An error occurred swapping in PID " + pid + ": " + e.getMessage());
        }
    }
}
