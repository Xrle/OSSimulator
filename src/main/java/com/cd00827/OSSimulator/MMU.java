package com.cd00827.OSSimulator;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Memory management unit.<br>
 * Maintains an array representing physical RAM, and allocates memory to processes using a paging system.
 * Allows for processes to be swapped out to text files when memory is full.
 * Provides read and write access to memory.<br>
 * Produces:<br>
 *     SCHEDULER => unblock [pid]<br>
 *     SCHEDULER => swappedOut [pid]<br>
 *     SCHEDULER => swappedIn [pid]<br>
 *     SCHEDULER => skip [pid]<br>
 *     SCHEDULER => drop [pid]<br>
 *     SCHEDULER => allocated [pid]<br>
 *     [PID] => data [type] [data] [final]<br>
 *
 * Consumes:<br>
 *     MMU => allocate [pid] [blocks] {swap order x:y:x}<br>
 *     MMU => free [pid] [blocks]<br>
 *     MMU => swapIn [pid]<br>
 *     MMU => read [pid] [address] [final]<br>
 *     MMU => write [pid] [address] [type] [data] [final]<br>
 *     MMU => drop [pid]<br>
 *
 * @author cd00827
 */
public class MMU implements Runnable {
    private final Address[] ram;
    private final int pageSize;
    private final int pageNumber;
    //Map pid to a map of page number to frame offset
    private final Map<Integer, Map<Integer, Integer>> pageTable;
    //Keep a record of allocated frames
    private final Map<Integer, Boolean> frameAllocationRecord;
    private final Mailbox mailbox;
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

