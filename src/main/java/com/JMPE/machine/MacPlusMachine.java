package com.JMPE.machine;

import com.JMPE.bus.AddressSpace;
import com.JMPE.bus.Bus;
import com.JMPE.bus.MemoryRegion;
import com.JMPE.bus.Mmio;
import com.JMPE.bus.OverlayMemoryRegion;
import com.JMPE.bus.Rom;
import com.JMPE.bus.RomRegion;
import com.JMPE.cpu.m68k.M68kCpu;
import com.JMPE.cpu.m68k.dispatch.DispatchTable;
import com.JMPE.cpu.m68k.exceptions.IllegalInstructionException;
import com.JMPE.devices.via.Via6522;
import com.JMPE.util.RomLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Coarse machine composition entry point for integration tests.
 */
public final class MacPlusMachine {
    private static final int LOW_MEMORY_BASE = 0x0000_0000;
    private static final int LOW_MEMORY_SIZE = 0x0040_0000;
    private static final int MAC_PLUS_ROM_BASE = 0x0040_0000;
    private static final int MAC_PLUS_ROM_APERTURE_SIZE = 0x0010_0000;
    private static final int SCSI_BASE = 0x0058_0000;
    private static final int SCSI_SIZE = 0x0008_0000;
    private static final int SCC_READ_BASE = 0x0080_0000;
    private static final int SCC_WRITE_BASE = 0x00A0_0000;
    private static final int DEVICE_WINDOW_SIZE = 0x0020_0000;
    private static final int IWM_BASE = 0x00C0_0000;
    private static final int VIA_BASE = 0x00E8_0000;
    private static final int VIA_SIZE = 0x0008_0000;
    private static final int OPEN_BUS_BASE = 0x00F0_0000;
    private static final int OPEN_BUS_SIZE = 0x000F_FFF0;
    private static final int VIA_OVERLAY_BIT = 4;

    private final Rom rom;
    private final M68kCpu cpu;
    private final AddressSpace bus;
    private final DispatchTable dispatchTable;

    public MacPlusMachine(Rom rom) {
        this(rom, new M68kCpu(), false);
    }

    public MacPlusMachine(Rom rom, M68kCpu cpu) {
        this(rom, cpu, false, new MemoryRegion[0]);
    }

    public MacPlusMachine(Rom rom, M68kCpu cpu, MemoryRegion... additionalRegions) {
        this(rom, cpu, false, additionalRegions);
    }

    private MacPlusMachine(Rom rom, M68kCpu cpu, boolean overlayEnabledAtReset, MemoryRegion... additionalRegions) {
        if (rom == null) {
            throw new IllegalArgumentException("rom must not be null");
        }
        if (cpu == null) {
            throw new IllegalArgumentException("cpu must not be null");
        }
        if (additionalRegions == null) {
            throw new IllegalArgumentException("additionalRegions must not be null");
        }
        this.rom = rom;
        this.cpu = cpu;
        this.bus = new AddressSpace();
        this.dispatchTable = new DispatchTable();

        MemoryRegion lowMemoryBacking = null;
        List<MemoryRegion> extraRegions = new ArrayList<>(additionalRegions.length);
        for (MemoryRegion region : additionalRegions) {
            if (region == null) {
                throw new IllegalArgumentException("additional region must not be null");
            }
            if (region.base() == LOW_MEMORY_BASE) {
                if (lowMemoryBacking != null) {
                    throw new IllegalArgumentException("Only one low-memory backing region may start at address 0");
                }
                lowMemoryBacking = region;
                continue;
            }
            extraRegions.add(region);
        }

        OverlayMemoryRegion overlayRegion = lowMemoryBacking == null
            ? null
            : new OverlayMemoryRegion(LOW_MEMORY_BASE, LOW_MEMORY_SIZE, rom, lowMemoryBacking, overlayEnabledAtReset);
        Via6522 via = new Via6522(portA -> {
            if (overlayRegion != null) {
                overlayRegion.setOverlayEnabled(((portA >>> VIA_OVERLAY_BIT) & 1) != 0);
            }
        });

        if (overlayRegion != null) {
            overlayRegion.setOverlayEnabled(overlayEnabledAtReset);
            this.bus.addRegion(overlayRegion);
        }
        for (MemoryRegion region : extraRegions) {
            this.bus.addRegion(region);
        }
        this.bus.addRegion(mainRomRegion(rom));
        this.bus.addRegion(Mmio.openBus(SCSI_BASE, SCSI_SIZE));
        this.bus.addRegion(Mmio.openBus(SCC_READ_BASE, DEVICE_WINDOW_SIZE));
        this.bus.addRegion(Mmio.openBus(SCC_WRITE_BASE, DEVICE_WINDOW_SIZE));
        this.bus.addRegion(Mmio.openBus(IWM_BASE, DEVICE_WINDOW_SIZE));
        this.bus.addRegion(viaMmio(via));
        this.bus.addRegion(Mmio.openBus(OPEN_BUS_BASE, OPEN_BUS_SIZE));
        this.cpu.resetFromRom(rom);
    }

    public static MacPlusMachine fromRomBytes(byte[] romBytes, int baseAddress) {
        return new MacPlusMachine(RomLoader.fromBytes(romBytes, baseAddress));
    }

    public static MacPlusMachine fromRomBytes(byte[] romBytes, int baseAddress, MemoryRegion... additionalRegions) {
        return new MacPlusMachine(RomLoader.fromBytes(romBytes, baseAddress), new M68kCpu(), additionalRegions);
    }

    public static MacPlusMachine bootMachine(Rom rom, MemoryRegion... additionalRegions) {
        return new MacPlusMachine(rom, new M68kCpu(), true, additionalRegions);
    }

    public static MacPlusMachine bootMachine(Rom rom, M68kCpu cpu, MemoryRegion... additionalRegions) {
        return new MacPlusMachine(rom, cpu, true, additionalRegions);
    }

    public Rom rom() {
        return rom;
    }

    public M68kCpu cpu() {
        return cpu;
    }

    public Bus bus() {
        return bus;
    }

    public M68kCpu.StepReport step() throws IllegalInstructionException {
        return cpu.step(bus, dispatchTable);
    }

    public M68kCpu.StepReport step(Consumer<String> reporter) throws IllegalInstructionException {
        return cpu.step(bus, dispatchTable, reporter);
    }

    public M68kCpu.StepReport stepWithConsoleReport() throws IllegalInstructionException {
        return cpu.stepWithConsoleReport(bus, dispatchTable);
    }

    private static MemoryRegion mainRomRegion(Rom rom) {
        if (rom.baseAddress() == MAC_PLUS_ROM_BASE) {
            return new RomRegion(rom, MAC_PLUS_ROM_BASE, MAC_PLUS_ROM_APERTURE_SIZE);
        }
        return new RomRegion(rom);
    }

    private static Mmio viaMmio(Via6522 via) {
        return Mmio.readWrite(
            VIA_BASE,
            VIA_SIZE,
            offset -> via.readRegister(viaRegister(offset)),
            (offset, value) -> via.writeRegister(viaRegister(offset), value)
        );
    }

    private static int viaRegister(int offset) {
        return (offset >>> 9) & 0x0F;
    }
}
