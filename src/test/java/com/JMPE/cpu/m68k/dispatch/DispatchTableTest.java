package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JMPE.cpu.m68k.instructions.Opcode;
import org.junit.jupiter.api.Test;

class DispatchTableTest {
    @Test
    void providesBuiltInAndiHandler() {
        DispatchTable dispatchTable = new DispatchTable();

        assertTrue(dispatchTable.hasHandler(Opcode.ANDI));
        assertTrue(dispatchTable.lookup(Opcode.ANDI) instanceof AndiOp);
    }

    @Test
    void providesBuiltInAndiToCcrHandler() {
        DispatchTable dispatchTable = new DispatchTable();

        assertTrue(dispatchTable.hasHandler(Opcode.ANDI_TO_CCR));
        assertTrue(dispatchTable.lookup(Opcode.ANDI_TO_CCR) instanceof AndiToCcrOp);
    }

    @Test
    void providesBuiltInAndiToSrHandler() {
        DispatchTable dispatchTable = new DispatchTable();

        assertTrue(dispatchTable.hasHandler(Opcode.ANDI_TO_SR));
        assertTrue(dispatchTable.lookup(Opcode.ANDI_TO_SR) instanceof AndiToSrOp);
    }

    @Test
    void providesBuiltInClrHandler() {
        DispatchTable dispatchTable = new DispatchTable();

        assertTrue(dispatchTable.hasHandler(Opcode.CLR));
        assertTrue(dispatchTable.lookup(Opcode.CLR) instanceof ClrOp);
    }

    @Test
    void providesBuiltInCmpiHandler() {
        DispatchTable dispatchTable = new DispatchTable();

        assertTrue(dispatchTable.hasHandler(Opcode.CMPI));
        assertTrue(dispatchTable.lookup(Opcode.CMPI) instanceof CmpiOp);
    }

    @Test
    void providesBuiltInEoriHandler() {
        DispatchTable dispatchTable = new DispatchTable();

        assertTrue(dispatchTable.hasHandler(Opcode.EORI));
        assertTrue(dispatchTable.lookup(Opcode.EORI) instanceof EoriOp);
    }

    @Test
    void providesBuiltInEoriToCcrHandler() {
        DispatchTable dispatchTable = new DispatchTable();

        assertTrue(dispatchTable.hasHandler(Opcode.EORI_TO_CCR));
        assertTrue(dispatchTable.lookup(Opcode.EORI_TO_CCR) instanceof EoriToCcrOp);
    }

    @Test
    void providesBuiltInEoriToSrHandler() {
        DispatchTable dispatchTable = new DispatchTable();

        assertTrue(dispatchTable.hasHandler(Opcode.EORI_TO_SR));
        assertTrue(dispatchTable.lookup(Opcode.EORI_TO_SR) instanceof EoriToSrOp);
    }

    @Test
    void providesBuiltInNopHandler() {
        DispatchTable dispatchTable = new DispatchTable();

        assertTrue(dispatchTable.hasHandler(Opcode.NOP));
        assertTrue(dispatchTable.lookup(Opcode.NOP) instanceof NopOp);
    }

    @Test
    void providesBuiltInNotHandler() {
        DispatchTable dispatchTable = new DispatchTable();

        assertTrue(dispatchTable.hasHandler(Opcode.NOT));
        assertTrue(dispatchTable.lookup(Opcode.NOT) instanceof NotOp);
    }

    @Test
    void providesBuiltInOriHandler() {
        DispatchTable dispatchTable = new DispatchTable();

        assertTrue(dispatchTable.hasHandler(Opcode.ORI));
        assertTrue(dispatchTable.lookup(Opcode.ORI) instanceof OriOp);
    }

    @Test
    void providesBuiltInTstHandler() {
        DispatchTable dispatchTable = new DispatchTable();

        assertTrue(dispatchTable.hasHandler(Opcode.TST));
        assertTrue(dispatchTable.lookup(Opcode.TST) instanceof TstOp);
    }

