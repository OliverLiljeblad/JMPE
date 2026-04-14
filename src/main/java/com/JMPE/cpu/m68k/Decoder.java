package com.JMPE.cpu.m68k;

import com.JMPE.bus.Bus;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;

/**
 * Decodes a single M68000 instruction from its raw bit encoding into a
 * structured {@link DecodedInstruction}.
 *
 * <h2>Responsibility boundary</h2>
 * The Decoder is responsible for exactly one thing: transforming a 16-bit opword
 * (plus any required extension words) into a fully typed description of an
 * instruction. It never reads or writes registers, flags, or data memory. Its
 * only permitted I/O is reading extension words from the Bus — words that are
 * architecturally part of the instruction encoding itself, not data operands.
 *
 * <h2>Why the opword is passed in rather than fetched here</h2>
 * The CPU's fetch/decode boundary is semantically real. On the physical 68000,
 * the first word of every instruction has already been consumed from the bus by
 * the time decode logic runs (the chip has a prefetch queue). Mirroring that
 * boundary in software means:
 * <ul>
 *   <li>Tests can call {@code decode(opword, null, 0)} for any instruction that
 *       has no extension words, with no Bus dependency at all.</li>
 *   <li>The Disassembler can reuse this class without triggering real memory I/O,
 *       because the opword is just an integer it already holds.</li>
 *   <li>{@code M68kCpu.step()} visibly shows the fetch step before calling
 *       decode, making the code self-documenting.</li>
 * </ul>
 *
 * <h2>Why the Bus IS passed for extension words</h2>
 * Extension words (immediates, displacements, full EA extensions) are part of the
 * instruction stream — not data accesses. The 68000 programmer's model defines
 * them as being consumed at decode time, advancing PC as they go. The Decoder
 * must read them to produce a complete description, and it is correct to do so
 * via the Bus. The alternative — pre-fetching all possible extension words in the
 * CPU before decode — is impossible because the number of extension words is not
 * known until the opword is partially decoded.
 *
 * <h2>Statelessness</h2>
 * The Decoder holds no mutable state. Every decode call is a pure function of its
 * arguments. This makes it safe to share across threads, trivial to test in
 * isolation, and free of ordering bugs.
 *
 * <h2>Intended call site in M68kCpu.step()</h2>
 * <pre>
 *   int opword = bus.readWord(registers.pc);
 *   DecodedInstruction decoded = decoder.decode(opword, bus, registers.pc + 2);
 *   registers.pc = decoded.nextPc();   // decoder consumed all extension words
 *   Op handler = dispatchTable.lookup(decoded.opcode());
 *   handler.execute(cpu, decoded);
 * </pre>
 */
