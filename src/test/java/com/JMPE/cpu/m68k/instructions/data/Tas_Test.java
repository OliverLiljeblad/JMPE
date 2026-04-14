package com.JMPE.cpu.m68k.instructions.data;

import com.JMPE.cpu.m68k.StatusRegister;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Tas_Test {
    @Test
    void testsByteAndSetsHighBitWithoutTouchingExtend() {
        StatusRegister statusRegister = new StatusRegister();
        statusRegister.setExtend(true);
        AtomicInteger writtenValue = new AtomicInteger(-1);

        int cycles = Tas.execute(() -> 0x01, writtenValue::set, statusRegister.moveConditionCodes());

        assertAll(
            () -> assertEquals(Tas.EXECUTION_CYCLES, cycles),
            () -> assertEquals(0x81, writtenValue.get()),
            () -> assertFalse(statusRegister.isNegativeSet()),
            () -> assertFalse(statusRegister.isZeroSet()),
            () -> assertFalse(statusRegister.isOverflowSet()),
            () -> assertFalse(statusRegister.isCarrySet()),
            () -> assertTrue(statusRegister.isExtendSet())
        );
    }

    @Test
    void zeroInputSetsZeroFlagButStillStoresBitSeven() {
        StatusRegister statusRegister = new StatusRegister();
        AtomicInteger writtenValue = new AtomicInteger(-1);

        Tas.execute(() -> 0x00, writtenValue::set, statusRegister.moveConditionCodes());

        assertAll(
            () -> assertEquals(0x80, writtenValue.get()),
            () -> assertFalse(statusRegister.isNegativeSet()),
            () -> assertTrue(statusRegister.isZeroSet())
        );
    }

    @Test
    void rejectsNullInputs() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> Tas.execute(null, value -> { }, new StatusRegister().moveConditionCodes())),
            () -> assertThrows(NullPointerException.class, () -> Tas.execute(() -> 0, null, new StatusRegister().moveConditionCodes())),
            () -> assertThrows(NullPointerException.class, () -> Tas.execute(() -> 0, value -> { }, null))
        );
    }
}
