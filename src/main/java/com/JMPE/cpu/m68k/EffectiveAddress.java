package com.JMPE.cpu.m68k;

/**
 * A descriptor for a single M68000 effective address, produced by the
 * {@link Decoder} and consumed by instruction executors.
 *
 * <h2>Design: sealed interface + records</h2>
 * Each of the 68000's addressing modes is represented as a separate record
 * implementing this sealed interface. This gives exhaustive, compiler-enforced
 * pattern matching in executors and the disassembler — if a new mode were ever
 * added, every switch would fail to compile until it was handled.
 *
 * <h2>Descriptor, not value</h2>
 * An {@code EffectiveAddress} describes <em>how</em> to locate an operand;
 * it does not hold the operand's value. Memory reads and register lookups
 * happen at execute time, not here. This matters for instructions like
 * {@code MOVE (A0)+,(A0)+} — both descriptors are built before either
 * post-increment side-effect is applied.
 *
 * <h2>Factory methods</h2>
 * Instances are created exclusively through the static factory methods defined
 * on this interface. The record constructors are package-visible but callers
 * should always prefer the factories for readability.
 *
 * <h2>Usage in executors</h2>
 * <pre>
 *   switch (decoded.src()) {
 *       case EffectiveAddress.DataReg(int reg)  -> registers.d[reg];
 *       case EffectiveAddress.Immediate(int v)  -> v;
 *       case EffectiveAddress.AddrRegInd(int r) -> bus.readLong(registers.a[r]);
 *       // ...
 *   }
 * </pre>
 */
