package com.cd00827.OSSimulator;

import java.nio.file.Path;

/**
 * Process Control Block, stores information about a process
 * @author cd00827
 */
public class PCB {
    private final int pid;
    private int codeLength;
    private final Path codePath;
    private boolean loaded;
    private boolean swapped;
    private final int quantum;
    private int timeLeft;
    public int pc;

    /**
     * Constructor
     * @param pid PID of process
     * @param codePath Path to the process' code
     * @param quantum Number of cycles this process will run in the scheduler for
     */
    public PCB(int pid, Path codePath, int quantum) {
        this.pid = pid;
        this.codePath = codePath;
        this.loaded = false;
        this.swapped = false;
        this.quantum = quantum;
        this.timeLeft = quantum;
        this.pc = 0;
    }

    /**
     * Get the length of this process's code
     * @return Length of code
     */
    public int getCodeLength() {
        return codeLength;
    }

    /**
     * Set the length of this process's code
     */
    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    /**
     * Get this process' PID
     * @return PID
     */
    public int getPid() {
        return pid;
    }

    /**
     * Get the path to this process' code
     * @return Path to code
     */
    public Path getCodePath() {
        return this.codePath;
    }

    /**
     * Mark this process as loaded
     */
    public void setLoaded() {
        this.loaded = true;
    }

    /**
     * Check if this process is loaded
     * @return True if loaded
     */
    public boolean isLoaded() {
        return this.loaded;
    }

    /**
     * Set the swapped status of this process
     * @param status True if process is currently swapped out of memory
     */
    public void setSwapped(boolean status) {
        this.swapped = status;
    }

    /**
     * Check if this process has been swapped out
     * @return True if swapped out
     */
    public boolean isSwapped() {
        return this.swapped;
    }

    /**
     * Decrement the number of scheduler cycles remaining on this process, resetting if it hits 0
     * @return True if 0 was reached
     */
    public boolean decrement() {
        this.timeLeft--;
        if (this.timeLeft == 0) {
            this.timeLeft = this.quantum;
            return true;
        }
        else {
            return false;
        }
    }
}
