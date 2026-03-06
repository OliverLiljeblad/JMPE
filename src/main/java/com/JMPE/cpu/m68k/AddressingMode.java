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
