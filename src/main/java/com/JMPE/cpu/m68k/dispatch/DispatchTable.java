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
        register(Opcode.ANDI, new AndiOp());
        register(Opcode.ANDI_TO_CCR, new AndiToCcrOp());
        register(Opcode.ANDI_TO_SR, new AndiToSrOp());
        register(Opcode.CLR, new ClrOp());
        register(Opcode.CMPI, new CmpiOp());
        register(Opcode.EORI, new EoriOp());
        register(Opcode.EORI_TO_CCR, new EoriToCcrOp());
        register(Opcode.EORI_TO_SR, new EoriToSrOp());
        register(Opcode.NOP, new NopOp());
        register(Opcode.NOT, new NotOp());
        register(Opcode.ORI, new OriOp());
        register(Opcode.TST, new TstOp());
    }
}
