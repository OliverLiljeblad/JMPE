package com.JMPE.cpu.m68k.dispatch;

import com.JMPE.cpu.m68k.instructions.Opcode;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registry mapping decoded {@link Opcode opcodes} to executable {@link Op}
 * handlers.
 *
 * <p>
 * In this project structure, {@code Decoder} answers "what instruction is
 * this?", while {@code DispatchTable} answers "which executor handles it?".
 * Keeping that lookup centralized avoids pushing a growing opcode switch into
 * {@code M68kCpu} and gives each instruction or instruction family a clear
 * place to plug into the runtime pipeline.
 * </p>
 */
public final class DispatchTable {
    private final Map<Opcode, Op> handlers = new EnumMap<>(Opcode.class);

    /**
     * Creates a dispatch table with the built-in handlers that are currently
     * available in the runtime.
     */
    public DispatchTable() {
        this(true);
    }

    private DispatchTable(boolean registerBuiltIns) {
        if (registerBuiltIns) {
            registerBuiltIns();
        }
    }

    /**
     * Creates an empty dispatch table.
     *
     * <p>
     * This is mainly useful for focused unit tests where the registration
     * behavior itself is under test.
     * </p>
     */
    public static DispatchTable empty() {
        return new DispatchTable(false);
    }

    /**
     * Registers the handler for one opcode.
     *
     * @throws NullPointerException if {@code opcode} or {@code handler} is null
     * @throws IllegalStateException if a handler is already registered
     */
    public void register(Opcode opcode, Op handler) {
        Objects.requireNonNull(opcode, "opcode must not be null");
        Objects.requireNonNull(handler, "handler must not be null");

        Op previous = handlers.putIfAbsent(opcode, handler);
        if (previous != null) {
            throw new IllegalStateException("Handler already registered for opcode " + opcode);
        }
    }

    /**
     * Returns the registered handler for the opcode.
     *
     * @throws NullPointerException if {@code opcode} is null
     * @throws IllegalStateException if no handler has been registered
     */
    public Op lookup(Opcode opcode) {
        Objects.requireNonNull(opcode, "opcode must not be null");

        Op handler = handlers.get(opcode);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for opcode " + opcode);
        }
        return handler;
    }

    /**
     * Returns whether the opcode currently has a registered handler.
     *
     * @throws NullPointerException if {@code opcode} is null
     */
    public boolean hasHandler(Opcode opcode) {
        Objects.requireNonNull(opcode, "opcode must not be null");
        return handlers.containsKey(opcode);
    }

    private void registerBuiltIns() {
        register(Opcode.ADD, new AddOp());
        register(Opcode.ADDI, new AddiOp());
        register(Opcode.ADDA, new AddaOp());
        register(Opcode.ADDX, new AddxOp());
        register(Opcode.ADDQ, new AddqOp());
        register(Opcode.AND, new AndOp());
        register(Opcode.ANDI, new AndiOp());
        register(Opcode.ANDI_TO_CCR, new AndiToCcrOp());
        register(Opcode.ANDI_TO_SR, new AndiToSrOp());
        register(Opcode.ASL, new AslOp());
        register(Opcode.ASR, new AsrOp());
        register(Opcode.BCC, new BccOp());
        register(Opcode.BCHG, new BchgOp());
        register(Opcode.BCLR, new BclrOp());
        register(Opcode.BSET, new BsetOp());
        register(Opcode.BRA, new BraOp());
        register(Opcode.BSR, new BsrOp());
        register(Opcode.BTST, new BtstOp());
        register(Opcode.CHK, new ChkOp());
        register(Opcode.CLR, new ClrOp());
        register(Opcode.CMP, new CmpOp());
        register(Opcode.CMPA, new CmpaOp());
        register(Opcode.CMPI, new CmpiOp());
        register(Opcode.CMPM, new CmpmOp());
        register(Opcode.DBcc, new DbccOp());
        register(Opcode.DIVS, new DivsOp());
        register(Opcode.DIVU, new DivuOp());
        register(Opcode.EOR, new EorOp());
        register(Opcode.EORI, new EoriOp());
        register(Opcode.EORI_TO_CCR, new EoriToCcrOp());
        register(Opcode.EORI_TO_SR, new EoriToSrOp());
        register(Opcode.EXT, new ExtOp());
        register(Opcode.EXG, new ExgOp());
        register(Opcode.JMP, new JmpOp());
        register(Opcode.JSR, new JsrOp());
        register(Opcode.LEA, new LeaOp());
        register(Opcode.LSL, new LslOp());
        register(Opcode.LSR, new LsrOp());
        register(Opcode.MOVE, new MoveOp());
        register(Opcode.MOVE_FROM_SR, new MoveFromSrOp());
        register(Opcode.MOVE_TO_CCR, new MoveToCcrOp());
        register(Opcode.MOVE_TO_SR, new MoveToSrOp());
        register(Opcode.MOVEA, new MoveaOp());
        register(Opcode.MOVEM_MEM_TO_REG, new MovemMemToRegOp());
        register(Opcode.MOVEM_REG_TO_MEM, new MovemRegToMemOp());
        register(Opcode.MOVEQ, new MoveQOp());
        register(Opcode.MULS, new MulsOp());
        register(Opcode.MULU, new MuluOp());
        register(Opcode.NEG, new NegOp());
        register(Opcode.NEGX, new NegxOp());
        register(Opcode.NOP, new NopOp());
        register(Opcode.NOT, new NotOp());
        register(Opcode.OR, new OrOp());
        register(Opcode.ORI, new OriOp());
        register(Opcode.ORI_TO_CCR, new OriToCcrOp());
        register(Opcode.ORI_TO_SR, new OriToSrOp());
        register(Opcode.PEA, new PeaOp());
        register(Opcode.ROL, new RolOp());
        register(Opcode.ROR, new RorOp());
        register(Opcode.ROXL, new RoxlOp());
        register(Opcode.ROXR, new RoxrOp());
        register(Opcode.RTS, new RtsOp());
        register(Opcode.Scc, new SccOp());
        register(Opcode.SUB, new SubOp());
        register(Opcode.SUBA, new SubaOp());
        register(Opcode.SUBI, new SubiOp());
        register(Opcode.SUBQ, new SubqOp());
        register(Opcode.SUBX, new SubxOp());
        register(Opcode.SWAP, new SwapOp());
        register(Opcode.TST, new TstOp());
    }
}
