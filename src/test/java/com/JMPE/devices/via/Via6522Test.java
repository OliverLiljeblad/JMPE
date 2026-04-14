package com.JMPE.devices.via;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Via6522Test {
    @Test
    void surfacesPortAWritesThroughDirectionRegister() {
        AtomicInteger portA = new AtomicInteger(-1);
        Via6522 via = new Via6522(portA::set);

        via.writeRegister(3, 0x10);
        via.writeRegister(1, 0x00);

        assertEquals(0xEF, portA.get());
    }

    @Test
    void interruptFlagRegisterSupportsReadPollClearAndRearm() {
        Via6522 via = new Via6522(ignored -> { });

        int initial = via.readRegister(13);
        via.writeRegister(13, 0x02);
        int afterClear = via.readRegister(13);
        int afterRearm = via.readRegister(13);

        assertAll(
            () -> assertEquals(0x02, initial),
            () -> assertEquals(0x00, afterClear),
            () -> assertEquals(0x02, afterRearm)
        );
    }

    @Test
    void interruptEnableRegisterControlsIrqSummaryBit() {
        Via6522 via = new Via6522(ignored -> { });

        via.writeRegister(14, 0x82);

        int ifr = via.readRegister(13);
        int ier = via.readRegister(14);

        assertAll(
            () -> assertEquals(0x82, ifr),
            () -> assertEquals(0x82, ier)
        );
    }
}
