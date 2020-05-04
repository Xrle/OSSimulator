package com.cd00827.OSSimulator;

public class Test {
    public static void main(String...args) {
        MMU mmu = new MMU(5, 5);
        try {
            /*
            mmu.allocate(1, 5);
            mmu.allocate(2, 5);
            mmu.allocate(1, 5);
            mmu.write(1, 1, 2);
            mmu.write(1, 2, true);
            mmu.write(1, 3, 5.5);
            mmu.write(1, 7, "hello");
            //mmu.swapOut(1);

             */
            mmu.swapIn(1);
        } catch (AddressingException e) {
            e.printStackTrace();
        }
    }
}
