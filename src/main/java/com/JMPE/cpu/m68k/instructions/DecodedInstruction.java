package com.JMPE.cpu.m68k.instructions;

import com.JMPE.cpu.m68k.Decoder;
import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.Size;

/**
 * The fully decoded form of one M68000 instruction, produced by {@link Decoder}
 * and consumed by {@link com.JMPE.cpu.m68k.dispatch.DispatchTable} and its
 * {@link com.JMPE.cpu.m68k.dispatch.Op} handlers.
 *
 * <h2>What this holds</h2>
 * <ul>
 *   <li>{@link #opcode}    — which operation to perform</li>
 *   <li>{@link #size}      — operand width (BYTE / WORD / LONG / UNSIZED)</li>
 *   <li>{@link #src}       — source effective address descriptor</li>
 *   <li>{@link #dst}       — destination effective address descriptor</li>
 *   <li>{@link #extension} — instruction-specific integer payload (see below)</li>
 *   <li>{@link #nextPc}    — address of the first byte after this instruction</li>
 * </ul>
 *
 * <h2>The {@code extension} field</h2>
 * Several instructions need a small integer that does not fit naturally into
 * either EA slot.  Rather than adding specialised fields for each, a single
 * {@code extension} int carries the payload:
 * <ul>
 *   <li><b>MOVEM</b> — 16-bit register-list mask (raw, unmodified)</li>
 *   <li><b>BCC</b>   — raw condition nibble (2–15; 0/1 are BRA/BSR instead)</li>
 *   <li><b>TRAP</b>  — trap vector number (0–15)</li>
 *   <li><b>STOP</b>  — the immediate value loaded into SR</li>
 *   <li><b>LINK</b>  — signed 16-bit stack displacement</li>
 *   <li><b>LINE_A_TRAP / LINE_F_TRAP</b> — the raw opword (full 16 bits)</li>
 *   <li>All other instructions — {@code 0} (unused)</li>
 * </ul>
 *
 * <h2>Why a record</h2>
 * {@code DecodedInstruction} is pure data: immutable, value-transparent, and
 * short-lived (one per CPU step). A record provides canonical accessors,
 * {@code equals}/{@code hashCode}/{@code toString} for free, and makes it
 * impossible to mutate a decoded instruction after the fact — which would be
 * a decoder bug.
 *
 * <h2>Intended call site in {@code M68kCpu.step()}</h2>
 * <pre>
 *   int opword = bus.readWord(registers.pc);
 *   DecodedInstruction decoded = decoder.decode(opword, bus, registers.pc + 2);
 *   registers.pc = decoded.nextPc();          // advance past all extension words
 *   Op handler = dispatchTable.lookup(decoded.opcode());
 *   handler.execute(cpu, decoded);
 * </pre>
 *
 * <h2>Single-EA instructions</h2>
 * Instructions with only one operand (e.g. {@code CLR}, {@code NOT}, {@code JMP})
 * place that operand in {@code dst} and set {@code src} to
 * {@link EffectiveAddress#none()}. Instructions with no operands at all (e.g.
 * {@code NOP}, {@code RTS}) set both EA slots to {@link EffectiveAddress#none()}.
 */
public record DecodedInstruction(
    Opcode          opcode,
    Size            size,
    EffectiveAddress src,
    EffectiveAddress dst,
    int             extension,
    int             nextPc
) {
    /**
     * Compact constructor — validates invariants that must hold for any
     * correctly decoded instruction.
     *
     * <p>These checks catch decoder bugs in development and test runs.
     * They are intentionally lightweight (no bus access, no allocation)
     * so they impose negligible overhead even with assertions enabled.
     */
    public DecodedInstruction {
        if (opcode == null)  throw new NullPointerException("opcode must not be null");
        if (size   == null)  throw new NullPointerException("size must not be null");
        if (src    == null)  throw new NullPointerException("src must not be null — use EffectiveAddress.none()");
        if (dst    == null)  throw new NullPointerException("dst must not be null — use EffectiveAddress.none()");
    }

    // -------------------------------------------------------------------------
    // Convenience query methods
    //
    // These sit here rather than in the executor classes so that any code with
    // a DecodedInstruction can ask simple questions without importing Op or
    // DispatchTable.
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this instruction has no meaningful source
     * operand — i.e., {@code src} is {@link EffectiveAddress.None}.
     */
    public boolean hasNoSource() {
        return src instanceof EffectiveAddress.None;
    }

    /**
     * Returns {@code true} if this instruction has no meaningful destination
     * operand — i.e., {@code dst} is {@link EffectiveAddress.None}.
     */
    public boolean hasNoDestination() {
        return dst instanceof EffectiveAddress.None;
    }

    /**
     * Returns {@code true} if this is an UNSIZED instruction — one that
     * does not operate on byte/word/long data (e.g. {@code NOP}, {@code JMP},
     * {@code RTS}, {@code TRAP}).
     */
    public boolean isUnsized() {
        return size == Size.UNSIZED;
    }

    /**
     * Returns a compact, human-readable summary suitable for logging and the
     * debug overlay.  Full disassembly is handled by
     * {@link com.JMPE.ui.debugger.Disassembler}, which has access to symbol
     * tables and memory; this is just for quick diagnostics.
     *
     * <p>Example output: {@code MOVE.W (A0)+,D3}  →  not implemented here,
     * but toString gives at least: {@code DecodedInstruction[MOVE.W src=(A0)+, dst=D3, nextPc=0x1006]}
     */
    @Override
    public String toString() {
        String sizeTag = isUnsized() ? "" : "." + size.name().charAt(0);
        return "DecodedInstruction["
            + opcode.name() + sizeTag
            + " src=" + src
            + ", dst=" + dst
            + (extension != 0 ? ", ext=0x" + Integer.toHexString(extension) : "")
            + ", nextPc=0x" + Integer.toHexString(nextPc)
            + "]";
    }
}
