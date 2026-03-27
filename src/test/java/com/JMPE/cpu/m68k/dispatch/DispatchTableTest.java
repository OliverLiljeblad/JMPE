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
    void returnsRegisteredHandlerFromEmptyTable() {
        DispatchTable dispatchTable = DispatchTable.empty();
        Op handler = (cpu, decoded) -> 4;

        dispatchTable.register(Opcode.RTS, handler);

        assertSame(handler, dispatchTable.lookup(Opcode.RTS));
    }

    @Test
    void tracksWhetherAnOpcodeHasAHandler() {
        DispatchTable dispatchTable = DispatchTable.empty();
        Op handler = (cpu, decoded) -> 4;

        assertFalse(dispatchTable.hasHandler(Opcode.RTS));
        dispatchTable.register(Opcode.RTS, handler);
        assertTrue(dispatchTable.hasHandler(Opcode.RTS));
        assertFalse(dispatchTable.hasHandler(Opcode.JSR));
    }

    @Test
    void rejectsDuplicateRegistration() {
        DispatchTable dispatchTable = DispatchTable.empty();
        Op first = (cpu, decoded) -> 4;
        Op second = (cpu, decoded) -> 8;

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
        Op handler = (cpu, decoded) -> 4;

        assertThrows(NullPointerException.class, () -> dispatchTable.register(null, handler));
        assertThrows(NullPointerException.class, () -> dispatchTable.register(Opcode.NOP, null));
        assertThrows(NullPointerException.class, () -> dispatchTable.lookup(null));
        assertThrows(NullPointerException.class, () -> dispatchTable.hasHandler(null));
    }
}
