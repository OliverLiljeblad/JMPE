package com.JMPE.cpu.m68k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class M68kCpuStepReportTest {
    @Test
    void executeStepWithReportLogsSuccessAndStateTransitions() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0040_0100);
        cpu.registers().setData(0, 0x0000_0001);
        cpu.statusRegister().setZero(false);
        List<String> logs = new ArrayList<>();

        M68kCpu.StepReport report = cpu.executeStepWithReport(
            "ADDQ.B #1,D0",
            () -> {
                int result = (cpu.registers().data(0) + 1) & 0xFF;
                cpu.registers().setData(0, result);
                cpu.registers().setProgramCounter(cpu.registers().programCounter() + 2);
                cpu.statusRegister().setZero(result == 0);
                cpu.statusRegister().setNegative((result & 0x80) != 0);
                cpu.statusRegister().setOverflow(false);
                cpu.statusRegister().setCarry(false);
            },
            logs::add
        );

        assertTrue(report.success());
        assertEquals(0x0000_0001, report.before().dataRegister(0));
        assertEquals(0x0000_0002, report.after().dataRegister(0));
        assertEquals(0x0040_0100, report.before().programCounter());
        assertEquals(0x0040_0102, report.after().programCounter());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] OK op=ADDQ.B #1,D0"));
        assertTrue(logs.get(0).contains("pc=0x00400100->0x00400102"));
    }

    @Test
    void executeStepWithReportLogsFailureAndRethrows() {
        M68kCpu cpu = new M68kCpu();
        cpu.registers().setProgramCounter(0x0040_0200);
        cpu.registers().setData(0, 0x1111_1111);
        List<String> logs = new ArrayList<>();

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> cpu.executeStepWithReport("MOVE.B D0,D1", () -> {
                cpu.registers().setProgramCounter(cpu.registers().programCounter() + 2);
                throw new IllegalStateException("decode mismatch");
            }, logs::add)
        );

        assertEquals("decode mismatch", thrown.getMessage());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[m68k-step] ERR op=MOVE.B D0,D1"));
        assertTrue(logs.get(0).contains("error=\"decode mismatch\""));
        assertTrue(logs.get(0).contains("pc=0x00400200->0x00400202"));
    }

    @Test
    void executeStepWithReportRejectsNullInputs() {
        M68kCpu cpu = new M68kCpu();
        assertThrows(NullPointerException.class, () -> cpu.executeStepWithReport(null, () -> {
        }, ignored -> {
        }));
        assertThrows(NullPointerException.class, () -> cpu.executeStepWithReport("NOP", null, ignored -> {
        }));
        assertThrows(NullPointerException.class, () -> cpu.executeStepWithReport("NOP", () -> {
        }, null));
    }

    @Test
    void snapshotValidatesRegisterIndices() {
        M68kCpu cpu = new M68kCpu();
        M68kCpu.StepReport report = cpu.executeStepWithReport("NOP", () -> {
        }, message -> {
        });

        assertNotNull(report);
        assertFalse(report.before().statusRegister() != 0 && report.before().conditionCodeRegister() == 0);
        assertThrows(IllegalArgumentException.class, () -> report.after().dataRegister(8));
        assertThrows(IllegalArgumentException.class, () -> report.after().addressRegister(8));
    }
}
