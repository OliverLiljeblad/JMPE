package com.JMPE.cpu.m68k.dispatch;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.JMPE.cpu.m68k.EffectiveAddress;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.Size;
import com.JMPE.cpu.m68k.exceptions.PrivilegeViolation;
import com.JMPE.cpu.m68k.instructions.DecodedInstruction;
import com.JMPE.cpu.m68k.instructions.Opcode;
import org.junit.jupiter.api.Test;

class UspMoveOpTest {
    @Test
    void moveToUspCopiesAddressRegisterWithoutChangingActiveSupervisorStack() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setSupervisor(true);
        cpu.registers().setSupervisorStackPointer(0x0000_2000);
        cpu.registers().setAddress(2, 0x0000_3456);

        int cycles = new MoveToUspOp().execute(cpu, null, moveToUsp(2));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x0000_3456, cpu.registers().userStackPointer()),
            () -> assertEquals(0x0000_2000, cpu.registers().stackPointer())
        );
    }

    @Test
    void moveFromUspCopiesStoredUserStackPointerIntoAddressRegister() {
        M68kCpu cpu = new M68kCpu();
        cpu.statusRegister().setSupervisor(true);
        cpu.registers().setUserStackPointer(0x0000_4567);

        int cycles = new MoveFromUspOp().execute(cpu, null, moveFromUsp(3));

        assertAll(
            () -> assertEquals(4, cycles),
            () -> assertEquals(0x0000_4567, cpu.registers().address(3))
        );
    }

    @Test
    void moveUspOpsRequireSupervisorMode() {
        M68kCpu cpu = new M68kCpu();

        assertEquals(
            "MOVE to USP requires supervisor mode",
            assertThrows(PrivilegeViolation.class, () -> new MoveToUspOp().execute(cpu, null, moveToUsp(0))).getMessage()
        );
        assertEquals(
            "MOVE from USP requires supervisor mode",
            assertThrows(PrivilegeViolation.class, () -> new MoveFromUspOp().execute(cpu, null, moveFromUsp(0))).getMessage()
        );
    }

    private static DecodedInstruction moveToUsp(int register) {
        return new DecodedInstruction(Opcode.MOVE_TO_USP, Size.LONG, EffectiveAddress.addrReg(register), EffectiveAddress.none(), 0, 0x1002);
    }

    private static DecodedInstruction moveFromUsp(int register) {
        return new DecodedInstruction(Opcode.MOVE_FROM_USP, Size.LONG, EffectiveAddress.none(), EffectiveAddress.addrReg(register), 0, 0x1002);
    }
}
