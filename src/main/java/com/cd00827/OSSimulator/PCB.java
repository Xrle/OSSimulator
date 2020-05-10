package com.cd00827.OSSimulator;

import java.io.File;
import java.nio.file.Path;

public class PCB {
    private int pid;
    private int pc;
    private int codeLength;
    private File code;
    private boolean loaded;
    private boolean swapped;
    private int quantum;
    private int timeLeft;

    public PCB(int pid, String codePath, int quantum) {
        this.pid = pid;
        this.code = new File(codePath);
        this.loaded = false;
        this.swapped = false;
        this.quantum = quantum;
        this.timeLeft = quantum;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
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
        return this.code.toPath();
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
