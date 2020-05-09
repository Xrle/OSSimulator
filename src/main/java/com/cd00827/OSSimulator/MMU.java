package com.cd00827.OSSimulator;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MMU implements Runnable {
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
        this.mailbox = mailbox;
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
            Message message = this.mailbox.get(Mailbox.MMU);
            if (message != null) {
                String[] command = message.getCommand();
                switch (command[0]) {

                    //allocate [pid] [blocks] [swap order separated by :]
                    case "allocate": {
                        int pid = Integer.parseInt(command[1]);
                        int blocks = Integer.parseInt(command[2]);

                        //Parse swap order
                        LinkedList<Integer> swapOrder;
                        try {
                            swapOrder = Pattern.compile(":")
                                    .splitAsStream(command[3]).map(Integer::valueOf)
                                    .collect(Collectors.toCollection(LinkedList::new));
                        }
                        //If no swap order is provided, catch the exception and initialise an empty list
                        catch (ArrayIndexOutOfBoundsException e) {
                            swapOrder = new LinkedList<>();
                        }
                        boolean done = false;

                        //Allocate memory
                        while (!done) {
                            switch (this.allocate(pid, blocks)) {
                                //Success, unblock process
                                case 1:
                                    this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "unblock " + pid);
                                    done = true;
                                    break;

                                //Must free up memory and try again
                                case -1:
                                    Integer process = swapOrder.poll();
                                    if (process == null) {
                                        //There is enough memory in the system, but no processes are available to swap
                                        //Therefore do nothing, scheduler shouldn't mark a blocked process for execution
                                        done = true;
                                    } else {
                                        this.swapOut(process);
                                    }
                                    break;

                                //Not enough total system memory - drop the process
                                case -2:
                                    this.flushProcess(pid);
                                    this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "drop " + pid);
                                    //Break out of loop as nothing more can be done
                                    done = true;
                                    break;
                            }
                        }
                    }
                    break;

                    //free [pid] [blocks]
                    case "free": {
                        int pid = Integer.parseInt(command[1]);
                        int blocks = Integer.parseInt(command[2]);

                        if (this.free(pid, blocks) < 0) {
                            //Process has caused an error, so drop it
                            this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "drop " + pid);
                        }
                    }
                    break;
                }
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

    /**
     * Allocate memory to a process
     * @param pid PID of process
     * @param blocks Number of blocks to allocate
     * @return 1: Success<br>
     * -1: Not enough free memory<br>
     * -2: Tried to allocate more memory than available to the system<br>
     */
    public int allocate(int pid, int blocks) {
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
            return -2;
        }

        //Check that there is enough free memory
        for (Map.Entry<Integer, Boolean> entry : this.frameAllocationRecord.entrySet()) {
            if (!entry.getValue()) {
                freePages++;
            }
        }
        if (freePages < pages) {
            return -1;
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
        return 1;
    }

    /**
     * Free a process's memory
     * @param pid PID of process
     * @param blocks Number of blocks to free
     * @return 1: Success<br>
     *     -1: Attempted to free more memory than allocated<br>
     */
    public int free(int pid, int blocks) {
        int pages = (int)Math.ceil((double)blocks / this.pageSize);

        //Check that process has enough pages allocated
        if (this.pageTable.get(pid) == null) {
            return -1;
        }
        else if (this.pageTable.get(pid).size() < pages) {
            return -1;
        }

        //Free pages from most to least recently allocated
        int size = this.pageTable.get(pid).size();
        for (int i = 1; i <= pages; i++) {
            this.frameAllocationRecord.put(this.pageTable.get(pid).get(size - i), false);
            this.pageTable.get(pid).remove(size - i);
        }
        return 1;
    }

    public void flushProcess(int pid) {
        int blocks = this.pageTable.get(pid).size() * this.pageSize;
        //No error handling should be needed as method calculates memory to free using page table
        this.free(pid, blocks);
    }

    /**
     * Swap a process's memory out to a file.<br>
     * Will throw a RuntimeException if swapping fails, as an inability to swap will prevent the simulator from
     * functioning correctly.
     * @param pid PID of process to swap out
     */
    public void swapOut(int pid) {
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
            e.printStackTrace();
            throw new RuntimeException("FATAL: Swapping out PID " + pid + " failed, check you have r/w access to /swap");
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
