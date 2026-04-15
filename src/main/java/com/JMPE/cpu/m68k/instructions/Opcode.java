package com.JMPE.cpu.m68k.instructions;

public enum Opcode {

    // ── Data movement ──────────────────────────────────────────────────────
    MOVE, MOVEA, MOVEQ, MOVEM, MOVEP,
    LEA, PEA,
    EXG, SWAP,

    // ── Arithmetic ─────────────────────────────────────────────────────────
    ADD, ADDA, ADDI, ADDQ, ADDX,
    SUB, SUBA, SUBI, SUBQ, SUBX,
    NEG, NEGX,
    MULS, MULU,
    DIVS, DIVU,
    ABCD, SBCD,
    CMP, CMPA, CMPI, CMPM,
    CLR,
    EXT,

    // ── Logic ──────────────────────────────────────────────────────────────
    AND, ANDI, OR, ORI, EOR, EORI, NOT,

    // CCR/SR variants — decoded as distinct opcodes so executors and the
    // disassembler can identify privileged writes without inspecting the EA
    ANDI_TO_CCR, ANDI_TO_SR,
    ORI_TO_CCR,  ORI_TO_SR,
    EORI_TO_CCR, EORI_TO_SR,
    MOVE_TO_CCR, MOVE_TO_SR, MOVE_FROM_SR,

    // ── Bit manipulation ───────────────────────────────────────────────────
    BTST, BCHG, BCLR, BSET,

    // ── Shift / rotate ─────────────────────────────────────────────────────
    LSL, LSR,
    ASL, ASR,
    ROL, ROR,
    ROXL, ROXR,

    // ── Branch / control flow ──────────────────────────────────────────────
    BRA, BSR,
    BCC,        // covers all Bcc variants; condition code stored in DecodedInstruction
    DBcc,
    Scc,
    JMP, JSR,
    RTS, RTR, RTE,

    // ── System ─────────────────────────────────────────────────────────────
    TRAP,
    TRAPV,
    STOP,
    NOP,
    RESET,
    LINK, UNLK,
    ILLEGAL,
    CHK,

    // ── Additional decoded variants ────────────────────────────────────────
    LINE_A_TRAP,
    NBCD, TAS, TST, MOVEM_MEM_TO_REG, MOVEM_REG_TO_MEM,
    MOVE_TO_USP,
    MOVE_FROM_USP,
    LINE_F_TRAP,
}