                    //allocate [pid] [blocks] [loading] {swap order x:y:z}
                    case "allocate": {
                        int pid = Integer.parseInt(command[1]);
                        int blocks = Integer.parseInt(command[2]);
                        boolean loading = Boolean.parseBoolean(command[3]);

                        //Parse swap order
                        Queue<Integer> swapOrder;
                        try {
                            swapOrder = Pattern.compile(":")
                                    .splitAsStream(command[4]).map(Integer::valueOf)
                                    .collect(Collectors.toCollection(ArrayDeque::new));
                        }
                        //If no swap order is provided, catch the exception and initialise an empty list
                        catch (ArrayIndexOutOfBoundsException e) {
                            swapOrder = new ArrayDeque<>();
                        }
                        boolean done = false;

                        //Allocate memory
                        while (!done) {
                            switch (this.allocate(pid, blocks)) {
                                //Success, unblock process
                                case 1:
                                    if (loading) {
                                        this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "allocated " + pid);
                                    }
                                    else {
                                        this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "unblock " + pid);
                                    }
                                    done = true;
                                    break;

                                //Must free up memory and try again
                                case -1:
                                    Integer process = swapOrder.poll();
                                    if (process == null) {
                                        //There is enough memory in the system, but no processes are available to swap
                                        //Tell scheduler to skip this process
                                        this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "skip " + pid);
                                        done = true;
                                    } else {
                                        //Swap out process and notify scheduler
                                        this.swapOut(process);
                                        this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "swappedOut " + pid);
                                    }
                                    break;

                                //Not enough total system memory - drop the process
                                case -2:
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

                        if (!this.free(pid, blocks)) {
                            //Process has caused an error, so drop it
                            this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "drop " + pid);
                        }
                    }
                    break;

                    //swapIn [pid]
                    case "swapIn": {
                        int pid = Integer.parseInt(command[1]);
                        //If there is enough memory to swap in process, do it and notify scheduler.
                        //Otherwise tell scheduler to skip this process
                        if (this.swapIn(pid)) {
                            this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "swappedOut " + pid);
                        }
                        else {
                            this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "skip " + pid);
                        }
                    }
                    break;

                    //read [pid] [address] [final]
                    case "read": {
                        int pid = Integer.parseInt(command[1]);
                        int address = Integer.parseInt(command[2]);
                        String[] data = this.read(pid, address);
                        //If read is successful, send data to whatever requested it, otherwise drop the process
                        if (data[0].equals("success")) {
                            this.mailbox.put(Mailbox.MMU, message.getSender(), "data " + data[1] + " " + data[2]);
                            //Unblock process if this was the final read operation
                            if (Boolean.parseBoolean(command[3])) {
                                this.mailbox.put(Mailbox.MMU, message.getSender(), "data " + data[1] + " " + data[2] + "true");
                                this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "unblock " + pid);
                            }
                            else {
                                this.mailbox.put(Mailbox.MMU, message.getSender(), "data " + data[1] + " " + data[2] + "false");
                            }
                        }
                        //Drop process if write causes an error
                        else {
                            this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "drop " + pid);
                        }
                    }
                    break;

                    //write [pid] [address] [type] [data] [final]
                    case "write": {
                        int pid = Integer.parseInt(command[1]);
                        int address = Integer.parseInt(command[2]);
                        String type = command[3];
                        String data = command[4];
                        if (this.write(pid, address, type, data)) {
                            //Unblock process if this was the final write operation
                            if (Boolean.parseBoolean(command[5])) {
                                this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "unblock " + pid);
                            }
                        }
                        else {
                            this.mailbox.put(Mailbox.MMU, Mailbox.SCHEDULER, "drop " + pid);
                        }
                    }
                    break;

                    //drop [pid]
                    case "drop": {
                        int pid = Integer.parseInt(command[1]);
                        this.flushProcess(pid);
                    }

                    break;
                }
            }

            //Wait for next clock cycle
            try {
                Thread.sleep((long)(clockSpeed * 1000));
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * Read from a virtual address
     * @param pid PID of process
     * @param address Virtual address to read
     * @return [status, type, data]<br>
     *     Status is either "success" or "error"
     */
    public String[] read(int pid, int address) {
        int page = address / this.pageSize;
        int offset = address % this.pageSize;
        //Check process has access to address
        if (this.pageTable.get(pid).containsKey(page)) {
            try {
                //Return data
                String[] value = this.ram[this.pageTable.get(pid).get(page) + offset].read();
                return new String[] {"success", value[0], value[1]};
            }
            //Catch exception if address is null
            catch (NullPointerException e) {
                return new String[] {"error", "null", "null"};
            }
        }
        return new String[] {"error", "null", "null"};
    }

    /**
     * Write to virtual address
     * @param pid PID of process
     * @param address Virtual address to write to
     * @param type Type of data to write
     * @param data Data to write
     * @return True if successful, false if process does not have access to requested address
     */
    public boolean write(int pid, int address, String type, String data) {
        int page = address / this.pageSize;
        int offset = address % this.pageSize;
        if (this.pageTable.get(pid).containsKey(page)) {
            this.ram[this.pageTable.get(pid).get(page) + offset] = new Address(type, data);
            return true;
        }
        return false;
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
     * @return True: Success<br>
     *     False: Attempted to free more memory than allocated<br>
     */
    public boolean free(int pid, int blocks) {
        int pages = (int)Math.ceil((double)blocks / this.pageSize);

        //Check that process has enough pages allocated
        if (this.pageTable.get(pid) == null) {
            return false;
        }
        else if (this.pageTable.get(pid).size() < pages) {
            return false;
        }

        //Free pages from most to least recently allocated
        int size = this.pageTable.get(pid).size();
        for (int i = 1; i <= pages; i++) {
            this.frameAllocationRecord.put(this.pageTable.get(pid).get(size - i), false);
            this.pageTable.get(pid).remove(size - i);
        }
        return true;
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

    /**
     * Swap a process's memory in from a file.<br>
     * Will throw a RuntimeException if opening the swap file fails, as this will prevent the simulator from running
     * properly and is likely caused by incorrect permissions
     * @param pid PID of process to swap in
     * @return True: Success<br>
     *     False: Could not allocate enough memory<br>
     */
    public boolean swapIn(int pid) {
        File file = new File("swap", pid + ".txt");
        try {
            //Get required memory
            Stream<String> stream = Files.lines(file.toPath());
            int blocks = (int)stream.count();
            stream.close();
            if (this.allocate(pid, blocks) < 0) {
                //Failed to allocate enough memory.
                //Case where there is not enough total system memory can be ignored, as if that was the case
                //the process would never have existed in memory to be swapped out.
                return false;
            }

            //Load data into memory
            BufferedReader reader = new BufferedReader(new FileReader(file));
            for (int i = 0; i < blocks; i++) {
                String line = reader.readLine();
                if (!line.trim().isEmpty()) {
                    String[] split = line.split("::", 2);
                    this.write(pid, i, split[0], split[1]);
                }
            }
            reader.close();
        }
        catch (IOException e) {
            throw new RuntimeException("FATAL: Swapping in PID " + pid + " failed, check you have r/w access to /swap");
        }
        return true;
    }
}