public sealed interface EffectiveAddress
    permits EffectiveAddress.DataReg,
    EffectiveAddress.AddrReg,
    EffectiveAddress.AddrRegInd,
    EffectiveAddress.AddrRegIndPostInc,
    EffectiveAddress.AddrRegIndPreDec,
    EffectiveAddress.AddrRegIndDisp,
    EffectiveAddress.AddrRegIndIndex,
    EffectiveAddress.AbsoluteShort,
    EffectiveAddress.AbsoluteLong,
    EffectiveAddress.PcRelativeDisp,
    EffectiveAddress.PcRelativeIndex,
    EffectiveAddress.Immediate,
    EffectiveAddress.Ccr,
    EffectiveAddress.Sr,
    EffectiveAddress.None {

    // =========================================================================
    // Record variants — one per 68000 addressing mode
    // =========================================================================

    /**
     * Data register direct: {@code Dn}.
     * The operand is the value of data register {@code reg} (0–7).
     */
    record DataReg(int reg) implements EffectiveAddress {}

    /**
     * Address register direct: {@code An}.
     * The operand is the value of address register {@code reg} (0–7).
     * <p>Note: byte-size operations on An are illegal on the 68000. The decoder
     * checks this for MOVE; individual executors must enforce it elsewhere.
     */
    record AddrReg(int reg) implements EffectiveAddress {}

    /**
     * Address register indirect: {@code (An)}.
     * The operand is at the memory address held in {@code An}.
     */
    record AddrRegInd(int reg) implements EffectiveAddress {}

    /**
     * Address register indirect with postincrement: {@code (An)+}.
     * The operand is at the address in {@code An}; after the access the
     * register is incremented by the operand size (1, 2, or 4 bytes).
     * <p>Exception: for A7 (SP) with BYTE size, the increment is 2 to keep
     * the stack word-aligned. The executor is responsible for this.
     */
    record AddrRegIndPostInc(int reg) implements EffectiveAddress {}

    /**
     * Address register indirect with predecrement: {@code -(An)}.
     * Before the access, {@code An} is decremented by the operand size;
     * the operand is then at that new address. Same A7/byte alignment
     * caveat as postincrement applies.
     */
    record AddrRegIndPreDec(int reg) implements EffectiveAddress {}

    /**
     * Address register indirect with 16-bit displacement: {@code (d16, An)}.
     * Effective address = {@code An + d16}. The displacement is signed and
     * already sign-extended to 32 bits by the decoder.
     */
    record AddrRegIndDisp(int reg, int d16) implements EffectiveAddress {}

    /**
     * Address register indirect with index: {@code (d8, An, Xn)}.
     * Effective address = {@code An + Xn + d8}.
     *
     * @param baseReg       the base address register number (0–7)
     * @param d8            signed 8-bit displacement, sign-extended to 32 bits
     * @param indexIsAddrReg {@code true} if the index register is An, {@code false} for Dn
     * @param indexRegNum   the index register number (0–7)
     * @param indexIsLong   {@code true} if the index is used as a 32-bit long;
     *                      {@code false} if it should be sign-extended from its low 16 bits
     */
    record AddrRegIndIndex(
        int     baseReg,
        int     d8,
        boolean indexIsAddrReg,
        int     indexRegNum,
        boolean indexIsLong
    ) implements EffectiveAddress {}

    /**
     * Absolute short: {@code (xxx).W}.
     * The address is a 16-bit value that the 68000 sign-extends to 24 bits.
     * Stored here as the raw signed 16-bit value; the executor sign-extends
     * it when computing the bus address.
     */
    record AbsoluteShort(int address) implements EffectiveAddress {}

    /**
     * Absolute long: {@code (xxx).L}.
     * The full 32-bit address is encoded in two consecutive extension words.
     * Stored here as a plain {@code int} (Java's 32-bit signed type covers
     * the 68000's 24-bit address space without loss).
     */
    record AbsoluteLong(int address) implements EffectiveAddress {}

    /**
     * PC-relative with 16-bit displacement: {@code (d16, PC)}.
     * Effective address = {@code basePC + d16}.
     *
     * <p>{@code basePC} is the address of the extension word containing the
     * displacement — NOT the address of the opword. This matches the
     * M68000 programmer's reference manual §2.2 exactly and must be preserved.
     */
    record PcRelativeDisp(int d16, int basePC) implements EffectiveAddress {}

    /**
     * PC-relative with index: {@code (d8, PC, Xn)}.
     * Effective address = {@code basePC + Xn + d8}.
     *
     * @param d8            signed 8-bit displacement, sign-extended to 32 bits
     * @param indexIsAddrReg {@code true} if index register is An, {@code false} for Dn
     * @param indexRegNum   the index register number (0–7)
     * @param indexIsLong   {@code true} for 32-bit index; {@code false} for sign-extended 16-bit
     * @param basePC        the address of the extension word, per the spec
     */
    record PcRelativeIndex(
        int     d8,
        boolean indexIsAddrReg,
        int     indexRegNum,
        boolean indexIsLong,
        int     basePC
    ) implements EffectiveAddress {}

    /**
     * Immediate data: {@code #<data>}.
     * The value is fully resolved at decode time and stored directly here.
     * <ul>
     *   <li>BYTE: value is in range [0, 255]</li>
     *   <li>WORD: value is in range [0, 65535]</li>
     *   <li>LONG: full 32-bit signed value</li>
     * </ul>
     * Also used as the source for static bit operations (BTST/BCHG/BCLR/BSET
     * with immediate bit number), where the value is the bit position.
     */
    record Immediate(int value) implements EffectiveAddress {}

    /**
     * Condition code register: {@code CCR}.
     * Used as the destination for {@code ORI/ANDI/EORI #n, CCR} and
     * {@code MOVE <ea>, CCR}. These are byte-wide operations affecting only
     * the low byte of the status register.
     * <p>Singleton — use {@link #ccr()} rather than constructing directly.
     */
    record Ccr() implements EffectiveAddress {
        private static final Ccr INSTANCE = new Ccr();
    }

    /**
     * Status register: {@code SR}.
     * Used as the source for {@code MOVE SR, <ea>} and as the destination for
     * {@code ORI/ANDI/EORI #n, SR} and {@code MOVE <ea>, SR}. All SR-write
     * forms are privileged; the executor must check supervisor mode.
     * <p>Singleton — use {@link #sr()} rather than constructing directly.
     */
    record Sr() implements EffectiveAddress {
        private static final Sr INSTANCE = new Sr();
    }

    /**
     * Sentinel for "no effective address".
     * Used for instructions that have no source or no destination operand —
     * for example: {@code NOP}, {@code RTS}, {@code RESET}, unary operations
     * where only one EA slot is populated, and so on.
     * <p>Singleton — use {@link #none()} rather than constructing directly.
     */
    record None() implements EffectiveAddress {
        private static final None INSTANCE = new None();
    }

    // =========================================================================
    // Factory methods
    //
    // These are the canonical way to create EA descriptors. They are what the
    // Decoder calls; executors and tests should use them too where convenient.
    // =========================================================================

    /** Dn direct. */
    static EffectiveAddress dataReg(int reg) {
        return new DataReg(reg);
    }

    /** An direct. */
    static EffectiveAddress addrReg(int reg) {
        return new AddrReg(reg);
    }

    /** (An) — indirect. */
    static EffectiveAddress addrRegInd(int reg) {
        return new AddrRegInd(reg);
    }

    /** (An)+ — indirect with postincrement. */
    static EffectiveAddress addrRegIndPostInc(int reg) {
        return new AddrRegIndPostInc(reg);
    }

    /** -(An) — indirect with predecrement. */
    static EffectiveAddress addrRegIndPreDec(int reg) {
        return new AddrRegIndPreDec(reg);
    }

    /** (d16, An) — indirect with 16-bit signed displacement. */
    static EffectiveAddress addrRegIndDisp(int reg, int d16) {
        return new AddrRegIndDisp(reg, d16);
    }

    /** (d8, An, Xn) — indirect with index and 8-bit signed displacement. */
    static EffectiveAddress addrRegIndIndex(
        int baseReg, int d8,
        boolean indexIsAddrReg, int indexRegNum, boolean indexIsLong) {
        return new AddrRegIndIndex(baseReg, d8, indexIsAddrReg, indexRegNum, indexIsLong);
    }

    /** (xxx).W — absolute short (16-bit address, sign-extended). */
    static EffectiveAddress absoluteShort(int address) {
        return new AbsoluteShort(address);
    }

    /** (xxx).L — absolute long (full 32-bit address). */
    static EffectiveAddress absoluteLong(int address) {
        return new AbsoluteLong(address);
    }

    /** (d16, PC) — PC-relative with 16-bit displacement. */
    static EffectiveAddress pcRelativeDisp(int d16, int basePC) {
        return new PcRelativeDisp(d16, basePC);
    }

    /** (d8, PC, Xn) — PC-relative with index. */
    static EffectiveAddress pcRelativeIndex(
        int d8, boolean indexIsAddrReg,
        int indexRegNum, boolean indexIsLong, int basePC) {
        return new PcRelativeIndex(d8, indexIsAddrReg, indexRegNum, indexIsLong, basePC);
    }

    /** #&lt;data&gt; — immediate value, resolved at decode time. */
    static EffectiveAddress immediate(int value) {
        return new Immediate(value);
    }

    /** CCR as destination. Singleton. */
    static EffectiveAddress ccr() {
        return Ccr.INSTANCE;
    }

    /** SR as source or destination. Singleton. */
    static EffectiveAddress sr() {
        return Sr.INSTANCE;
    }

    /**
     * No effective address — sentinel for unused src/dst slots.
     * Singleton.
     */
    static EffectiveAddress none() {
        return None.INSTANCE;
    }
}
