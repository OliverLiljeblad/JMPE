package com.JMPE.devices.via;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Via6522Test {
    private static final int ORA_OFFSET = 1 << 9;
    private static final int DDRA_OFFSET = 3 << 9;
    private static final int IFR_OFFSET = 13 << 9;
    private static final int IER_OFFSET = 14 << 9;

    @Test
    void surfacesPortAWritesThroughDirectionRegister() {
        AtomicInteger portA = new AtomicInteger(-1);
        Via6522 via = new Via6522(portA::set);

        via.writeRegister(DDRA_OFFSET, 0x10);
        via.writeRegister(ORA_OFFSET, 0x00);

        assertEquals(0xEF, portA.get());
    }

    @Test
    void interruptFlagRegisterSupportsReadPollClearAndRearm() {
        Via6522 via = new Via6522(ignored -> { });

        int initial = via.readRegister(IFR_OFFSET);
        via.writeRegister(IFR_OFFSET, 0x02);
        int afterClear = via.readRegister(IFR_OFFSET);
        int afterRearm = via.readRegister(IFR_OFFSET);

        assertAll(
            () -> assertEquals(0x02, initial),
            () -> assertEquals(0x00, afterClear),
            () -> assertEquals(0x02, afterRearm)
        );
    }

    @Test
    void interruptEnableRegisterControlsIrqSummaryBit() {
        Via6522 via = new Via6522(ignored -> { });

        via.writeRegister(IER_OFFSET, 0x82);

        int ifr = via.readRegister(IFR_OFFSET);
        int ier = via.readRegister(IER_OFFSET);

        assertAll(
            () -> assertEquals(0x82, ifr),
            () -> assertEquals(0x82, ier),
            () -> assertTrue(via.isIrqAsserted())
        );
    }

    @Test
    void irqLineDropsWhenEnabledInterruptFlagsAreCleared() {
        Via6522 via = new Via6522(ignored -> { });
        via.writeRegister(IER_OFFSET, 0x82);

        via.writeRegister(IFR_OFFSET, 0x02);

        assertFalse(via.isIrqAsserted());
    }
}
