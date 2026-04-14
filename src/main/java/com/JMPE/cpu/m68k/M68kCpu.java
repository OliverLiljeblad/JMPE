package com.JMPE.cpu.m68k;

import com.JMPE.bus.Bus;
import com.JMPE.bus.Rom;
import com.JMPE.cpu.m68k.dispatch.DispatchTable;
import com.JMPE.cpu.m68k.dispatch.Op;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Minimal CPU shell used to wire core components during early emulator milestones.
 */
public final class M68kCpu {
    private static final Decoder DECODER = new Decoder();
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
        this.statusRegister.setSupervisorModeListener(this.registers::setSupervisorStackActive);
        this.registers.setSupervisorStackActive(this.statusRegister.isSupervisorSet());
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

        statusRegister.setRawValue(0);
        statusRegister.setSupervisor(true);
        statusRegister.setTrace(false);
        statusRegister.setInterruptMask(RESET_INTERRUPT_MASK);
        registers.setSupervisorStackPointer(rom.initialSupervisorStackPointer());
        registers.setProgramCounter(rom.initialProgramCounter());
    }

    /**
     * Executes one real CPU step through fetch, decode, dispatch, and execute.
     *
     * <p>
     * This is the first non-placeholder execution path in the CPU core. The
     * method performs the architecturally visible instruction fetch, asks the
     * stateless {@link Decoder} to decode the fetched opword plus any extension
     * words, advances PC to {@link DecodedInstruction#nextPc()} once decode
     * succeeds, and then dispatches to the registered {@link Op} handler for
     * the decoded opcode.
     * </p>
     *
     * <p>
     * PC is advanced before the handler runs. This matches the decoder's
     * contract: by the time execution begins, the instruction stream has
     * already consumed the opword and all extension words. Instructions that
     * change control flow later (for example {@code BRA} or {@code RTS}) can
     * still overwrite PC during execution.
     * </p>
     *
     * @param bus the system bus used for opword fetch and decoder extension-word reads
     * @param dispatchTable the opcode-to-handler registry for execution
     * @param reporter sink for one step log line
     * @return the before/after state report for this step
     * @throws IllegalInstructionException if decode fails for the fetched opword
     */
    public StepReport step(Bus bus,
                           DispatchTable dispatchTable,
                           Consumer<String> reporter) throws IllegalInstructionException {
        Objects.requireNonNull(bus, "bus must not be null");
        Objects.requireNonNull(dispatchTable, "dispatchTable must not be null");
        Objects.requireNonNull(reporter, "reporter must not be null");

        StepSnapshot before = StepSnapshot.capture(registers, statusRegister);
        int opword = bus.readWord(registers.programCounter());
        String instructionName = String.format("0x%04X", opword & 0xFFFF);

        try {
            DecodedInstruction decoded = DECODER.decode(opword, bus, registers.programCounter() + 2);
            instructionName = decoded.opcode().name();

            registers.setProgramCounter(decoded.nextPc());
            Op handler = dispatchTable.lookup(decoded.opcode());
            int cycles = handler.execute(this, bus, decoded);

            StepSnapshot after = StepSnapshot.capture(registers, statusRegister);
            StepReport report = StepReport.success(instructionName, before, after, cycles);
            reporter.accept(report.toLogLine());
            return report;
        } catch (IllegalInstructionException | RuntimeException exception) {
            StepSnapshot after = StepSnapshot.capture(registers, statusRegister);
            StepReport report = StepReport.failure(instructionName, before, after, exception.getMessage());
            reporter.accept(report.toLogLine());
            throw exception;
        }
    }

    public StepReport step(Bus bus, DispatchTable dispatchTable) throws IllegalInstructionException {
        return step(bus, dispatchTable, ignored -> {
        });
    }

    public StepReport stepWithConsoleReport(Bus bus,
                                            DispatchTable dispatchTable) throws IllegalInstructionException {
        return step(bus, dispatchTable, System.out::println);
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
        private final int cycles;

        private StepReport(String instructionName,
                           boolean success,
                           StepSnapshot before,
                           StepSnapshot after,
                           String errorMessage,
                           int cycles) {
            this.instructionName = instructionName;
            this.success = success;
            this.before = before;
            this.after = after;
            this.errorMessage = errorMessage;
            this.cycles = cycles;
        }

        static StepReport success(String instructionName, StepSnapshot before, StepSnapshot after, int cycles) {
            return new StepReport(instructionName, true, before, after, null, cycles);
        }

        static StepReport success(String instructionName, StepSnapshot before, StepSnapshot after) {
            return new StepReport(instructionName, true, before, after, null, 0);
        }

        static StepReport failure(String instructionName,
                                  StepSnapshot before,
                                  StepSnapshot after,
                                  String errorMessage) {
            return new StepReport(instructionName, false, before, after, errorMessage, 0);
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

        public int cycles() {
            return cycles;
        }

        public String toLogLine() {
            String status = success ? "OK" : "ERR";
            String error = success ? "" : " error=\"" + (errorMessage == null ? "<no-message>" : errorMessage) + "\"";
            return "[m68k-step] "
                + status
                + " op=" + instructionName
                + " cycles=" + cycles
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
