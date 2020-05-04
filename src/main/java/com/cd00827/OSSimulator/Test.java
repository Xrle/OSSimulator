package com.cd00827.OSSimulator;

public class Test {
    public static void main(String args[]) {
        MMU mmu = new MMU(5, 5);
        try {
            mmu.allocate(1, 7);
        } catch (AddressingException e) {
            e.printStackTrace();
        }
    }
}
