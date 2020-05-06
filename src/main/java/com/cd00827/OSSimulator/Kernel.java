package com.cd00827.OSSimulator;

public class Kernel {
    private Mailbox mailbox;
    private Thread mmu;

    public Kernel(int pageSize, int pageNumber, int memoryClock) {
        this.mailbox = new Mailbox();
        this.mmu =new Thread(new MMU(pageSize, pageNumber, memoryClock, this.mailbox));
    }

    public void boot() {
        this.mmu.start();
    }
}
