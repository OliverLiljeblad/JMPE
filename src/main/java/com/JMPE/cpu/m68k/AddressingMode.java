package com.JMPE.cpu.m68k;

public enum AddressingMode {
    // Register Direct
    DATA_REGISTER_DIRECT,
    ADDRESS_REGISTER_DIRECT,

    // Register Indirect
    REGISTER_INDIRECT,
    POST_INCREMENT,
    PRE_DECREMENT,
    INDIRECT_DISPLACEMENT,

    // Index
    INDEX_8BIT,
    INDEX_BASE_DISPLACEMENT,

    // Memory Indirect
    MEMORY_INDIRECT_POSTINDEXED,
    MEMORY_INDIRECT_PREINDEXED,

    // PC Relative
    PC_RELATIVE_DISPLACEMENT,
    PC_RELATIVE_INDEXED,

    // PC Memory Indirect
    PC_MEMORY_INDIRECT_POSTINDEXED,
    PC_MEMORY_INDIRECT_PREINDEXED,

    // Absolute
    ABSOLUTE_SHORT,
    ABSOLUTE_LONG,

    // Immediate
    IMMEDIATE;

    // Add opcode-mode mapping for the 8 basic modes (0-7)
    // Note: ABSOLUTE_SHORT/LONG, IMMEDIATE, etc. are decoded separately
    private static final AddressingMode[] BY_BASIC_MODE_BITS = new AddressingMode[8];

    static {
        BY_BASIC_MODE_BITS[0] = DATA_REGISTER_DIRECT;      // 000
        BY_BASIC_MODE_BITS[1] = ADDRESS_REGISTER_DIRECT;   // 001
        BY_BASIC_MODE_BITS[2] = REGISTER_INDIRECT;         // 010
        BY_BASIC_MODE_BITS[3] = POST_INCREMENT;            // 011
        BY_BASIC_MODE_BITS[4] = PRE_DECREMENT;             // 100
        BY_BASIC_MODE_BITS[5] = INDIRECT_DISPLACEMENT;     // 101
        BY_BASIC_MODE_BITS[6] = INDEX_8BIT;                // 110
        BY_BASIC_MODE_BITS[7] = ABSOLUTE_SHORT;            // 111 (special: may become ABSOLUTE_LONG)
    }

    /**
     * Lookup basic addressing mode from 3-bit mode field in opcode.
     * @param modeBits value 0-7 from bits 3-5 of opcode
     * @return AddressingMode for basic modes; ABSOLUTE_SHORT is placeholder for 111
     */
    public static AddressingMode fromBasicModeBits(int modeBits) {
        return BY_BASIC_MODE_BITS[modeBits];
    }

    /**
     * Does this mode require reading extension word(s) from the instruction stream?
     * Used by Decoder to know how many words to fetch ahead.
     */
    public boolean needsExtensionWords() {
        return switch (this) {
            case INDIRECT_DISPLACEMENT,
                 INDEX_8BIT,
                 INDEX_BASE_DISPLACEMENT,
                 MEMORY_INDIRECT_POSTINDEXED,
                 MEMORY_INDIRECT_PREINDEXED,
                 PC_RELATIVE_DISPLACEMENT,
                 PC_RELATIVE_INDEXED,
                 PC_MEMORY_INDIRECT_POSTINDEXED,
                 PC_MEMORY_INDIRECT_PREINDEXED,
                 ABSOLUTE_SHORT,    // may need second word → ABSOLUTE_LONG
                 ABSOLUTE_LONG,
                 IMMEDIATE -> true;
            default -> false;
        };
    }

    /**
     * Does this mode modify the address register (post-increment, pre-decrement)?
     * Used for cycle accounting and write-back logic.
     */
    public boolean modifiesAddressRegister() {
        return this == POST_INCREMENT || this == PRE_DECREMENT;
    }

    public boolean isRegisterDirect() {
        return this == DATA_REGISTER_DIRECT || this == ADDRESS_REGISTER_DIRECT;
    }

    public boolean isMemoryAccess() {
        return !isRegisterDirect() && this != IMMEDIATE;
    }

    public String getSyntax() {
        return switch (this) {
            case DATA_REGISTER_DIRECT -> "Dn";
            case ADDRESS_REGISTER_DIRECT -> "An";
            case REGISTER_INDIRECT -> "(An)";
            case POST_INCREMENT -> "(An)+";
            case PRE_DECREMENT -> "-(An)";
            case INDIRECT_DISPLACEMENT -> "d16(An)";
            case INDEX_8BIT -> "d8(An,Xn)";
            case INDEX_BASE_DISPLACEMENT -> "bd(An,Xn)";
            case MEMORY_INDIRECT_POSTINDEXED -> "[d8,An],Xn,od";
            case MEMORY_INDIRECT_PREINDEXED -> "[bd,An,Xn],od";
            case PC_RELATIVE_DISPLACEMENT -> "d16(PC)";
            case PC_RELATIVE_INDEXED -> "d8(PC,Xn)";
            case PC_MEMORY_INDIRECT_POSTINDEXED -> "[d8,PC],Xn,od";
            case PC_MEMORY_INDIRECT_PREINDEXED -> "[bd,PC,Xn],od";
            case ABSOLUTE_SHORT -> "xxx.W";
            case ABSOLUTE_LONG -> "xxx.L";
            case IMMEDIATE -> "#data";
        };
    }
}
