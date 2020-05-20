package com.cd00827.OSSimulator;

import java.nio.file.Path;

public class PCB {
    private int pid;
    private int codeLength;
    private Path codePath;
    private boolean loaded;
    private boolean swapped;
    private int quantum;
    private int timeLeft;
    public int pc;

    public PCB(int pid, Path codePath, int quantum) {
        this.pid = pid;
        this.codePath = codePath;
        this.loaded = false;
        this.swapped = false;
        this.quantum = quantum;
        this.timeLeft = quantum;
        this.pc = 0;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public int getPid() {
        return pid;
    }

    public Path getCodePath() {
        return this.codePath;
    }

    public void setLoaded() {
        this.loaded = true;
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public void setSwapped(boolean status) {
        this.swapped = status;
    }

    public boolean isSwapped() {
        return this.swapped;
    }

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
