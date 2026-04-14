package com.JMPE.cpu.m68k.instructions.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Reset_Test {
    @Test
    void returnsExecutionCyclesWithoutOtherCpuVisibleEffects() {
        assertEquals(Reset.EXECUTION_CYCLES, Reset.execute());
    }
}
