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
        try {
            Thread.sleep(1500);
            mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "allocate 1 5");
            Thread.sleep(1500);
            mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "allocate 2 20 ");
            Thread.sleep(1500);
            mailbox.put(Mailbox.SCHEDULER, Mailbox.MMU, "allocate 1 15 2");
            Thread.sleep(1500);
            this.mmu.join();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