    @Test
    void providesExtendedBuiltInHandlers() {
        DispatchTable dispatchTable = new DispatchTable();

        assertBuiltInHandler(dispatchTable, Opcode.ADD, AddOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.ADDI, AddiOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.ADDA, AddaOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.ADDX, AddxOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.ADDQ, AddqOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.AND, AndOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.ASL, AslOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.ASR, AsrOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.BCC, BccOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.BCHG, BchgOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.BCLR, BclrOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.BSET, BsetOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.BRA, BraOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.BSR, BsrOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.BTST, BtstOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.CHK, ChkOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.CMP, CmpOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.CMPA, CmpaOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.DBcc, DbccOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.CMPM, CmpmOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.EOR, EorOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.EXT, ExtOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.EXG, ExgOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.JMP, JmpOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.JSR, JsrOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.LEA, LeaOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.LSL, LslOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.LSR, LsrOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.MOVE, MoveOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.MOVE_FROM_SR, MoveFromSrOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.MOVE_TO_CCR, MoveToCcrOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.MOVE_TO_SR, MoveToSrOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.MOVEA, MoveaOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.MOVEM_MEM_TO_REG, MovemMemToRegOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.MOVEM_REG_TO_MEM, MovemRegToMemOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.MOVEQ, MoveQOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.NEG, NegOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.NEGX, NegxOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.OR, OrOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.ORI_TO_CCR, OriToCcrOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.ORI_TO_SR, OriToSrOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.PEA, PeaOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.ROL, RolOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.ROR, RorOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.ROXL, RoxlOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.ROXR, RoxrOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.RTS, RtsOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.Scc, SccOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.SUB, SubOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.SUBA, SubaOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.SUBI, SubiOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.SUBQ, SubqOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.SUBX, SubxOp.class);
        assertBuiltInHandler(dispatchTable, Opcode.SWAP, SwapOp.class);
    }

    @Test
    void returnsRegisteredHandlerFromEmptyTable() {
        DispatchTable dispatchTable = DispatchTable.empty();
        Op handler = (cpu, bus, decoded) -> 4;

        dispatchTable.register(Opcode.RTS, handler);

        assertSame(handler, dispatchTable.lookup(Opcode.RTS));
    }

    @Test
    void tracksWhetherAnOpcodeHasAHandler() {
        DispatchTable dispatchTable = DispatchTable.empty();
        Op handler = (cpu, bus, decoded) -> 4;

        assertFalse(dispatchTable.hasHandler(Opcode.RTS));
        dispatchTable.register(Opcode.RTS, handler);
        assertTrue(dispatchTable.hasHandler(Opcode.RTS));
        assertFalse(dispatchTable.hasHandler(Opcode.JSR));
    }

    @Test
    void rejectsDuplicateRegistration() {
        DispatchTable dispatchTable = DispatchTable.empty();
        Op first = (cpu, bus, decoded) -> 4;
        Op second = (cpu, bus, decoded) -> 8;

        dispatchTable.register(Opcode.RTS, first);

        assertThrows(IllegalStateException.class, () -> dispatchTable.register(Opcode.RTS, second));
    }

    @Test
    void rejectsMissingHandlerLookup() {
        DispatchTable dispatchTable = DispatchTable.empty();

        assertThrows(IllegalStateException.class, () -> dispatchTable.lookup(Opcode.NOP));
    }

    @Test
    void rejectsNullInputs() {
        DispatchTable dispatchTable = new DispatchTable();
        Op handler = (cpu, bus, decoded) -> 4;

        assertThrows(NullPointerException.class, () -> dispatchTable.register(null, handler));
        assertThrows(NullPointerException.class, () -> dispatchTable.register(Opcode.NOP, null));
        assertThrows(NullPointerException.class, () -> dispatchTable.lookup(null));
        assertThrows(NullPointerException.class, () -> dispatchTable.hasHandler(null));
    }

    private static void assertBuiltInHandler(DispatchTable dispatchTable, Opcode opcode, Class<? extends Op> handlerType) {
        assertTrue(dispatchTable.hasHandler(opcode));
        assertTrue(handlerType.isInstance(dispatchTable.lookup(opcode)));
    }
}
