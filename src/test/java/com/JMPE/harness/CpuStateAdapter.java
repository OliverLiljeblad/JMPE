package com.JMPE.harness;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Mmio;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.dispatch.DispatchTable;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CpuStateAdapter {
    private static final int ADDRESS_MASK = 0x00FF_FFFF;
    private static final int FULL_ADDRESS_SPACE_SIZE = 0x0100_0000;
    private static final DispatchTable DISPATCH_TABLE = new DispatchTable();

    private CpuStateAdapter() {
    }

    public static RunResult execute(SingleStepLoader.CaseSpec caseSpec) {
        Objects.requireNonNull(caseSpec, "caseSpec must not be null");

        SparseMemory sparseMemory = new SparseMemory();
        AddressSpace bus = new AddressSpace();
        bus.addRegion(Mmio.readWrite(0, FULL_ADDRESS_SPACE_SIZE, sparseMemory::readByte, sparseMemory::writeByte));

        M68kCpu cpu = new M68kCpu();
        applyInitialState(cpu, bus, caseSpec.testCase().initial());

        String[] lastLog = new String[1];
        try {
            M68kCpu.StepReport report = cpu.step(bus, DISPATCH_TABLE, message -> lastLog[0] = message);
            return new RunResult(cpu, bus, report, lastLog[0]);
        } catch (IllegalInstructionException | RuntimeException exception) {
            throw new AssertionError(
                "Single-step case '" + caseSpec.displayName() + "' failed at pc="
                    + String.format("0x%08X", cpu.registers().programCounter())
                    + ". Last log: " + (lastLog[0] == null ? "<none>" : lastLog[0]),
                exception
            );
        }
    }

    public static void assertArchitectedState(SingleStepLoader.CaseSpec caseSpec, RunResult result) {
        SingleStepLoader.Snapshot expected = caseSpec.testCase().finalState();
        M68kCpu cpu = result.cpu();
        M68kCpu.StepReport report = result.report();
        String lastLog = result.lastLog();

        assertTrue(report.success(), "Single-step report was not successful for " + caseSpec.displayName());
        if (SingleStepLoader.compareCycles()) {
            assertEquals(caseSpec.testCase().length(), report.cycles(),
                DiffPrinter.cycleMismatch(caseSpec, caseSpec.testCase().length(), report.cycles(), lastLog));
        }

        assertAll(
            () -> assertEquals(expected.d0(), unsigned(cpu.registers().data(0)),
                DiffPrinter.cpuMismatch(caseSpec, "D0", expected.d0(), unsigned(cpu.registers().data(0)), report, lastLog)),
            () -> assertEquals(expected.d1(), unsigned(cpu.registers().data(1)),
                DiffPrinter.cpuMismatch(caseSpec, "D1", expected.d1(), unsigned(cpu.registers().data(1)), report, lastLog)),
            () -> assertEquals(expected.d2(), unsigned(cpu.registers().data(2)),
                DiffPrinter.cpuMismatch(caseSpec, "D2", expected.d2(), unsigned(cpu.registers().data(2)), report, lastLog)),
            () -> assertEquals(expected.d3(), unsigned(cpu.registers().data(3)),
                DiffPrinter.cpuMismatch(caseSpec, "D3", expected.d3(), unsigned(cpu.registers().data(3)), report, lastLog)),
            () -> assertEquals(expected.d4(), unsigned(cpu.registers().data(4)),
                DiffPrinter.cpuMismatch(caseSpec, "D4", expected.d4(), unsigned(cpu.registers().data(4)), report, lastLog)),
            () -> assertEquals(expected.d5(), unsigned(cpu.registers().data(5)),
                DiffPrinter.cpuMismatch(caseSpec, "D5", expected.d5(), unsigned(cpu.registers().data(5)), report, lastLog)),
            () -> assertEquals(expected.d6(), unsigned(cpu.registers().data(6)),
                DiffPrinter.cpuMismatch(caseSpec, "D6", expected.d6(), unsigned(cpu.registers().data(6)), report, lastLog)),
            () -> assertEquals(expected.d7(), unsigned(cpu.registers().data(7)),
                DiffPrinter.cpuMismatch(caseSpec, "D7", expected.d7(), unsigned(cpu.registers().data(7)), report, lastLog)),
            () -> assertEquals(expected.a0(), unsigned(cpu.registers().address(0)),
                DiffPrinter.cpuMismatch(caseSpec, "A0", expected.a0(), unsigned(cpu.registers().address(0)), report, lastLog)),
            () -> assertEquals(expected.a1(), unsigned(cpu.registers().address(1)),
                DiffPrinter.cpuMismatch(caseSpec, "A1", expected.a1(), unsigned(cpu.registers().address(1)), report, lastLog)),
            () -> assertEquals(expected.a2(), unsigned(cpu.registers().address(2)),
                DiffPrinter.cpuMismatch(caseSpec, "A2", expected.a2(), unsigned(cpu.registers().address(2)), report, lastLog)),
            () -> assertEquals(expected.a3(), unsigned(cpu.registers().address(3)),
                DiffPrinter.cpuMismatch(caseSpec, "A3", expected.a3(), unsigned(cpu.registers().address(3)), report, lastLog)),
            () -> assertEquals(expected.a4(), unsigned(cpu.registers().address(4)),
                DiffPrinter.cpuMismatch(caseSpec, "A4", expected.a4(), unsigned(cpu.registers().address(4)), report, lastLog)),
            () -> assertEquals(expected.a5(), unsigned(cpu.registers().address(5)),
                DiffPrinter.cpuMismatch(caseSpec, "A5", expected.a5(), unsigned(cpu.registers().address(5)), report, lastLog)),
            () -> assertEquals(expected.a6(), unsigned(cpu.registers().address(6)),
                DiffPrinter.cpuMismatch(caseSpec, "A6", expected.a6(), unsigned(cpu.registers().address(6)), report, lastLog)),
            () -> assertEquals(expected.usp(), unsigned(cpu.registers().userStackPointer()),
                DiffPrinter.cpuMismatch(caseSpec, "USP", expected.usp(), unsigned(cpu.registers().userStackPointer()), report, lastLog)),
            () -> assertEquals(expected.ssp(), unsigned(cpu.registers().supervisorStackPointer()),
                DiffPrinter.cpuMismatch(caseSpec, "SSP", expected.ssp(), unsigned(cpu.registers().supervisorStackPointer()), report, lastLog)),
            () -> assertEquals(expected.sr() & 0xFFFF, cpu.statusRegister().rawValue(),
                DiffPrinter.wordMismatch(caseSpec, "SR", expected.sr(), cpu.statusRegister().rawValue(), report, lastLog)),
            () -> assertEquals(expected.pc(), unsigned(cpu.registers().programCounter()),
                DiffPrinter.cpuMismatch(caseSpec, "PC", expected.pc(), unsigned(cpu.registers().programCounter()), report, lastLog)),
            () -> assertEquals(activeStackPointer(expected), unsigned(cpu.registers().stackPointer()),
                DiffPrinter.cpuMismatch(caseSpec, "A7", activeStackPointer(expected), unsigned(cpu.registers().stackPointer()), report, lastLog))
        );
    }

    private static void applyInitialState(M68kCpu cpu, AddressSpace bus, SingleStepLoader.Snapshot state) {
        loadMemory(bus, state.ram());
        loadPrefetch(bus, state.pc(), state.prefetch());

        cpu.registers().setData(0, (int) state.d0());
        cpu.registers().setData(1, (int) state.d1());
        cpu.registers().setData(2, (int) state.d2());
        cpu.registers().setData(3, (int) state.d3());
        cpu.registers().setData(4, (int) state.d4());
        cpu.registers().setData(5, (int) state.d5());
        cpu.registers().setData(6, (int) state.d6());
        cpu.registers().setData(7, (int) state.d7());
        cpu.registers().setAddress(0, (int) state.a0());
        cpu.registers().setAddress(1, (int) state.a1());
        cpu.registers().setAddress(2, (int) state.a2());
        cpu.registers().setAddress(3, (int) state.a3());
        cpu.registers().setAddress(4, (int) state.a4());
        cpu.registers().setAddress(5, (int) state.a5());
        cpu.registers().setAddress(6, (int) state.a6());
        cpu.registers().setUserStackPointer((int) state.usp());
        cpu.registers().setSupervisorStackPointer((int) state.ssp());
        cpu.statusRegister().setRawValue(state.sr());
        cpu.registers().setProgramCounter((int) state.pc());
    }

    private static void loadMemory(AddressSpace bus, int[][] ram) {
        if (ram == null) {
            return;
        }

        for (int[] cell : ram) {
            if (cell == null || cell.length != 2) {
                throw new IllegalArgumentException("Single-step RAM cell must contain [address, byte]");
            }
            bus.writeByte(cell[0], cell[1]);
        }
    }

    private static void loadPrefetch(AddressSpace bus, long pc, int[] prefetch) {
        if (prefetch == null) {
            return;
        }

        int address = (int) pc;
        for (int index = 0; index < prefetch.length; index++) {
            writeWordBytes(bus, address + (index * 2), prefetch[index]);
        }
    }

    private static void writeWordBytes(AddressSpace bus, int address, int value) {
        bus.writeByte(address, value >>> 8);
        bus.writeByte(address + 1, value);
    }

    private static long activeStackPointer(SingleStepLoader.Snapshot state) {
        return ((state.sr() & (1 << 13)) != 0) ? state.ssp() : state.usp();
    }

    private static long unsigned(int value) {
        return value & 0xFFFF_FFFFL;
    }

    private static final class SparseMemory {
        private final Map<Integer, Integer> bytes = new HashMap<>();

        int readByte(int offset) {
            return bytes.getOrDefault(offset & ADDRESS_MASK, 0);
        }

        void writeByte(int offset, int value) {
            bytes.put(offset & ADDRESS_MASK, value & 0xFF);
        }
    }

    public record RunResult(M68kCpu cpu, AddressSpace bus, M68kCpu.StepReport report, String lastLog) {
    }
}