public final class Decoder {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Decodes one instruction.
     *
     * @param opword the 16-bit instruction word, already fetched by the CPU.
     *               Passed as an {@code int} because Java has no unsigned short;
     *               only the low 16 bits are significant.
     * @param bus    the system bus, used only to read extension words when the
     *               opword requires them. May be {@code null} for instructions
     *               that are known to have no extension words (useful in tests),
     *               but passing {@code null} for an instruction that does have
     *               extension words will throw a NullPointerException.
     * @param extPc  the bus address of the first extension word — i.e., the
     *               address immediately after the opword. The Decoder reads
     *               extension words sequentially from here, and reports the
     *               final read position back through {@link DecodedInstruction#nextPc()}.
     * @return a fully structured description of the instruction.
     * @throws IllegalInstructionException if the opword does not correspond to
     *         any valid instruction. The caller (M68kCpu) is expected to catch
     *         this and route it through {@link com.JMPE.cpu.m68k.exceptions.ExceptionDispatcher}.
     */
    public DecodedInstruction decode(int opword, Bus bus, int extPc) throws IllegalInstructionException {
        // The top 4 bits (bits 15:12) identify one of 16 instruction families.
        // This is the most reliable and uniform first-level split in the 68k
        // encoding — every instruction belongs to exactly one line.
        //
        // Why a switch rather than a jump table (array of lambdas)?
        // Clarity. The Decoder is already the most complex single class in the
        // cpu package. A switch makes the line boundaries obvious at a glance,
        // and the JIT will optimise it to a table branch anyway.
        int line = (opword >>> 12) & 0xF;
        return switch (line) {
            case 0x0 -> decodeLine0(opword, bus, extPc);
            case 0x1 -> decodeLine1(opword, bus, extPc);
            case 0x2 -> decodeLine2(opword, bus, extPc);
            case 0x3 -> decodeLine3(opword, bus, extPc);
            case 0x4 -> decodeLine4(opword, bus, extPc);
            case 0x5 -> decodeLine5(opword, bus, extPc);
            case 0x6 -> decodeLine6(opword, bus, extPc);
            case 0x7 -> decodeLine7(opword, bus, extPc);
            case 0x8 -> decodeLine8(opword, bus, extPc);
            case 0x9 -> decodeLine9(opword, bus, extPc);
            case 0xA -> decodeLineA(opword, extPc);   // Line-A trap — no extension words
            case 0xB -> decodeLineB(opword, bus, extPc);
            case 0xC -> decodeLineC(opword, bus, extPc);
            case 0xD -> decodeLineD(opword, bus, extPc);
            case 0xE -> decodeLineE(opword, bus, extPc);
            case 0xF -> decodeLineF(opword, extPc);   // Line-F trap — no extension words
            default  -> throw new IllegalInstructionException(opword, extPc - 2);
            // default is unreachable (4-bit value is always 0-15), but the
            // compiler requires it and it documents intent clearly.
        };
    }

    // -------------------------------------------------------------------------
    // Line decoders (first-level dispatch)
    //
    // Each private method handles one of the 16 instruction families. They are
    // the only callers of the bitfield helpers below, keeping the flow
    // top-down and readable.
    //
    // Naming convention: decodeLineX where X is the hex digit of bits 15:12.
    // -------------------------------------------------------------------------

    /**
     * Line 0 (0000): Bit manipulation, MOVEP, and immediate operations.
     *
     * <p>The full decode tree for this line is:
     * <pre>
     *   Bit 8 = 1
     *     Bits 5:3 = 001 (address register)  → MOVEP
     *     Bits 5:3 ≠ 001                     → Dynamic bit op (BTST/BCHG/BCLR/BSET, Dn source)
     *
     *   Bit 8 = 0
     *     Bits 11:9 = 000 → ORI  #data, <ea>  (and ORI to CCR / ORI to SR)
     *     Bits 11:9 = 001 → ANDI #data, <ea>  (and ANDI to CCR / ANDI to SR)
     *     Bits 11:9 = 010 → SUBI #data, <ea>
     *     Bits 11:9 = 011 → ADDI #data, <ea>
     *     Bits 11:9 = 100 → Static bit op (BTST/BCHG/BCLR/BSET, #data source)
     *     Bits 11:9 = 101 → EORI #data, <ea>  (and EORI to CCR / EORI to SR)
     *     Bits 11:9 = 110 → CMPI #data, <ea>
     *     Bits 11:9 = 111 → Illegal
     * </pre>
     *
     * <p>The single most confusing thing about Line 0 is how MOVEP and the
     * dynamic bit operations share the same bit-8=1 space. They are separated
     * by bits 5:3 (the EA mode field in the general case, repurposed as an
     * "address register direct" flag in the MOVEP case). The value 001 in
     * bits 5:3 can never be a valid destination for a bit operation (address
     * registers are not addressable by bit ops on the 68000), so Motorola
     * used it as the MOVEP distinguisher without ambiguity.
     */
    private DecodedInstruction decodeLine0(int op, Bus bus, int extPc) throws IllegalInstructionException {
        int[] cursor = {extPc};

        if ((op & 0x0100) != 0) {
            // ---- Bit 8 = 1: MOVEP or dynamic bit op ----
            //
            // The sole distinguishing criterion is bits 5:3.
            // MOVEP always has bits 5:3 = 001 (address register direct).
            // Dynamic bit ops use any other EA mode (001 is not valid as a
            // bit-operation destination, so there is no ambiguity).
            if (eaMode(op) == 0b001) {
                return decodeMovep(op, bus, cursor);
            } else {
                return decodeDynamicBitOp(op, bus, cursor);
            }
        } else {
            // ---- Bit 8 = 0: immediate group or static bit op ----
            //
            // Bits 11:9 are the operation type selector. The static bit-op
            // group (100) is naturally sandwiched between ADDI (011) and
            // EORI (101) and must be handled as its own branch.
            int optype = (op >>> 9) & 0x7;
            int opwordAddr = extPc - 2;
            return switch (optype) {
                case 0b000 -> decodeImmediateGroup(Opcode.ORI,  op, bus, cursor, true);
                case 0b001 -> decodeImmediateGroup(Opcode.ANDI, op, bus, cursor, true);
                case 0b010 -> decodeImmediateGroup(Opcode.SUBI, op, bus, cursor, false);
                case 0b011 -> decodeImmediateGroup(Opcode.ADDI, op, bus, cursor, false);
                case 0b100 -> decodeStaticBitOp(op, bus, cursor);
                case 0b101 -> decodeImmediateGroup(Opcode.EORI, op, bus, cursor, true);
                case 0b110 -> decodeImmediateGroup(Opcode.CMPI, op, bus, cursor, false);
                default    -> throw new IllegalInstructionException(op, opwordAddr);
                // optype = 111 is illegal on the 68000.
            };
        }
    }

    // =========================================================================
    // Line 0 private helpers
    //
    // Each helper below corresponds to one named leaf in the decode tree above.
    // None of them should be called from outside decodeLine0 — they exist purely
    // to keep the line decoder readable by extracting a single complex case into
    // a well-named method.
    // =========================================================================

    /**
     * Decodes a MOVEP instruction (bit 8 = 1, bits 5:3 = 001).
     *
     * <p>MOVEP transfers a word or long word between a data register and
     * alternating bytes in memory (designed for interfacing 8-bit peripherals
     * on a 16-bit data bus). The address is always {@code (d16, An)}, with the
     * 16-bit signed displacement in the single extension word.
     *
     * <p>Opword layout:
     * <pre>
     *   15:12  = 0000  (line 0)
     *   11:9   = Dn    (data register number)
     *   8      = 1     (identifies MOVEP / dynamic-bit-op group)
     *   7      = direction: 0 = memory→register, 1 = register→memory
     *   6      = size:      0 = word,             1 = long
     *   5:3    = 001   (distinguishes MOVEP from dynamic bit ops)
     *   2:0    = An    (address register number)
     * </pre>
     * Extension words: one signed 16-bit displacement.
     */
    private static DecodedInstruction decodeMovep(int op, Bus bus, int[] cursor) {
        int     dn        = regX(op);                    // bits 11:9
        int     an        = regY(op);                    // bits 2:0
        boolean toMemory  = (op & 0x0080) != 0;         // bit 7: direction
        boolean isLong    = (op & 0x0040) != 0;         // bit 6: size

        Size size = isLong ? Size.LONG : Size.WORD;
        int  d16  = readExtWord(bus, cursor);            // signed displacement

        // Why readExtWord and not readExtWordUnsigned?
        // The displacement is signed — a negative d16 addresses bytes below An.
        // The EA descriptor carries the raw signed value; the executor sign-extends
        // it when computing the physical address (An + d16).

        EffectiveAddress regEa = EffectiveAddress.dataReg(dn);
        EffectiveAddress memEa = EffectiveAddress.addrRegIndDisp(an, d16);

        // Convention: src is always the thing being *read from*, dst the thing
        // being *written to*. MOVEP direction is in bit 7.
        if (toMemory) {
            return new DecodedInstruction(Opcode.MOVEP, size, regEa, memEa, 0, cursor[0]);
        } else {
            return new DecodedInstruction(Opcode.MOVEP, size, memEa, regEa, 0, cursor[0]);
        }
    }

    /**
     * Decodes a dynamic bit operation (bit 8 = 1, bits 5:3 ≠ 001).
     *
     * <p>"Dynamic" means the bit number is held in a data register at
     * execute time (as opposed to the static form where it is an immediate
     * constant in the instruction stream).
     *
     * <p>Opword layout:
     * <pre>
     *   15:12 = 0000   (line 0)
     *   11:9  = Dn     (bit-number register)
     *   8     = 1
     *   7:6   = op:    00=BTST, 01=BCHG, 10=BCLR, 11=BSET
     *   5:3   = destination EA mode  (anything except 001)
     *   2:0   = destination EA register
     * </pre>
     * No extension words for the source (Dn carries the bit number at runtime).
     * The destination EA may consume extension words.
     *
     * <p>Size decision: bit ops on data registers (mode = 000) operate on the
     * full 32-bit long word; bit ops on memory operate on a single byte. Because
     * we know the EA mode at decode time, we set size here rather than deferring
     * to the executor — this keeps the executor free of EA-type inspection.
     */
    private static DecodedInstruction decodeDynamicBitOp(int op, Bus bus, int[] cursor) throws IllegalInstructionException {
        int bitRegNum = regX(op);              // bits 11:9 — the bit-number register
        int bitOp     = (op >>> 6) & 0x3;     // bits 7:6 — which bit operation
        int mode      = eaMode(op);            // bits 5:3
        int reg       = regY(op);              // bits 2:0

        // Size is determined by destination type, not encoded in the opword.
        // For register destinations: LONG (bit number is modulo 32).
        // For memory destinations:  BYTE (bit number is modulo 8).
        Size size = (mode == 0b000) ? Size.LONG : Size.BYTE;

        // The source EA is the data register holding the bit number.
        EffectiveAddress src = EffectiveAddress.dataReg(bitRegNum);

        // The destination EA is a general EA (no size needed by decodeEa itself
        // since immediate — mode=7,reg=4 — is not a valid destination for bit ops).
        // We pass BYTE as a safe sentinel; it would only matter if mode=7,reg=4
        // which the hardware treats as illegal for bit-op destinations.
        EffectiveAddress dst = decodeEa(mode, reg, Size.BYTE, bus, cursor);

        Opcode opcode = decodeBitOpcode(bitOp);
        return new DecodedInstruction(opcode, size, src, dst, 0, cursor[0]);
    }

    /**
     * Decodes a static bit operation (bit 8 = 0, bits 11:9 = 100).
     *
     * <p>"Static" means the bit number is an immediate constant encoded in the
     * instruction stream (the extension word immediately following the opword),
     * as opposed to the dynamic form where it comes from a register.
     *
     * <p>Opword layout:
     * <pre>
     *   15:12 = 0000   (line 0)
     *   11:9  = 100    (static bit-op identifier)
     *   8     = 0
     *   7:6   = op:    00=BTST, 01=BCHG, 10=BCLR, 11=BSET
     *   5:3   = destination EA mode
     *   2:0   = destination EA register
     * </pre>
     * Extension word 1: the bit number (bits 7:0; bits 15:8 must be zero per spec).
     * Extension word 2+: destination EA extension word(s), if required.
     *
     * <p>The bit number is delivered as an {@link EffectiveAddress#immediate(int)}
     * so the executor can treat dynamic and static bit ops identically once the
     * bit number has been resolved to an integer.
     */
    private static DecodedInstruction decodeStaticBitOp(int op, Bus bus, int[] cursor) throws IllegalInstructionException {
        // Read the bit number from the first extension word.
        // Per the M68000 spec, only bits 7:0 are the bit number; bits 15:8
        // must be zero. We mask them without validating — if the ROM is wrong
        // here that is a ROM bug, not a decoder bug.
        int bitNum = readExtWord(bus, cursor) & 0xFF;

        int bitOp  = (op >>> 6) & 0x3;    // bits 7:6 — which bit operation
        int mode   = eaMode(op);           // bits 5:3
        int reg    = regY(op);             // bits 2:0

        // Size determination: same rule as dynamic bit ops.
        Size size = (mode == 0b000) ? Size.LONG : Size.BYTE;

        EffectiveAddress src = EffectiveAddress.immediate(bitNum);
        EffectiveAddress dst = decodeEa(mode, reg, Size.BYTE, bus, cursor);

        Opcode opcode = decodeBitOpcode(bitOp);
        return new DecodedInstruction(opcode, size, src, dst, 0, cursor[0]);
    }

    /**
     * Decodes the immediate instruction group (ORI, ANDI, SUBI, ADDI, EORI, CMPI).
     *
     * <p>All six instructions share the same opword structure:
     * <pre>
     *   15:12 = 0000   (line 0)
     *   11:9  = type   (identifies which instruction)
     *   8     = 0
     *   7:6   = size:  00=BYTE, 01=WORD, 10=LONG
     *   5:3   = destination EA mode
     *   2:0   = destination EA register
     * </pre>
     * Extension words: immediate value first (1 word for BYTE/WORD, 2 for LONG),
     * then destination EA extension word(s) if required.
     *
     * <p><b>CCR and SR destinations (ORI, ANDI, EORI only):</b><br>
     * When bits 5:3 = 111 and bits 2:0 = 100, what would normally be the
     * "immediate" addressing mode in a source position appears in the destination
     * field. This is the encoding the 68000 uses for the CCR/SR destination forms:
     * <ul>
     *   <li>size = BYTE → destination is CCR (condition code register)</li>
     *   <li>size = WORD → destination is SR  (full status register)</li>
     * </ul>
     * This pattern is only valid for ORI, ANDI, EORI. For SUBI, ADDI, CMPI it
     * is illegal. The {@code hasCcrSrVariants} parameter gates this check.
     *
     * <p>Why emit distinct opcodes (ORI_TO_CCR, ORI_TO_SR) rather than letting
     * the executor inspect the destination EA? Two reasons:
     * <ol>
     *   <li>The SR forms are privileged — they trigger a privilege violation
     *       exception in user mode. The executor can check {@code opcode} once
     *       rather than pattern-matching on the destination descriptor.</li>
     *   <li>The Disassembler can print the correct mnemonic ({@code ORI #n,CCR}
     *       vs {@code ORI #n,SR}) without decoding EA records.</li>
     * </ol>
     *
     * @param baseOpcode       the base opcode (ORI, ANDI, etc.)
     * @param hasCcrSrVariants true for ORI/ANDI/EORI; false for SUBI/ADDI/CMPI
     */
    private static DecodedInstruction decodeImmediateGroup(
        Opcode baseOpcode, int op, Bus bus, int[] cursor, boolean hasCcrSrVariants) throws IllegalInstructionException {

        int opwordAddr = cursor[0] - 2;    // save for error reporting before cursor moves

        Size size = decodeSize(sizeBits(op), op, opwordAddr);
        int  imm  = readImmediate(size, bus, cursor);

        int mode = eaMode(op);             // bits 5:3
        int reg  = regY(op);              // bits 2:0

        // ---- CCR / SR destination detection ----
        //
        // Mode = 111 (7), register = 100 (4) is "immediate data" in the *source*
        // EA table. It is never a valid *destination* in the general case.
        // The 68000 reuses this invalid-destination encoding to mean CCR or SR.
        //
        // We check for this before calling decodeEa because decodeEa would
        // mis-read another extension word trying to resolve the "immediate" EA.
        if (mode == 0b111 && reg == 0b100) {
            if (!hasCcrSrVariants) {
                // SUBI / ADDI / CMPI: this pattern is always illegal.
                throw new IllegalInstructionException(op, opwordAddr);
            }
            return switch (size) {
                case BYTE ->
                    // ORI/ANDI/EORI #n, CCR — byte immediate, CCR destination.
                    new DecodedInstruction(
                        toCcrOpcode(baseOpcode), Size.BYTE,
                        EffectiveAddress.immediate(imm),
                        EffectiveAddress.ccr(),
                        0, cursor[0]);
                case WORD ->
                    // ORI/ANDI/EORI #n, SR — word immediate, SR destination.
                    // These are privileged; the runtime executor checks supervisor mode.
                    new DecodedInstruction(
                        toSrOpcode(baseOpcode), Size.WORD,
                        EffectiveAddress.immediate(imm),
                        EffectiveAddress.sr(),
                        0, cursor[0]);
                case LONG ->
                    // LONG with this destination field is always illegal.
                    throw new IllegalInstructionException(op, opwordAddr);
                default ->
                    throw new IllegalInstructionException(op, opwordAddr);
            };
        }

        // ---- General destination EA ----
        EffectiveAddress dst = decodeEa(mode, reg, size, bus, cursor);
        return new DecodedInstruction(
            baseOpcode, size,
            EffectiveAddress.immediate(imm),
            dst, 0, cursor[0]);
    }

    /**
     * Maps a 2-bit bit-operation field (bits 7:6 of a dynamic or static bit-op
     * opword) to the corresponding {@link Opcode}.
     *
     * <pre>
     *   00 → BTST   (bit test; affects Z only, no write)
     *   01 → BCHG   (bit test and change)
     *   10 → BCLR   (bit test and clear)
     *   11 → BSET   (bit test and set)
     * </pre>
     *
     * The {@code default} branch is unreachable for any 2-bit input; it exists
     * only because the Java compiler requires exhaustive switches.
     */
    private static Opcode decodeBitOpcode(int bits76) {
        return switch (bits76 & 0x3) {
            case 0b00 -> Opcode.BTST;
            case 0b01 -> Opcode.BCHG;
            case 0b10 -> Opcode.BCLR;
            case 0b11 -> Opcode.BSET;
            default   -> throw new AssertionError("unreachable: bits76=" + bits76);
        };
    }

    /**
     * Maps a base immediate opcode to its CCR-targeting variant.
     * Only ORI, ANDI, and EORI have CCR variants on the 68000.
     */
    private static Opcode toCcrOpcode(Opcode base) {
        return switch (base) {
            case ORI  -> Opcode.ORI_TO_CCR;
            case ANDI -> Opcode.ANDI_TO_CCR;
            case EORI -> Opcode.EORI_TO_CCR;
            default   -> throw new IllegalArgumentException(
                "No CCR variant for opcode " + base);
        };
    }

    /**
     * Maps a base immediate opcode to its SR-targeting variant.
     * Only ORI, ANDI, and EORI have SR variants on the 68000.
     * These variants are privileged — the executor must check.
     */
    private static Opcode toSrOpcode(Opcode base) {
        return switch (base) {
            case ORI  -> Opcode.ORI_TO_SR;
            case ANDI -> Opcode.ANDI_TO_SR;
            case EORI -> Opcode.EORI_TO_SR;
            default   -> throw new IllegalArgumentException(
                "No SR variant for opcode " + base);
        };
    }

    /**
     * Line 1 (0001): MOVE.B — byte-sized move.
     *
     * <p>Delegates entirely to {@link #decodeMove}. The size is fixed to BYTE
     * by the line number itself — there is nothing else to decide here.
     */
    private DecodedInstruction decodeLine1(int op, Bus bus, int extPc) throws IllegalInstructionException {
        return decodeMove(Size.BYTE, op, bus, new int[]{extPc});
    }

    /**
     * Line 2 (0010): MOVE.L — long-sized move.
     *
     * <p>Note the encoding anomaly: the MOVE size field (bits 13:12) uses
     * the value {@code 10} for LONG, which makes this line 2 not line 3.
     * The size-to-line mapping is: BYTE=01→line1, LONG=10→line2, WORD=11→line3.
     * This non-intuitive ordering is a Motorola design choice; the decoder
     * handles it transparently by treating the line number as the size selector
     * rather than decoding a size field within the opword.
     */
    private DecodedInstruction decodeLine2(int op, Bus bus, int extPc) throws IllegalInstructionException {
        return decodeMove(Size.LONG, op, bus, new int[]{extPc});
    }

    /**
     * Line 3 (0011): MOVE.W — word-sized move. See {@link #decodeLine2} for
     * a note on the non-obvious size encoding.
     */
    private DecodedInstruction decodeLine3(int op, Bus bus, int extPc) throws IllegalInstructionException {
        return decodeMove(Size.WORD, op, bus, new int[]{extPc});
    }

    // =========================================================================
    // Lines 1/2/3 shared helper
    // =========================================================================

    /**
     * Decodes a MOVE or MOVEA instruction for any of the three MOVE lines.
     *
     * <h3>Opword layout</h3>
     * <pre>
     *   15:14  line high bits (00 for all three MOVE lines)
     *   13:12  size:  01=BYTE (line 1), 10=LONG (line 2), 11=WORD (line 3)
     *   11:9   destination register
     *    8:6   destination EA mode
     *    5:3   source EA mode
     *    2:0   source EA register
     * </pre>
     *
     * <h3>The reversed destination field</h3>
     * Every other instruction encodes a source EA as mode={bits 5:3},
     * reg={bits 2:0}. MOVE is the only instruction where the destination EA
     * uses the <em>upper</em> part of the opword, with the fields swapped:
     * mode is in bits 8:6 and register is in bits 11:9. This is unique to
     * MOVE in the entire 68k ISA and exists because MOVE is also the only
     * instruction that encodes two general EAs in a single opword. Motorola
     * needed somewhere to put the second EA and the upper bits were available.
     *
     * <p>The generic {@link #decodeEa} method cannot be used for the destination
     * without explicit field extraction, because it always reads mode from
     * bits 5:3 and reg from bits 2:0. Two dedicated local variables
     * ({@code dstMode}, {@code dstReg}) extract the destination fields before
     * calling {@code decodeEa}.
     *
     * <h3>Extension word order</h3>
     * When both source and destination require extension words (e.g.,
     * {@code MOVE.L (xxx).L, (xxx).L}), the source extension words come
     * <em>first</em> in the instruction stream, followed by the destination
     * extension words. This matches the left-to-right reading order of the
     * assembly syntax. The cursor is advanced through source first, then
     * destination — the order of the two {@code decodeEa} calls is therefore
     * not arbitrary and must not be swapped.
     *
     * <h3>MOVE vs MOVEA</h3>
     * When the destination mode field is {@code 001} (address register direct),
     * the instruction is MOVEA, not MOVE. They differ in two behaviour-critical
     * ways that the executor must handle:
     * <ol>
     *   <li>MOVEA <em>never</em> affects the condition codes. MOVE always does.</li>
     *   <li>MOVEA with WORD size sign-extends the 16-bit source value to 32 bits
     *       before writing the full address register. MOVE.W to a data register
     *       writes only the low 16 bits.</li>
     * </ol>
     * Emitting {@code Opcode.MOVEA} for this case lets the executor implement
     * these differences with a single opcode check rather than inspecting the
     * destination EA type at runtime.
     *
     * <p>MOVEA.B does not exist on the 68000. If destination mode is {@code 001}
     * and the size is BYTE, the opword is illegal and we throw accordingly.
     *
     * <h3>MOVE.B with An as source</h3>
     * For MOVE.B (line 1 only), address register direct ({@code mode=001}) is
     * not a valid <em>source</em> EA. The spec explicitly states: "For byte size
     * operation, address register direct is not allowed." We detect this here
     * and throw {@link IllegalInstructionException}, which the CPU routes to
     * the illegal instruction exception vector — exactly what real hardware does.
     *
     * @param size   the operand size, determined by which line (1/2/3) we are in
     * @param op     the raw 16-bit opword
     * @param bus    the system bus, for reading extension words
     * @param cursor the instruction-stream read cursor; updated in place
     */
    private static DecodedInstruction decodeMove(
        Size size, int op, Bus bus, int[] cursor) throws IllegalInstructionException {

        int opwordAddr = cursor[0] - 2;

        // ---- Extract source EA fields (normal position: bits 5:3, 2:0) ----
        int srcMode = eaMode(op);   // bits 5:3
        int srcReg  = regY(op);     // bits 2:0

        // MOVE.B An,<ea> is explicitly illegal on the 68000.
        // Address registers have no byte-sized representation.
        if (size == Size.BYTE && srcMode == 0b001) {
            throw new IllegalInstructionException(op, opwordAddr);
        }

        // Decode source EA first — its extension words precede the destination's
        // in the instruction stream.
        EffectiveAddress src = decodeEa(srcMode, srcReg, size, bus, cursor);

        // ---- Extract destination EA fields (reversed position: bits 8:6, 11:9) ----
        //
        // This is the only place in the decoder where the destination EA is not
        // in the canonical low-bit position. The two local variables make the
        // inversion explicit and prevent regX/regY from being used incorrectly.
        int dstMode = (op >>> 6) & 0x7;    // bits 8:6 — note: NOT eaMode(op)
        int dstReg  = (op >>> 9) & 0x7;    // bits 11:9 — note: NOT regX(op)

        // ---- MOVEA detection ----
        //
        // Destination mode 001 = address register direct. That is MOVEA.
        if (dstMode == 0b001) {
            // MOVEA.B is not defined on the 68000.
            if (size == Size.BYTE) {
                throw new IllegalInstructionException(op, opwordAddr);
            }
            // dstReg is the address register number. No extension words for
            // a register-direct destination.
            EffectiveAddress dst = EffectiveAddress.addrReg(dstReg);
            return new DecodedInstruction(Opcode.MOVEA, size, src, dst, 0, cursor[0]);
        }

        // ---- General MOVE ----
        //
        // Decode the destination EA. Note that immediate (mode=7,reg=4) and
        // PC-relative modes (mode=7,reg=2/3) are not valid destinations for MOVE —
        // the spec lists only data-alterable modes. We do not validate this here;
        // decodeEa will produce a descriptor for whatever mode is encoded, and
        // the executor or a separate validation pass can enforce the restriction.
        // Refusing to decode would make the decoder more complex without improving
        // correctness in the emulator, since real ROMs will never emit invalid EAs.
        EffectiveAddress dst = decodeEa(dstMode, dstReg, size, bus, cursor);
        return new DecodedInstruction(Opcode.MOVE, size, src, dst, 0, cursor[0]);
    }

    /**
     * Line 4 (0100): Miscellaneous instructions — the most complex line in the ISA.
     *
     * <h3>Top-level decode key: bit 8</h3>
     * <p>Bit 8 = 1 is used exclusively by CHK and LEA, which encode a register
     * number in bits 11:9 and a 3-bit opcode in bits 8:6.  Every other Line 4
     * instruction has bit 8 = 0 and uses bits 11:9 as part of its own fixed
     * opcode.  This single-bit test cleanly separates the two halves.
     *
     * <pre>
     *   bit 8 = 1, bit 6 = 0  →  CHK  Dn, ⟨ea⟩   (bits 8:6 = 110)
     *   bit 8 = 1, bit 6 = 1  →  LEA  ⟨ea⟩, An   (bits 8:6 = 111)
     * </pre>
     *
     * <h3>Bit 8 = 0: dispatch on bits 11:9</h3>
     * <pre>
     *   000  NEGX.B/W/L (sz=00,01,10)  |  MOVE SR,⟨ea⟩          (sz=11)
     *   001  CLR.B/W/L  (sz=00,01,10)  |  MOVE ⟨ea⟩,CCR         (sz=11)
     *   010  NEG.B/W/L  (sz=00,01,10)  |  illegal                (sz=11)
     *   011  NOT.B/W/L  (sz=00,01,10)  |  MOVE ⟨ea⟩,SR          (sz=11, privileged)
     *   100  NBCD (sz=00)  |  SWAP/PEA (sz=01)  |  EXT/MOVEM-r→m (sz=10,11)
     *   101  TST.B/W/L  (sz=00,01,10)  |  TAS/ILLEGAL            (sz=11)
     *   110  MOVEM.W/L mem→reg          (bits 7:6 = 10 or 11)
     *   111  misc: TRAP / LINK / UNLK / MOVE USP / RESET / NOP /
     *              STOP / RTE / RTS / TRAPV / RTR / JSR / JMP
     * </pre>
     *
     * <h3>Relationship between NEGX/CLR/NEG/NOT/TST and their sz=11 cousins</h3>
     * <p>Each of the five unary operation groups re-uses the sz=11 slot (which
     * would be unused since 11 is not a valid size code) for a distinct MOVE
     * variant.  These are detected by checking {@code sizeBits(op) == 0b11}
     * before calling {@link #decodeUnaryEa}, which would otherwise throw on
     * an invalid size.
     *
     * <h3>NBCD/SWAP/PEA/EXT/MOVEM overlap at bits 11:9 = 100</h3>
     * <p>The six instructions in this slot are all distinguished by a two-step
     * test: first {@code sizeBits(op)} (bits 7:6), then {@code eaMode(op) == 0}
     * (data-register direct).  EXT and SWAP target only data registers; MOVEM
     * and PEA target only memory — the hardware and the decoder agree.
     */
    private DecodedInstruction decodeLine4(int op, Bus bus, int extPc) throws IllegalInstructionException {
        int[] cursor = {extPc};
        int   opAddr = extPc - 2;
        int   sz     = sizeBits(op);   // bits 7:6 — used throughout

        // ---- Top split: bit 8 ----
        //
        // CHK and LEA are the only Line 4 instructions that encode a register
        // number in bits 11:9.  All other instructions use those three bits as
        // part of a fixed opcode field and have bit 8 = 0.
        if ((op & 0x0100) != 0) {
            // bit 6 = 0 → CHK (bits 8:6 = 110),  bit 6 = 1 → LEA (bits 8:6 = 111)
            return ((op & 0x0040) != 0)
                ? decodeLea(op, bus, cursor)
                : decodeChk(op, bus, cursor);
        }

        // ---- Bit 8 = 0: dispatch on bits 11:9 ----
        return switch ((op >>> 9) & 0x7) {

            // ------------------------------------------------------------------
            // Group 000: NEGX (sz 00..10)  |  MOVE from SR (sz = 11)
            //
            // MOVE from SR is NOT privileged on the 68000 (it became privileged
            // on the 68010).  We emit MOVE_FROM_SR so the executor can treat the
            // two differently — the Mac Plus runs in supervisor mode almost
            // always, but correctness still requires the distinction.
            // ------------------------------------------------------------------
            case 0b000 -> {
                if (sz == 0b11) {
                    EffectiveAddress dst = decodeEa(eaMode(op), regY(op), Size.WORD, bus, cursor);
                    yield new DecodedInstruction(
                        Opcode.MOVE_FROM_SR, Size.WORD,
                        EffectiveAddress.sr(), dst, 0, cursor[0]);
                }
                yield decodeUnaryEa(Opcode.NEGX, op, bus, cursor, opAddr);
            }

            // ------------------------------------------------------------------
            // Group 001: CLR (sz 00..10)  |  MOVE to CCR (sz = 11)
            //
            // MOVE to CCR is a WORD operation per the spec — the source is read
            // as a word, but only the low byte is loaded into CCR.  The spec note
            // also says: "MOVE to CCR is a word operation; ANDI/ORI/EORI to CCR
            // are byte operations."  We emit Size.WORD accordingly.
            // ------------------------------------------------------------------
            case 0b001 -> {
                if (sz == 0b11) {
                    EffectiveAddress src = decodeEa(eaMode(op), regY(op), Size.WORD, bus, cursor);
                    yield new DecodedInstruction(
                        Opcode.MOVE_TO_CCR, Size.WORD,
                        src, EffectiveAddress.ccr(), 0, cursor[0]);
                }
                yield decodeUnaryEa(Opcode.CLR, op, bus, cursor, opAddr);
            }

            // ------------------------------------------------------------------
            // Group 010: NEG (sz 00..10)  |  illegal (sz = 11)
            // ------------------------------------------------------------------
            case 0b010 -> {
                if (sz == 0b11) throw new IllegalInstructionException(op, opAddr);
                yield decodeUnaryEa(Opcode.NEG, op, bus, cursor, opAddr);
            }

            // ------------------------------------------------------------------
            // Group 011: NOT (sz 00..10)  |  MOVE to SR (sz = 11, privileged)
            // ------------------------------------------------------------------
            case 0b011 -> {
                if (sz == 0b11) {
                    EffectiveAddress src = decodeEa(eaMode(op), regY(op), Size.WORD, bus, cursor);
                    yield new DecodedInstruction(
                        Opcode.MOVE_TO_SR, Size.WORD,
                        src, EffectiveAddress.sr(), 0, cursor[0]);
                }
                yield decodeUnaryEa(Opcode.NOT, op, bus, cursor, opAddr);
            }

            // ------------------------------------------------------------------
            // Group 100: NBCD | SWAP | PEA | EXT.W | EXT.L | MOVEM reg→mem
            //
            // Disambiguation within this group:
            //   sz=00            → NBCD (always memory-like EA, BYTE)
            //   sz=01, mode=000  → SWAP Dn   (data register direct)
            //   sz=01, mode≠000  → PEA <ea>  (control addressing modes)
            //   sz=10, mode=000  → EXT.W Dn  (byte→word sign extension)
            //   sz=10, mode≠000  → MOVEM.W <list>,<ea>  (reg→mem, word)
            //   sz=11, mode=000  → EXT.L Dn  (word→long sign extension)
            //   sz=11, mode≠000  → MOVEM.L <list>,<ea>  (reg→mem, long)
            //
            // EXT vs MOVEM: EXT is restricted to data-register-direct (mode=000).
            // MOVEM reg→mem never uses data-register-direct as its EA (it uses
            // memory modes only).  mode=000 is therefore an unambiguous selector.
            // ------------------------------------------------------------------
            case 0b100 -> {
                int mode = eaMode(op);
                int reg  = regY(op);
                yield switch (sz) {
                    case 0b00 -> {
                        // NBCD — byte operation, no source EA
                        if (mode == 0b001 || (mode == 0b111 && reg >= 0b010)) {
                            throw new IllegalInstructionException(op, opAddr);
                        }
                        EffectiveAddress dst = decodeEa(mode, reg, Size.BYTE, bus, cursor);
                        yield new DecodedInstruction(
                            Opcode.NBCD, Size.BYTE,
                            EffectiveAddress.none(), dst, 0, cursor[0]);
                    }
                    case 0b01 -> {
                        if (mode == 0b000) {
                            // SWAP Dn — operates on the full 32-bit register
                            yield new DecodedInstruction(
                                Opcode.SWAP, Size.LONG,
                                EffectiveAddress.none(),
                                EffectiveAddress.dataReg(reg),
                                0, cursor[0]);
                        }
                        // PEA <ea> — push effective address onto stack
                        EffectiveAddress ea = decodeEa(mode, reg, Size.LONG, bus, cursor);
                        yield new DecodedInstruction(
                            Opcode.PEA, Size.LONG,
                            ea, EffectiveAddress.none(), 0, cursor[0]);
                    }
                    case 0b10 -> {
                        if (mode == 0b000) {
                            // EXT.W — sign-extend low byte of Dn to word
                            yield new DecodedInstruction(
                                Opcode.EXT, Size.WORD,
                                EffectiveAddress.none(),
                                EffectiveAddress.dataReg(reg),
                                0, cursor[0]);
                        }
                        yield decodeMovem(op, Size.WORD, /*memToReg=*/false, bus, cursor);
                    }
                    default -> { // 0b11
                        if (mode == 0b000) {
                            // EXT.L — sign-extend low word of Dn to long
                            yield new DecodedInstruction(
                                Opcode.EXT, Size.LONG,
                                EffectiveAddress.none(),
                                EffectiveAddress.dataReg(reg),
                                0, cursor[0]);
                        }
                        yield decodeMovem(op, Size.LONG, /*memToReg=*/false, bus, cursor);
                    }
                };
            }

            // ------------------------------------------------------------------
            // Group 101: TST (sz 00..10)  |  ILLEGAL (0x4AFC)  |  TAS (sz = 11)
            //
            // 0x4AFC = 0100_1010_1111_1100.  This is TAS with EA = mode=7, reg=4
            // ("immediate"), which is not a valid destination.  Motorola assigned
            // this specific pattern as the architecturally defined ILLEGAL opcode.
            // We detect it by exact value match before reaching the TAS branch.
            //
            // We throw IllegalInstructionException — the CPU catches it and routes
            // to exception vector 4, which is the same destination whether the
            // processor encountered the ILLEGAL opcode deliberately or stumbled on
            // an undefined encoding.  The distinction is a matter of intent, not
            // of hardware behaviour.
            // ------------------------------------------------------------------
            case 0b101 -> {
                if (op == 0x4AFC) {
                    // The deliberately-illegal instruction: always fires vector 4.
                    throw new IllegalInstructionException(op, opAddr);
                }
                if (sz == 0b11) {
                    // TAS — test-and-set; BYTE, atomic RMW on real hardware
                    EffectiveAddress dst = decodeEa(eaMode(op), regY(op), Size.BYTE, bus, cursor);
                    if (!isDataAlterable(dst)) {
                        throw new IllegalInstructionException(op, opAddr);
                    }
                    yield new DecodedInstruction(
                        Opcode.TAS, Size.BYTE,
                        EffectiveAddress.none(), dst, 0, cursor[0]);
                }
                yield decodeUnaryEa(Opcode.TST, op, bus, cursor, opAddr);
            }

            // ------------------------------------------------------------------
            // Group 110: MOVEM mem→reg
            //
            // Only bits 7:6 = 10 (word) and 11 (long) are valid.  Bits 7:6 = 00
            // and 01 are not defined for this group and are treated as illegal.
            // The register-list mask is the first extension word (unsigned 16-bit).
            // ------------------------------------------------------------------
            case 0b110 -> {
                if (sz == 0b10) {
                    yield decodeMovem(op, Size.WORD, /*memToReg=*/true, bus, cursor);
                } else if (sz == 0b11) {
                    yield decodeMovem(op, Size.LONG, /*memToReg=*/true, bus, cursor);
                } else {
                    throw new IllegalInstructionException(op, opAddr);
                }
            }

            // ------------------------------------------------------------------
            // Group 111: misc group (bits 11:9 = 111, i.e. bits 11:8 = 1110)
            // ------------------------------------------------------------------
            default -> decodeLine4MiscGroup(op, bus, cursor, opAddr);
        };
    }

    // =========================================================================
    // Line 4 helpers
    // =========================================================================

    /**
     * Decodes a CHK instruction — {@code CHK <ea>, Dn}.
     *
     * <p>CHK compares a data register against an upper bound supplied by the EA
     * and traps if the register is negative or greater than the bound. On the
     * 68000, CHK is always a word-sized operation; there is no size field in
     * the opword.
     *
     * <p>Opword layout:
     * <pre>
     *   15:12 = 0100   (line 4)
     *   11:9  = Dn     (data register to check)
     *    8:6  = 110    (fixed — CHK opcode identifier)
     *    5:3  = EA mode
     *    2:0  = EA register
     * </pre>
     */
    private static DecodedInstruction decodeChk(int op, Bus bus, int[] cursor) throws IllegalInstructionException {
        int dn = regX(op);   // bits 11:9 — register being checked against bound
        EffectiveAddress bound = decodeEa(eaMode(op), regY(op), Size.WORD, bus, cursor);
        // src = the upper-bound operand; dst = the register being tested
        return new DecodedInstruction(
            Opcode.CHK, Size.WORD,
            bound, EffectiveAddress.dataReg(dn), 0, cursor[0]);
    }

    /**
     * Decodes a LEA instruction — {@code LEA <ea>, An}.
     *
     * <p>LEA computes an effective address and loads it (as a 32-bit value) into
     * the specified address register without performing any memory access. Only
     * control addressing modes are valid for the EA. The size is always LONG
     * (addresses are 32 bits on the 68000).
     *
     * <p>Opword layout:
     * <pre>
     *   15:12 = 0100   (line 4)
     *   11:9  = An     (destination address register)
     *    8:6  = 111    (fixed — LEA opcode identifier)
     *    5:3  = EA mode
     *    2:0  = EA register
     * </pre>
     */
    private static DecodedInstruction decodeLea(int op, Bus bus, int[] cursor) throws IllegalInstructionException {
        int an = regX(op);   // bits 11:9 — destination address register
        EffectiveAddress src = decodeEa(eaMode(op), regY(op), Size.LONG, bus, cursor);
        return new DecodedInstruction(
            Opcode.LEA, Size.LONG,
            src, EffectiveAddress.addrReg(an), 0, cursor[0]);
    }

    /**
     * Shared decoder for the five unary EA instructions: NEGX, CLR, NEG, NOT, TST.
     *
     * <p>All five share the identical opword structure:
     * <pre>
     *   15:12 = 0100   (line 4)
     *   11:9  = fixed group identifier (000..011, 101)
     *    8    = 0
     *    7:6  = size:  00=BYTE, 01=WORD, 10=LONG  (11 handled by caller)
     *    5:3  = EA mode
     *    2:0  = EA register
     * </pre>
     * The caller must check for {@code sizeBits(op) == 0b11} and handle the
     * special instruction at that slot before calling here; {@link #decodeSize}
     * will throw on a raw size value of 11.
     *
     * <p>The EA is placed in {@code dst} for all five instructions.  For TST it
     * is technically read-only, but the executor knows that and ignores the write;
     * placing it in {@code dst} keeps the group structurally uniform.
     *
     * @param opcode the specific opcode (NEGX, CLR, NEG, NOT, or TST)
     */
    private static DecodedInstruction decodeUnaryEa(
        Opcode opcode, int op, Bus bus, int[] cursor, int opAddr) throws IllegalInstructionException {
        Size size = decodeSize(sizeBits(op), op, opAddr);
        EffectiveAddress ea = decodeEa(eaMode(op), regY(op), size, bus, cursor);
        return new DecodedInstruction(
            opcode, size, EffectiveAddress.none(), ea, 0, cursor[0]);
    }

    /**
     * Decodes a MOVEM instruction — register-to-memory or memory-to-register.
     *
     * <p>MOVEM transfers a programmer-specified set of registers to or from
     * consecutive memory locations. The set is encoded as a 16-bit mask in the
     * first extension word, where each bit corresponds to one of the 16 data and
     * address registers.
     *
     * <p>Opword layout:
     * <pre>
     *   15:12 = 0100   (line 4)
     *   11:9  = 100 (reg→mem) or 110 (mem→reg)
     *    8    = 0
     *    7    = 1    (fixed)
     *    6    = size: 0=word, 1=long
     *    5:3  = EA mode
     *    2:0  = EA register
     * </pre>
     * Extension word 1 (always): 16-bit register list mask, stored in
     * {@link DecodedInstruction#extension()}.
     * Extension words 2+: EA extension words, if the EA mode requires them.
     *
     * <p>The register mask is stored in {@code extension} rather than an EA slot
     * because it is a bit-vector of up to 16 register destinations, not an
     * effective address.  The {@code extension} field already exists for
     * instruction-specific payload (A-trap vectors, TRAP vectors) and is the
     * right home here.
     *
     * <p>Register mask bit ordering (M68000 spec §4-128):
     * <ul>
     *   <li>Predecrement mode (reg→mem only): bit 15=D0 ... bit 8=D7,
     *       bit 7=A0 ... bit 0=A7 — reversed vs. all other modes.</li>
     *   <li>All other modes: bit 15=A7 ... bit 8=A0, bit 7=D7 ... bit 0=D0.</li>
     * </ul>
     * The executor is responsible for applying the correct bit ordering when
     * iterating; the decoder passes the raw mask unchanged.
     *
     * @param memToReg {@code true} for memory→register (load),
     *                 {@code false} for register→memory (store)
     */
    private static DecodedInstruction decodeMovem(
        int op, Size size, boolean memToReg, Bus bus, int[] cursor) throws IllegalInstructionException {
        // Register list mask is always the first extension word.
        // We read it unsigned — all 16 bits are significant.
        int mask = readExtWordUnsigned(bus, cursor);

        // The EA is the memory side of the transfer.
        EffectiveAddress memEa = decodeEa(eaMode(op), regY(op), size, bus, cursor);

        Opcode opcode = memToReg ? Opcode.MOVEM_MEM_TO_REG : Opcode.MOVEM_REG_TO_MEM;

        if (memToReg) {
            // src = memory EA, mask goes in extension; dst = none (registers are implicit)
            return new DecodedInstruction(opcode, size, memEa, EffectiveAddress.none(), mask, cursor[0]);
        } else {
            // src = none (registers implicit via mask), dst = memory EA
            return new DecodedInstruction(opcode, size, EffectiveAddress.none(), memEa, mask, cursor[0]);
        }
    }

    /**
     * Decodes the Line 4 miscellaneous group — all instructions with
     * bits 11:9 = 111 (i.e. upper byte of opword = {@code 0100_1110} = 0x4E).
     *
     * <p>Further decoded by bits 7:6, then by remaining low bits:
     * <pre>
     *   bits 7:6 = 00  →  illegal
     *   bits 7:6 = 01  →  0x4E4x: TRAP #v
     *                      0x4E5x: LINK An,#d16 (bit 3=0) / UNLK An (bit 3=1)
     *                      0x4E6x: MOVE An,USP  (bit 3=0) / MOVE USP,An (bit 3=1)
     *                      0x4E70: RESET   0x4E71: NOP    0x4E72: STOP
     *                      0x4E73: RTE     0x4E75: RTS    0x4E76: TRAPV  0x4E77: RTR
     *   bits 7:6 = 10  →  JSR ⟨ea⟩
     *   bits 7:6 = 11  →  JMP ⟨ea⟩
     * </pre>
     *
     * <h3>STOP</h3>
     * <p>STOP reads a 16-bit immediate word (the new SR value) from the instruction
     * stream and stores it in {@link DecodedInstruction#extension()}.  STOP is
     * privileged; the executor must check supervisor mode.
     *
     * <h3>LINK</h3>
     * <p>Reads a signed 16-bit displacement; stored in {@code extension}.
     * The address register number is in bits 2:0 of the opword.
     *
     * <h3>MOVE USP</h3>
     * <p>Both forms are privileged.  The address register is in bits 2:0.
     * Bit 3 = 0 → {@code MOVE An,USP} ({@link Opcode#MOVE_TO_USP}),
     * bit 3 = 1 → {@code MOVE USP,An} ({@link Opcode#MOVE_FROM_USP}).
     */
    private static DecodedInstruction decodeLine4MiscGroup(
        int op, Bus bus, int[] cursor, int opAddr) throws IllegalInstructionException {

        int sz = sizeBits(op);   // bits 7:6 act as the coarse key here

        return switch (sz) {

            case 0b00 ->
                throw new IllegalInstructionException(op, opAddr);

            case 0b01 -> {
                // Sub-decode on bits 7:4 (upper nibble of the low byte, within
                // the constraint that bits 7:6 = 01).
                int bits74 = (op >>> 4) & 0xF;
                yield switch (bits74) {

                    // TRAP #v — bits 7:4 = 0100, vector in bits 3:0.
                    // Adds 32 to the vector number to obtain the exception vector
                    // address.  No extension words.  Vector in extension field.
                    case 0b0100 ->
                        new DecodedInstruction(
                            Opcode.TRAP, Size.UNSIZED,
                            EffectiveAddress.none(), EffectiveAddress.none(),
                            op & 0xF, cursor[0]);

                    // LINK An, #d16  (bit 3 = 0)
                    // UNLK An        (bit 3 = 1)
                    case 0b0101 -> {
                        int an = regY(op);
                        if ((op & 0x8) == 0) {
                            int d16 = readExtWord(bus, cursor);
                            yield new DecodedInstruction(
                                Opcode.LINK, Size.UNSIZED,
                                EffectiveAddress.addrReg(an), EffectiveAddress.none(),
                                d16, cursor[0]);
                        } else {
                            yield new DecodedInstruction(
                                Opcode.UNLK, Size.UNSIZED,
                                EffectiveAddress.addrReg(an), EffectiveAddress.none(),
                                0, cursor[0]);
                        }
                    }

                    // MOVE An, USP  (bit 3 = 0): An → USP
                    // MOVE USP, An  (bit 3 = 1): USP → An
                    // Both privileged.
                    case 0b0110 -> {
                        int an = regY(op);
                        if ((op & 0x8) == 0) {
                            yield new DecodedInstruction(
                                Opcode.MOVE_TO_USP, Size.LONG,
                                EffectiveAddress.addrReg(an), EffectiveAddress.none(),
                                0, cursor[0]);
                        } else {
                            yield new DecodedInstruction(
                                Opcode.MOVE_FROM_USP, Size.LONG,
                                EffectiveAddress.none(), EffectiveAddress.addrReg(an),
                                0, cursor[0]);
                        }
                    }

                    // Single-opword group — decode on the full low byte (0x4E7x).
                    // 0x4E74 = RTD on 68010+ — not defined on 68000.
                    // 0x4E78..0x4E7F are also undefined on the 68000.
                    case 0b0111 -> switch (op & 0xFF) {
                        case 0x70 ->   // RESET — privileged
                            new DecodedInstruction(Opcode.RESET, Size.UNSIZED,
                                EffectiveAddress.none(), EffectiveAddress.none(), 0, cursor[0]);
                        case 0x71 ->   // NOP
                            new DecodedInstruction(Opcode.NOP, Size.UNSIZED,
                                EffectiveAddress.none(), EffectiveAddress.none(), 0, cursor[0]);
                        case 0x72 -> { // STOP #imm — privileged; imm → SR
                            int newSr = readExtWordUnsigned(bus, cursor);
                            yield new DecodedInstruction(Opcode.STOP, Size.UNSIZED,
                                EffectiveAddress.none(), EffectiveAddress.none(), newSr, cursor[0]);
                        }
                        case 0x73 ->   // RTE — privileged, pops SR+PC from supervisor stack
                            new DecodedInstruction(Opcode.RTE, Size.UNSIZED,
                                EffectiveAddress.none(), EffectiveAddress.none(), 0, cursor[0]);
                        case 0x75 ->   // RTS — pops return address from stack
                            new DecodedInstruction(Opcode.RTS, Size.UNSIZED,
                                EffectiveAddress.none(), EffectiveAddress.none(), 0, cursor[0]);
                        case 0x76 ->   // TRAPV — trap if V set
                            new DecodedInstruction(Opcode.TRAPV, Size.UNSIZED,
                                EffectiveAddress.none(), EffectiveAddress.none(), 0, cursor[0]);
                        case 0x77 ->   // RTR — pops CCR+PC from stack
                            new DecodedInstruction(Opcode.RTR, Size.UNSIZED,
                                EffectiveAddress.none(), EffectiveAddress.none(), 0, cursor[0]);
                        default ->
                            throw new IllegalInstructionException(op, opAddr);
                    };

                    default ->
                        // bits 7:4 = 0000..0011, 1000..1111 are unused in the 01 sub-group
                        throw new IllegalInstructionException(op, opAddr);
                };
            }

            // JSR <ea> — bits 7:6 = 10
            case 0b10 -> {
                EffectiveAddress target = decodeEa(eaMode(op), regY(op), Size.UNSIZED, bus, cursor);
                yield new DecodedInstruction(
                    Opcode.JSR, Size.UNSIZED,
                    target, EffectiveAddress.none(), 0, cursor[0]);
            }

            // JMP <ea> — bits 7:6 = 11
            default -> {  // 0b11
                EffectiveAddress target = decodeEa(eaMode(op), regY(op), Size.UNSIZED, bus, cursor);
                yield new DecodedInstruction(
                    Opcode.JMP, Size.UNSIZED,
                    target, EffectiveAddress.none(), 0, cursor[0]);
            }
        };
    }

    /**
     * Line 5 (0101): ADDQ, SUBQ, Scc, and DBcc.
     *
     * <p>Bits 7:6 are the first split in this line:
     * <pre>
     *   bits 7:6 = 00/01/10  →  ADDQ / SUBQ
     *   bits 7:6 = 11        →  Scc / DBcc
     * </pre>
     *
     * <p>For ADDQ/SUBQ, bits 11:9 carry the quick value (000 encodes 8), bit 8
     * distinguishes ADDQ (0) from SUBQ (1), and bits 5:0 encode the destination
     * EA. Address-register direct is legal only for WORD/LONG sizes.
     *
     * <p>For Scc/DBcc, bits 11:8 are the condition code. The DBcc form is the
     * special mode=001 slot and uses the low register field as a data register,
     * not an address register. DBcc always carries a signed 16-bit displacement
     * extension word; Scc instead writes a conditional byte result to an alterable
     * destination EA and carries the raw condition in the decoded extension field.
     */
    private DecodedInstruction decodeLine5(int op, Bus bus, int extPc) throws IllegalInstructionException {
        int sizeBits = sizeBits(op);

        if (sizeBits == 0b11) {
            if (eaMode(op) == 0b001) {
                int[] cursor = {extPc};
                return new DecodedInstruction(
                    Opcode.DBcc,
                    Size.UNSIZED,
                    EffectiveAddress.immediate(readExtWord(bus, cursor)),
                    EffectiveAddress.dataReg(regY(op)),
                    condition(op),
                    cursor[0]
                );
            }
            int opwordAddr = extPc - 2;
            int mode = eaMode(op);
            int reg = regY(op);
            if (mode == 0b111 && reg >= 0b010) {
                throw new IllegalInstructionException(op, opwordAddr);
            }

            int[] cursor = {extPc};
            EffectiveAddress dst = decodeEa(mode, reg, Size.BYTE, bus, cursor);
            return new DecodedInstruction(
                Opcode.Scc,
                Size.BYTE,
                EffectiveAddress.none(),
                dst,
                condition(op),
                cursor[0]
            );
        }

        int opwordAddr = extPc - 2;
        Size size = decodeSize(sizeBits, op, opwordAddr);
        int mode = eaMode(op);
        int reg = regY(op);

        if (mode == 0b001 && size == Size.BYTE) {
            throw new IllegalInstructionException(op, opwordAddr);
        }
        if (mode == 0b111 && reg >= 0b010) {
            throw new IllegalInstructionException(op, opwordAddr);
        }

        int[] cursor = {extPc};
        int quickValue = regX(op);
        if (quickValue == 0) {
            quickValue = 8;
        }

        EffectiveAddress dst = decodeEa(mode, reg, size, bus, cursor);
        Opcode opcode = ((op & 0x0100) == 0) ? Opcode.ADDQ : Opcode.SUBQ;
        return new DecodedInstruction(
            opcode,
            size,
            EffectiveAddress.immediate(quickValue),
            dst,
            0,
            cursor[0]
        );
    }

    /**
     * Line 6 (0110): BRA, BSR, and Bcc (conditional branches).
     *
     * <p>Bits 11:8 encode the condition (0000 = BRA, 0001 = BSR, others = Bcc).
     * The displacement is in the low byte of the opword; if it is $00 the
     * displacement is a full signed 16-bit word in the next extension word.
     * If it is $FF (MC68020+), it is a 32-bit displacement — the 68000 used
     * only in the Mac Plus does not support this, so treat $FF as illegal.
     *
     * <p>This is a good example of why the Decoder must read extension words:
     * the branch target cannot be known without consuming the displacement word
     * when the byte displacement is zero.
     */
    private DecodedInstruction decodeLine6(int op, Bus bus, int extPc) throws IllegalInstructionException {
        int opwordAddr = extPc - 2;
        int[] cursor = {extPc};
        int displacementByte = op & 0xFF;

        Size displacementSize;
        int displacement;
        if (displacementByte == 0x00) {
            displacementSize = Size.WORD;
            displacement = readExtWord(bus, cursor);
        } else {
            if (displacementByte == 0xFF) {
                throw new IllegalInstructionException(op, opwordAddr);
            }
            displacementSize = Size.BYTE;
            displacement = (byte) displacementByte;
        }

        EffectiveAddress src = EffectiveAddress.immediate(displacement);
        int rawCondition = condition(op);
        return switch (rawCondition) {
            case 0x0 -> new DecodedInstruction(
                Opcode.BRA, displacementSize,
                src, EffectiveAddress.none(),
                0, cursor[0]);
            case 0x1 -> new DecodedInstruction(
                Opcode.BSR, displacementSize,
                src, EffectiveAddress.none(),
                0, cursor[0]);
            default -> new DecodedInstruction(
                Opcode.BCC, displacementSize,
                src, EffectiveAddress.none(),
                rawCondition, cursor[0]);
        };
    }

    /**
     * Line 7 (0111): MOVEQ — move quick immediate to data register.
     *
     * <p>This is one of the simplest encodings in the ISA: bits 11:9 = Dn,
     * bit 8 must be 0, and bits 7:0 are the signed 8-bit immediate, sign-extended
     * to 32 bits at execute time. No extension words. No Bus access required.
     * Bit 8 = 1 is illegal.
     */
    private DecodedInstruction decodeLine7(int op, Bus bus, int extPc) throws IllegalInstructionException {
        int opwordAddr = extPc - 2;
        if ((op & 0x0100) != 0) {
            throw new IllegalInstructionException(op, opwordAddr);
        }

        return new DecodedInstruction(
            Opcode.MOVEQ,
            Size.LONG,
            EffectiveAddress.immediate(op & 0xFF),
            EffectiveAddress.dataReg(regX(op)),
            0,
            extPc
        );
    }

    /**
     * Line 8 (1000): OR, DIVU, DIVS, SBCD.
     *
     * <p>Bits 7:6 = 11 with bit 4 = 0 → DIVU; bit 4 = 1 → DIVS.
     * Bits 7:6 = 00 and specific EA encoding → SBCD.
     * Otherwise → OR Dn,<ea> or OR <ea>,Dn (direction from bit 8).
     */
    private DecodedInstruction decodeLine8(int op, Bus bus, int extPc) throws IllegalInstructionException {
        int opwordAddr = extPc - 2;
        int sizeBits = sizeBits(op);
        int mode = eaMode(op);

        if (sizeBits == 0b11) {
            return decodeWordMultiplyOrDivide((op & 0x0100) != 0 ? Opcode.DIVS : Opcode.DIVU, op, bus, extPc);
        }
        if (directionToEa(op) && mode < 0b010) {
            if (sizeBits == 0b00) {
                return decodeBcdRegisterOrPredecrement(Opcode.SBCD, op, opwordAddr, extPc);
            }
            throw new IllegalInstructionException(op, opwordAddr);
        }

        return decodeRegisterEaBinaryOp(Opcode.OR, op, bus, extPc);
    }

    /**
     * Line 9 (1001): SUB, SUBA, SUBX.
     *
     * <p>SUBA is distinguished by bits 7:6 = 11 (size = word or long for ADDA/SUBA
     * follows a different encoding than the normal 2-bit size field).
     * SUBX is distinguished by bit 8 = 1 with bits 5:3 = 000 (register) or 001 (memory).
     */
    private DecodedInstruction decodeLine9(int op, Bus bus, int extPc) throws IllegalInstructionException {
        int sizeBits = sizeBits(op);
        int mode = eaMode(op);

        if (sizeBits == 0b11) {
            return decodeAddressRegisterBinaryOp(Opcode.SUBA, op, bus, extPc);
        }
        if (directionToEa(op) && mode < 0b010) {
            return decodeAddSubX(Opcode.SUBX, op, extPc - 2, extPc);
        }

        return decodeRegisterEaBinaryOp(Opcode.SUB, op, bus, extPc);
    }

    /**
     * Line A (1010): Unimplemented instruction — Line-A emulator trap.
     *
     * <p>On a real 68000, any opword with bits 15:12 = 1010 triggers an
     * unimplemented instruction exception (vector 10). The Mac Plus ROM uses
     * this heavily for its A-trap OS call mechanism. We return a dedicated
     * opcode here rather than throwing, so the execute phase can dispatch it
     * to the trap handler rather than treating it as a hard error.
     *
     * <p>No extension words are consumed — the trap vector is in the opword itself.
     */
    private DecodedInstruction decodeLineA(int op, int extPc) {
        int trapVector = op & 0x0FFF;
        return new DecodedInstruction(
            Opcode.LINE_A_TRAP,
            Size.UNSIZED,
            EffectiveAddress.none(),
            EffectiveAddress.none(),
            trapVector,
            extPc   // no extension words consumed
        );
    }

    /**
     * Line B (1011): CMP, CMPA, CMPM, EOR.
     *
     * <p>Bit 8 = 1 and bits 5:3 = 001 → CMPM (memory-to-memory compare).
     * Bits 7:6 = 11 → CMPA. Bit 8 = 1 (and not CMPM) → EOR. Bit 8 = 0 → CMP.
     */
    private DecodedInstruction decodeLineB(int op, Bus bus, int extPc) throws IllegalInstructionException {
        int opwordAddr = extPc - 2;
        int sizeBits = sizeBits(op);
        int mode = eaMode(op);
        int reg = regY(op);
        int[] cursor = {extPc};

        if (sizeBits == 0b11) {
            return decodeAddressRegisterBinaryOp(Opcode.CMPA, op, bus, extPc);
        }

        Size size = decodeSize(sizeBits, op, opwordAddr);
        if ((op & 0x0100) == 0) {
            if (mode == 0b001) {
                throw new IllegalInstructionException(op, opwordAddr);
            }
            EffectiveAddress src = decodeEa(mode, reg, size, bus, cursor);
            return new DecodedInstruction(
                Opcode.CMP,
                size,
                src,
                EffectiveAddress.dataReg(regX(op)),
                0,
                cursor[0]
            );
        }

        if (mode == 0b001) {
            return decodeCmpm(op, opwordAddr, extPc);
        }
        EffectiveAddress dst = decodeEa(mode, reg, size, bus, cursor);
        return new DecodedInstruction(
            Opcode.EOR,
            size,
            EffectiveAddress.dataReg(regX(op)),
            dst,
            0,
            cursor[0]
        );
    }

    /**
     * Line C (1100): AND, MULU, MULS, ABCD, EXG.
     *
     * <p>Bits 7:6 = 11 → MULU/MULS. Bit 8 = 1 with bits 5:4 = 00 → ABCD.
     * Bit 8 = 1 with specific patterns → EXG. Otherwise → AND.
     */
    private DecodedInstruction decodeLineC(int op, Bus bus, int extPc) throws IllegalInstructionException {
        int opwordAddr = extPc - 2;
        int sizeBits = sizeBits(op);
        int mode = eaMode(op);

        if (sizeBits == 0b11) {
            return decodeWordMultiplyOrDivide((op & 0x0100) != 0 ? Opcode.MULS : Opcode.MULU, op, bus, extPc);
        }
        if (directionToEa(op) && mode < 0b010) {
            if (sizeBits == 0b00) {
                return decodeBcdRegisterOrPredecrement(Opcode.ABCD, op, opwordAddr, extPc);
            }
            return decodeExg(op, opwordAddr, extPc);
        }

        return decodeRegisterEaBinaryOp(Opcode.AND, op, bus, extPc);
    }

    /**
     * Line D (1101): ADD, ADDA, ADDX.
     *
     * <p>Mirror of Line 9 (SUB). Same structural rules apply; see
     * {@link #decodeLine9} comments.
     */
    private DecodedInstruction decodeLineD(int op, Bus bus, int extPc) throws IllegalInstructionException {
        int sizeBits = sizeBits(op);
        int mode = eaMode(op);

        if (sizeBits == 0b11) {
            return decodeAddressRegisterBinaryOp(Opcode.ADDA, op, bus, extPc);
        }
        if (directionToEa(op) && mode < 0b010) {
            return decodeAddSubX(Opcode.ADDX, op, extPc - 2, extPc);
        }

        return decodeRegisterEaBinaryOp(Opcode.ADD, op, bus, extPc);
    }

    /**
     * Line E (1110): Shift and rotate instructions.
     * Includes: ASL, ASR, LSL, LSR, ROL, ROR, ROXL, ROXR.
     *
     * <p>Bits 7:6 distinguish between register shifts (not 11) and memory shifts
     * (11). For register shifts, bit 5 selects immediate count vs. register count,
     * and bits 4:3 encode the operation type (AS/LS/ROX/RO).
     */
    private DecodedInstruction decodeLineE(int op, Bus bus, int extPc) throws IllegalInstructionException {
        if (sizeBits(op) == 0b11) {
            return decodeMemoryShiftOrRotate(op, bus, extPc);
        }
        return decodeRegisterShiftOrRotate(op, extPc - 2, extPc);
    }

    private static DecodedInstruction decodeRegisterEaBinaryOp(
        Opcode opcode, int op, Bus bus, int extPc) throws IllegalInstructionException {

        int opwordAddr = extPc - 2;
        Size size = decodeSize(sizeBits(op), op, opwordAddr);
        int mode = eaMode(op);
        int reg = regY(op);
        int[] cursor = {extPc};

        if (!directionToEa(op)) {
            if (mode == 0b001) {
                throw new IllegalInstructionException(op, opwordAddr);
            }
            EffectiveAddress src = decodeEa(mode, reg, size, bus, cursor);
            return new DecodedInstruction(
                opcode,
                size,
                src,
                EffectiveAddress.dataReg(regX(op)),
                0,
                cursor[0]
            );
        }

        EffectiveAddress dst = decodeEa(mode, reg, size, bus, cursor);
        return new DecodedInstruction(
            opcode,
            size,
            EffectiveAddress.dataReg(regX(op)),
            dst,
            0,
            cursor[0]
        );
    }

    private static DecodedInstruction decodeWordMultiplyOrDivide(
        Opcode opcode, int op, Bus bus, int extPc) throws IllegalInstructionException {

        int opwordAddr = extPc - 2;
        if (eaMode(op) == 0b001) {
            throw new IllegalInstructionException(op, opwordAddr);
        }

        int[] cursor = {extPc};
        EffectiveAddress src = decodeEa(eaMode(op), regY(op), Size.WORD, bus, cursor);
        return new DecodedInstruction(
            opcode,
            Size.WORD,
            src,
            EffectiveAddress.dataReg(regX(op)),
            0,
            cursor[0]
        );
    }

    private static DecodedInstruction decodeAddressRegisterBinaryOp(
        Opcode opcode, int op, Bus bus, int extPc) throws IllegalInstructionException {

        int[] cursor = {extPc};
        Size size = decodeSingleBitSize((op >>> 8) & 1);
        EffectiveAddress src = decodeEa(eaMode(op), regY(op), size, bus, cursor);
        return new DecodedInstruction(
            opcode,
            size,
            src,
            EffectiveAddress.addrReg(regX(op)),
            0,
            cursor[0]
        );
    }

    private static DecodedInstruction decodeBcdRegisterOrPredecrement(
        Opcode opcode, int op, int opwordAddr, int extPc) throws IllegalInstructionException {

        return switch (eaMode(op)) {
            case 0b000 -> new DecodedInstruction(
                opcode,
                Size.BYTE,
                EffectiveAddress.dataReg(regY(op)),
                EffectiveAddress.dataReg(regX(op)),
                0,
                extPc
            );
            case 0b001 -> new DecodedInstruction(
                opcode,
                Size.BYTE,
                EffectiveAddress.addrRegIndPreDec(regY(op)),
                EffectiveAddress.addrRegIndPreDec(regX(op)),
                0,
                extPc
            );
            default -> throw new IllegalInstructionException(op, opwordAddr);
        };
    }

    private static boolean isDataAlterable(EffectiveAddress operand) {
        return operand instanceof EffectiveAddress.DataReg
            || operand instanceof EffectiveAddress.AddrRegInd
            || operand instanceof EffectiveAddress.AddrRegIndPostInc
            || operand instanceof EffectiveAddress.AddrRegIndPreDec
            || operand instanceof EffectiveAddress.AddrRegIndDisp
            || operand instanceof EffectiveAddress.AddrRegIndIndex
            || operand instanceof EffectiveAddress.AbsoluteShort
            || operand instanceof EffectiveAddress.AbsoluteLong;
    }

    private static DecodedInstruction decodeAddSubX(
        Opcode opcode, int op, int opwordAddr, int extPc) throws IllegalInstructionException {

        Size size = decodeSize(sizeBits(op), op, opwordAddr);
        return switch (eaMode(op)) {
            case 0b000 -> new DecodedInstruction(
                opcode,
                size,
                EffectiveAddress.dataReg(regY(op)),
                EffectiveAddress.dataReg(regX(op)),
                0,
                extPc
            );
            case 0b001 -> new DecodedInstruction(
                opcode,
                size,
                EffectiveAddress.addrRegIndPreDec(regY(op)),
                EffectiveAddress.addrRegIndPreDec(regX(op)),
                0,
                extPc
            );
            default -> throw new IllegalInstructionException(op, opwordAddr);
        };
    }

    private static DecodedInstruction decodeCmpm(
        int op, int opwordAddr, int extPc) throws IllegalInstructionException {

        if (eaMode(op) != 0b001) {
            throw new IllegalInstructionException(op, opwordAddr);
        }

        Size size = decodeSize(sizeBits(op), op, opwordAddr);
        return new DecodedInstruction(
            Opcode.CMPM,
            size,
            EffectiveAddress.addrRegIndPostInc(regY(op)),
            EffectiveAddress.addrRegIndPostInc(regX(op)),
            0,
            extPc
        );
    }

    private static DecodedInstruction decodeExg(
        int op, int opwordAddr, int extPc) throws IllegalInstructionException {

        int mode = eaMode(op);
        int sizeBits = sizeBits(op);
        EffectiveAddress src;
        EffectiveAddress dst;

        if (mode == 0b000 && sizeBits == 0b01) {
            src = EffectiveAddress.dataReg(regX(op));
            dst = EffectiveAddress.dataReg(regY(op));
        } else if (mode == 0b001 && sizeBits == 0b01) {
            src = EffectiveAddress.addrReg(regX(op));
            dst = EffectiveAddress.addrReg(regY(op));
        } else if (mode == 0b001 && sizeBits == 0b10) {
            src = EffectiveAddress.dataReg(regX(op));
            dst = EffectiveAddress.addrReg(regY(op));
        } else {
            throw new IllegalInstructionException(op, opwordAddr);
        }

        return new DecodedInstruction(
            Opcode.EXG,
            Size.LONG,
            src,
            dst,
            0,
            extPc
        );
    }

    private static DecodedInstruction decodeRegisterShiftOrRotate(
        int op, int opwordAddr, int extPc) throws IllegalInstructionException {

        int operationBits = (op >>> 3) & 0x3;
        boolean left = (op & 0x0100) != 0;
        Opcode opcode = switch (operationBits) {
            case 0b00 -> left ? Opcode.ASL : Opcode.ASR;
            case 0b01 -> left ? Opcode.LSL : Opcode.LSR;
            case 0b10 -> left ? Opcode.ROXL : Opcode.ROXR;
            case 0b11 -> left ? Opcode.ROL : Opcode.ROR;
            default -> throw new AssertionError("unreachable: operationBits=" + operationBits);
        };

        Size size = decodeSize(sizeBits(op), op, opwordAddr);
        EffectiveAddress src;
        if ((op & 0x0020) != 0) {
            src = EffectiveAddress.dataReg(regX(op));
        } else {
            int count = regX(op);
            src = EffectiveAddress.immediate(count == 0 ? 8 : count);
        }

        return new DecodedInstruction(
            opcode,
            size,
            src,
            EffectiveAddress.dataReg(regY(op)),
            0,
            extPc
        );
    }

    private static DecodedInstruction decodeMemoryShiftOrRotate(
        int op, Bus bus, int extPc) throws IllegalInstructionException {

        int opwordAddr = extPc - 2;
        int mode = eaMode(op);
        int reg = regY(op);
        if (mode < 0b010 || (mode == 0b111 && reg >= 0b010)) {
            throw new IllegalInstructionException(op, opwordAddr);
        }

        Opcode opcode = switch ((op >>> 8) & 0x7) {
            case 0b000 -> Opcode.ASR;
            case 0b001 -> Opcode.ASL;
            case 0b010 -> Opcode.LSR;
            case 0b011 -> Opcode.LSL;
            case 0b100 -> Opcode.ROXR;
            case 0b101 -> Opcode.ROXL;
            case 0b110 -> Opcode.ROR;
            case 0b111 -> Opcode.ROL;
            default -> throw new AssertionError("unreachable");
        };

        int[] cursor = {extPc};
        EffectiveAddress dst = decodeEa(mode, reg, Size.WORD, bus, cursor);
        return new DecodedInstruction(
            opcode,
            Size.WORD,
            EffectiveAddress.none(),
            dst,
            0,
            cursor[0]
        );
    }

    /**
     * Line F (1111): Unimplemented instruction — Line-F coprocessor/trap.
     *
     * <p>Analogous to Line-A. Returns a dedicated opcode so the execute phase
     * can fire the correct exception vector (vector 11) rather than panicking.
     */
    private DecodedInstruction decodeLineF(int op, int extPc) {
        return new DecodedInstruction(
            Opcode.LINE_F_TRAP,
            Size.UNSIZED,
            EffectiveAddress.none(),
            EffectiveAddress.none(),
            0,
            extPc
        );
    }

    // -------------------------------------------------------------------------
    // Bitfield extraction helpers
    //
    // These are the only place in the entire codebase that bit-twiddling of
    // opwords should appear. Every line decoder above calls these helpers;
    // none of them should inline their own bit masks.
    //
    // All methods are private and static because:
    //   - private: nothing outside Decoder should be parsing opwords
    //   - static:  they take no implicit state; making that explicit prevents
    //              accidental reads of instance fields
    //
    // The names match the M68000 programmer's reference manual terminology
    // so they can be cross-referenced directly against the encoding tables.
    // -------------------------------------------------------------------------

    /**
     * Bits 15:12 — the instruction "line", the primary decode key.
     * Result is always in range [0, 15].
     */
    static int line(int op) {
        return (op >>> 12) & 0xF;
    }

    /**
     * Bits 11:9 — the "register" field in most instruction formats.
     * Used as: destination Dn, destination An, shift count register,
     * condition code index, and others depending on line.
     * Result is always in range [0, 7].
     */
    static int regX(int op) {
        return (op >>> 9) & 0x7;
    }

    /**
     * Bits 2:0 — the EA register field. Combined with {@link #eaMode(int)}
     * to fully specify the source (or destination) effective address.
     * Result is always in range [0, 7].
     *
     * <p>For MOVE instructions, the destination EA uses a reversed field
     * layout: dest-reg = bits 11:9, dest-mode = bits 8:6. Those fields
     * must be extracted manually in {@link #decodeLine1} and siblings;
     * {@code regY} must NOT be used for the MOVE destination.
     */
    static int regY(int op) {
        return op & 0x7;
    }

    /**
     * Bits 5:3 — the EA mode field. The value determines how to interpret
     * {@link #regY(int)}:
     * <pre>
     *   000 → Data Register Direct (Dn)        regY = register number
     *   001 → Address Register Direct (An)      regY = register number
     *   010 → Address Register Indirect (An)    regY = register number
     *   011 → (An)+ postincrement               regY = register number
     *   100 → -(An) predecrement                regY = register number
     *   101 → (d16,An) displacement             regY = register number, +1 ext word
     *   110 → (d8,An,Xn) index                  regY = register number, +1 ext word
     *   111 → extended, see regY:
     *          000 → (xxx).W absolute short     +1 ext word
     *          001 → (xxx).L absolute long      +2 ext words
     *          010 → (d16,PC)                   +1 ext word
     *          011 → (d8,PC,Xn)                 +1 ext word
     *          100 → #<data> immediate          +1 or +2 ext words (size-dependent)
     * </pre>
     * Result is always in range [0, 7].
     */
    static int eaMode(int op) {
        return (op >>> 3) & 0x7;
    }

    /**
     * Bits 7:6 — the size field, in the most common encoding used by most
     * instructions. Mapping:
     * <pre>
     *   00 → BYTE
     *   01 → WORD
     *   10 → LONG
     *   11 → (not a size — means something else in the specific instruction)
     * </pre>
     *
     * <p>Note: several instructions use a single bit for size (e.g., MOVEA
     * uses bit 12), and ADDQ/SUBQ use a 2-bit field at bits 7:6 that skips
     * the 11 value. Always verify against the instruction-specific encoding
     * table before using this helper.
     */
    static int sizeBits(int op) {
        return (op >>> 6) & 0x3;
    }

    /**
     * Bits 11:8 — the condition code field, used by Bcc, DBcc, and Scc.
     * <pre>
     *   0000 → T  (true / BRA)
     *   0001 → F  (false / BSR when in branch group)
     *   0010 → HI    0011 → LS
     *   0100 → CC    0101 → CS
     *   0110 → NE    0111 → EQ
     *   1000 → VC    1001 → VS
     *   1010 → PL    1011 → MI
     *   1100 → GE    1101 → LT
     *   1110 → GT    1111 → LE
     * </pre>
     * Result is always in range [0, 15].
     */
    static int condition(int op) {
        return (op >>> 8) & 0xF;
    }

    /**
     * Bit 8 — the direction bit used by several instructions (AND, OR, EOR,
     * ADD, SUB, CMP) to distinguish "Dn op <ea> → Dn" from "Dn op <ea> → <ea>".
     *
     * <p>Returns {@code true} when the result is stored to the EA (memory
     * direction), {@code false} when the result is stored to the data register.
     */
    static boolean directionToEa(int op) {
        return ((op >>> 8) & 1) == 1;
    }

    // -------------------------------------------------------------------------
    // Extension word readers
    //
    // These helpers consume extension words from the Bus and return the value,
    // advancing a mutable cursor. Because Java lacks value-type references,
    // the cursor is passed as an int[] of length 1 — a deliberate choice over
    // returning a two-element array or using a separate mutable object.
    //
    // Why int[1] rather than a proper Cursor class?
    // A Cursor class would be cleaner in isolation but adds a type that serves
    // no other purpose, must be instantiated on every decode call, and puts
    // pressure on the GC. int[1] is ugly but zero-cost on modern JVMs and
    // keeps the allocation entirely on the stack when the JIT escapes-analyses
    // it — which it will for a tight decode loop.
    //
    // All extension word reads must go through these methods (never call
    // bus.readWord directly in a line decoder) so that the cursor is always
    // advanced correctly and the next-PC value is reliable.
    // -------------------------------------------------------------------------

    /**
     * Reads one signed 16-bit word from the instruction stream and advances
     * the cursor by 2.
     *
     * @param bus    the system bus
     * @param cursor a single-element array holding the current read address;
     *               updated in place to point past the word just read
     * @return the word value, sign-extended to 32 bits
     */
    private static int readExtWord(Bus bus, int[] cursor) {
        //TODO: Decide if this should propagate the exception or handle it here
        int value = (short) bus.readWord(cursor[0]);
        cursor[0] += 2;
        return value;
    }

    /**
     * Reads a signed 16-bit word and zero-extends it to produce an unsigned
     * 32-bit value. Used for absolute-short addresses, which are sign-extended
     * by the hardware but treated as addresses in the full 24-bit space.
     */
    private static int readExtWordUnsigned(Bus bus, int[] cursor) {
        return readExtWord(bus, cursor) & 0xFFFF;
    }

    /**
     * Reads two consecutive words forming a 32-bit long value (big-endian,
     * high word first). Advances the cursor by 4.
     */
    private static int readExtLong(Bus bus, int[] cursor) {
        int hi = readExtWord(bus, cursor);
        int lo = readExtWord(bus, cursor);
        return (hi << 16) | (lo & 0xFFFF);
    }

    /**
     * Reads an immediate value of the given size from the instruction stream.
     * <ul>
     *   <li>BYTE: one extension word is consumed; bits 7:0 are the value.</li>
     *   <li>WORD: one extension word; the full 16 bits are the value.</li>
     *   <li>LONG: two extension words; forms a 32-bit value.</li>
     * </ul>
     *
     * <p>For BYTE immediates, the high byte of the extension word must be zero
     * per the 68000 spec. We do not validate this — garbage in the high byte
     * is the ROM's problem, not the decoder's.
     */
    private static int readImmediate(Size size, Bus bus, int[] cursor) {
        return switch (size) {
            case BYTE -> readExtWord(bus, cursor) & 0xFF;
            case WORD -> readExtWord(bus, cursor) & 0xFFFF;
            case LONG -> readExtLong(bus, cursor);
            case UNSIZED -> throw new IllegalArgumentException(
                "Cannot read immediate of unsized size");
        };
    }

    // -------------------------------------------------------------------------
    // EA decoding
    //
    // Produces an EffectiveAddress *descriptor* — a record describing how to
    // locate or compute the operand, not the operand's value. Memory reads
    // happen at execute time, not here.
    //
    // This is the "descriptor vs. value" decision: by deferring memory reads,
    // we ensure that instructions like MOVE (A0)+,(A0)+ can be executed
    // correctly — both EA descriptors are computed with the register value
    // as it was at decode time, and only execution applies the side effects
    // (post-increment) in the correct order.
    //
    // The base PC passed to PC-relative modes is the address of the extension
    // word being evaluated, per the M68000 programmer's reference manual
    // section 2.2 — NOT the address of the opword. This distinction matters
    // and must be preserved exactly.
    // -------------------------------------------------------------------------

    /**
     * Decodes an effective address from the mode and register fields of the
     * opword, reading extension words as required.
     *
     * @param mode   the 3-bit EA mode field (bits 5:3 of most opwords)
     * @param reg    the 3-bit EA register field (bits 2:0)
     * @param size   the operand size, needed to determine how many extension
     *               words an immediate occupies
     * @param bus    the system bus, used only if extension words are needed
     * @param cursor the current instruction-stream read position; advanced
     *               for each extension word consumed
     * @return a descriptor for the effective address
     */
    private static EffectiveAddress decodeEa(
        int mode, int reg, Size size, Bus bus, int[] cursor) throws IllegalInstructionException {

        return switch (mode) {
            case 0b000 -> EffectiveAddress.dataReg(reg);
            case 0b001 -> EffectiveAddress.addrReg(reg);
            case 0b010 -> EffectiveAddress.addrRegInd(reg);
            case 0b011 -> EffectiveAddress.addrRegIndPostInc(reg);
            case 0b100 -> EffectiveAddress.addrRegIndPreDec(reg);
            case 0b101 -> {
                // (d16, An): one signed 16-bit displacement word
                int d16 = readExtWord(bus, cursor);
                yield EffectiveAddress.addrRegIndDisp(reg, d16);
            }
            case 0b110 -> {
                // (d8, An, Xn): one extension word encodes the index register,
                // index size, and signed 8-bit displacement. The format of this
                // extension word is specified in M68000 User's Manual section 2.2.
                int extWord = readExtWord(bus, cursor);
                yield decodeIndexedEa(reg, extWord, false, cursor[0]);
            }
            case 0b111 -> switch (reg) {
                case 0b000 -> {
                    // (xxx).W: absolute short — 16-bit address sign-extended to 32
                    int addr = readExtWord(bus, cursor);
                    yield EffectiveAddress.absoluteShort(addr);
                }
                case 0b001 -> {
                    // (xxx).L: absolute long — full 32-bit address
                    int addr = readExtLong(bus, cursor);
                    yield EffectiveAddress.absoluteLong(addr);
                }
                case 0b010 -> {
                    // (d16, PC): PC-relative with 16-bit displacement
                    // basePC is the address of THIS extension word, per the spec.
                    int basePC = cursor[0];
                    int d16 = readExtWord(bus, cursor);
                    yield EffectiveAddress.pcRelativeDisp(d16, basePC);
                }
                case 0b011 -> {
                    // (d8, PC, Xn): PC-relative with index
                    int basePC = cursor[0];
                    int extWord = readExtWord(bus, cursor);
                    yield decodeIndexedEa(-1 /* PC */, extWord, true, basePC);
                }
                case 0b100 -> {
                    // #<data>: immediate
                    int value = readImmediate(size, bus, cursor);
                    yield EffectiveAddress.immediate(value);
                }
                default -> throw new IllegalInstructionException(
                    -1, cursor[0]);
                // mode=111, reg=101/110/111 are reserved on the 68000.
            };
            default -> throw new IllegalInstructionException(-1, cursor[0]);
            // Unreachable for a 3-bit field, but required by compiler.
        };
    }

    /**
     * Decodes the brief extension word used by indexed addressing modes
     * ({@code (d8,An,Xn)} and {@code (d8,PC,Xn)}).
     *
     * <p>The brief extension word layout (M68000 User's Manual §2.2.6):
     * <pre>
     *   Bit 15:    index register type: 0 = Dn, 1 = An
     *   Bits 14:12 index register number
     *   Bit 11:    index size: 0 = sign-extended word, 1 = long
     *   Bits 10:8  (scale — 68020+ only, must be 000 on 68000)
     *   Bits 7:0   signed 8-bit displacement
     * </pre>
     *
     * @param baseReg   the base address register number (0–7), or -1 if PC
     * @param extWord   the brief extension word
     * @param usePc     true if the base is PC rather than an address register
     * @param basePC    the PC value to use if {@code usePc} is true
     */
    private static EffectiveAddress decodeIndexedEa(
        int baseReg, int extWord, boolean usePc, int basePC) {

        boolean indexIsAddrReg = (extWord & 0x8000) != 0;
        int     indexRegNum    = (extWord >>> 12) & 0x7;
        boolean indexIsLong    = (extWord & 0x0800) != 0;
        int     d8             = (byte) (extWord & 0xFF); // sign-extend 8→32

        if (usePc) {
            return EffectiveAddress.pcRelativeIndex(
                d8, indexIsAddrReg, indexRegNum, indexIsLong, basePC);
        } else {
            return EffectiveAddress.addrRegIndIndex(
                baseReg, d8, indexIsAddrReg, indexRegNum, indexIsLong);
        }
    }

    /**
     * Converts the 2-bit size field (bits 7:6 of most opwords) to a {@link Size}.
     *
     * <p>The mapping {@code 00→BYTE, 01→WORD, 10→LONG} is the standard M68k
     * encoding. The value {@code 11} is not a valid size in most contexts and
     * this helper throws if it is passed — callers that need to handle 11 as a
     * special case must check before calling.
     *
     * @throws IllegalInstructionException if bits are 11
     */
    static Size decodeSize(int sizeBits, int opword, int pc) throws IllegalInstructionException {
        return switch (sizeBits & 0x3) {
            case 0b00 -> Size.BYTE;
            case 0b01 -> Size.WORD;
            case 0b10 -> Size.LONG;
            default   -> throw new IllegalInstructionException(opword, pc);
        };
    }

    /**
     * Converts the 1-bit size field used by MOVEA, ADDA, SUBA, and CMPA.
     * In these instructions, bit 8 (or a similar single bit depending on context)
     * encodes size: 0 = WORD, 1 = LONG.
     */
    static Size decodeSingleBitSize(int bit) {
        return (bit == 0) ? Size.WORD : Size.LONG;
    }
}
