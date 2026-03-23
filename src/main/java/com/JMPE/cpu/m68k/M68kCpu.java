package com.JMPE.cpu.m68k;

import com.JMPE.bus.Rom;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Minimal CPU shell used to wire core components during early emulator milestones.
 */
public final class M68kCpu {
    private static final int RESET_INTERRUPT_MASK = 7;

    private final Registers registers;
    private final StatusRegister statusRegister;

    public M68kCpu() {
        this(new Registers(), new StatusRegister());
    }

    public M68kCpu(StatusRegister statusRegister) {
        this(new Registers(), statusRegister);
    }

    public M68kCpu(Registers registers, StatusRegister statusRegister) {
        if (registers == null) {
            throw new IllegalArgumentException("registers must not be null");
        }
        if (statusRegister == null) {
            throw new IllegalArgumentException("statusRegister must not be null");
        }
        this.registers = registers;
        this.statusRegister = statusRegister;
    }

    public Registers registers() {
        return registers;
    }

    public StatusRegister statusRegister() {
        return statusRegister;
    }

    /**
     * Applies 68000 reset bootstrap state from ROM vectors.
     * <p>
     * On reset, the CPU loads SSP and PC from vector table offsets 0 and 4, enters supervisor mode,
     * clears trace, and masks interrupts to level 7.
     * </p>
     */
    public void resetFromRom(Rom rom) {
        if (rom == null) {
            throw new IllegalArgumentException("rom must not be null");
        }

        registers.setStackPointer(rom.initialSupervisorStackPointer());
        registers.setProgramCounter(rom.initialProgramCounter());

        statusRegister.setRawValue(0);
        statusRegister.setSupervisor(true);
        statusRegister.setTrace(false);
        statusRegister.setInterruptMask(RESET_INTERRUPT_MASK);
    }

    /**
     * Executes one decoded instruction callback, producing a deterministic register/flag report.
     *
     * <p>
     * This is intentionally narrow and focused on current pipeline needs: it snapshots visible CPU
     * state before and after the step, emits one scan-friendly log line, and rethrows failures so CI
     * still fails fast while preserving diagnostics.
     * </p>
     */
    public StepReport executeStepWithReport(String instructionName,
                                            StepExecutor stepExecutor,
                                            Consumer<String> reporter) {
        Objects.requireNonNull(instructionName, "instructionName must not be null");
        Objects.requireNonNull(stepExecutor, "stepExecutor must not be null");
        Objects.requireNonNull(reporter, "reporter must not be null");

        StepSnapshot before = StepSnapshot.capture(registers, statusRegister);
        try {
            stepExecutor.execute();
            StepSnapshot after = StepSnapshot.capture(registers, statusRegister);
            StepReport report = StepReport.success(instructionName, before, after);
            reporter.accept(report.toLogLine());
            return report;
        } catch (RuntimeException exception) {
            StepSnapshot after = StepSnapshot.capture(registers, statusRegister);
            StepReport report = StepReport.failure(instructionName, before, after, exception.getMessage());
            reporter.accept(report.toLogLine());
            throw exception;
        }
    }

    public StepReport executeStepWithConsoleReport(String instructionName, StepExecutor stepExecutor) {
        return executeStepWithReport(instructionName, stepExecutor, System.out::println);
    }

    @FunctionalInterface
    public interface StepExecutor {
        void execute();
    }

    public static final class StepReport {
        private final String instructionName;
        private final boolean success;
        private final StepSnapshot before;
        private final StepSnapshot after;
        private final String errorMessage;

        private StepReport(String instructionName,
                           boolean success,
                           StepSnapshot before,
                           StepSnapshot after,
                           String errorMessage) {
            this.instructionName = instructionName;
            this.success = success;
            this.before = before;
            this.after = after;
            this.errorMessage = errorMessage;
        }

        static StepReport success(String instructionName, StepSnapshot before, StepSnapshot after) {
            return new StepReport(instructionName, true, before, after, null);
        }

        static StepReport failure(String instructionName,
                                  StepSnapshot before,
                                  StepSnapshot after,
                                  String errorMessage) {
            return new StepReport(instructionName, false, before, after, errorMessage);
        }

        public boolean success() {
            return success;
        }

        public StepSnapshot before() {
            return before;
        }

        public StepSnapshot after() {
            return after;
        }

        public String toLogLine() {
            String status = success ? "OK" : "ERR";
            String error = success ? "" : " error=\"" + (errorMessage == null ? "<no-message>" : errorMessage) + "\"";
            return "[m68k-step] "
                + status
                + " op=" + instructionName
                + " pc=" + formatHex(before.programCounter()) + "->" + formatHex(after.programCounter())
                + " sr=" + formatWord(before.statusRegister()) + "(" + formatFlags(before.conditionCodeRegister()) + ")"
                + "->" + formatWord(after.statusRegister()) + "(" + formatFlags(after.conditionCodeRegister()) + ")"
                + " d0=" + formatHex(before.dataRegister(0)) + "->" + formatHex(after.dataRegister(0))
                + " a7=" + formatHex(before.addressRegister(Registers.STACK_POINTER_REGISTER))
                + "->" + formatHex(after.addressRegister(Registers.STACK_POINTER_REGISTER))
                + error;
        }

        private static String formatFlags(int ccr) {
            return "X=" + bit(ccr, 4)
                + ",N=" + bit(ccr, 3)
                + ",Z=" + bit(ccr, 2)
                + ",V=" + bit(ccr, 1)
                + ",C=" + bit(ccr, 0);
        }

        private static int bit(int value, int bitIndex) {
            return (value >>> bitIndex) & 1;
        }

        private static String formatHex(int value) {
            return String.format("0x%08X", value);
        }

        private static String formatWord(int value) {
            return String.format("0x%04X", value & 0xFFFF);
        }
    }

    public static final class StepSnapshot {
        private final int[] dataRegisters;
        private final int[] addressRegisters;
        private final int programCounter;
        private final int statusRegister;

        private StepSnapshot(int[] dataRegisters, int[] addressRegisters, int programCounter, int statusRegister) {
            this.dataRegisters = dataRegisters;
            this.addressRegisters = addressRegisters;
            this.programCounter = programCounter;
            this.statusRegister = statusRegister;
        }

        static StepSnapshot capture(Registers registers, StatusRegister statusRegister) {
            return new StepSnapshot(
                registers.copyDataRegisters(),
                registers.copyAddressRegisters(),
                registers.programCounter(),
                statusRegister.rawValue()
            );
        }

        public int dataRegister(int index) {
            if (index < 0 || index >= Registers.DATA_REGISTER_COUNT) {
                throw new IllegalArgumentException("Invalid data register index D" + index);
            }
            return dataRegisters[index];
        }

        public int addressRegister(int index) {
            if (index < 0 || index >= Registers.ADDRESS_REGISTER_COUNT) {
                throw new IllegalArgumentException("Invalid address register index A" + index);
            }
            return addressRegisters[index];
        }

        public int programCounter() {
            return programCounter;
        }

        public int statusRegister() {
            return statusRegister;
        }

        public int conditionCodeRegister() {
            return statusRegister & 0xFF;
        }
    }
}
