package com.cd00827.OSSimulator;

public class Test {
    public static void main(String...args) {
        MMU mmu = new MMU(5, 5);
        try {
            mmu.allocate(1, 7);
            mmu.write(1, 1,  "Test");
            mmu.allocate(2, 3);
            mmu.write(2, 3, true);
            mmu.allocate(1, 5);
            mmu.write(1, 13, 1.8);
            mmu.allocate(3,26);
        } catch (AddressingException e) {
            e.printStackTrace();
        }
    }
}
