package com.JMPE.bus;

import com.JMPE.cpu.m68k.exceptions.AddressErrorException;
import com.JMPE.cpu.m68k.exceptions.BusErrorException;

/**
 * The system bus — the CPU's and decoder's view of the entire address space.
 *
 * <h2>Responsibility boundary</h2>
 * {@code Bus} is a pure interface.  It exposes the six read/write operations
 * that the 68000 programmer's model defines (byte, word, long × read/write)
 * and nothing else.  It knows nothing about regions, mirroring, or devices.
 * All of that lives in {@link AddressSpace}, the sole production implementation.
 *
 * <h2>Why an interface rather than an abstract class</h2>
 * <ul>
 *   <li>Tests can supply a lightweight stub or mock without pulling in the
 *       full region machinery.  The {@link com.JMPE.cpu.m68k.Decoder} Javadoc
 *       already notes that {@code null} may be passed for extension-word-free
 *       instructions; a proper stub is cleaner.</li>
 *   <li>The {@link com.JMPE.ui.debugger.Disassembler} can wrap an
 *       {@code AddressSpace} in a read-only view that throws on any write,
 *       without changing the type seen by callers.</li>
 * </ul>
 *
 * <h2>Signed vs unsigned</h2>
 * Java has no unsigned integer types.  All read methods return {@code int},
 * with values zero-extended into the appropriate number of bits:
 * <ul>
 *   <li>{@link #readByte}  — value in bits [7:0],  bits [31:8]  = 0</li>
 *   <li>{@link #readWord}  — value in bits [15:0], bits [31:16] = 0</li>
 *   <li>{@link #readLong}  — full 32-bit value (callers treat as unsigned
 *       where the 68000 would, e.g. for addresses)</li>
 * </ul>
 * Write methods use only the low bits of the supplied {@code value}; the
 * upper bits are silently ignored.
 *
 * <h2>Alignment and exceptions</h2>
 * The 68000 requires that word and long accesses use even addresses.
 * Accessing an odd address for a word or long read/write causes the
 * {@code AddressSpace} implementation to throw
 * {@link AddressErrorException}, which the CPU routes to the 68000
 * Address Error exception vector.  Callers need not pre-check alignment.
 *
 * <p>An access to an unmapped region throws {@link BusErrorException},
 * routed to the 68000 Bus Error exception vector.
 *
 * <h2>Endianness</h2>
 * All multi-byte accesses are big-endian, matching the 68000 and the Mac Plus
 * hardware.  The high byte is at the lower address.
 */
public interface Bus {

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    /**
     * Reads one byte at {@code address}.
     * The result is zero-extended to 32 bits.
     *
     * @param address the bus address (only the low 24 bits are significant)
     * @return the byte value in bits [7:0]
     * @throws BusErrorException if the address is unmapped
     */
    int readByte(int address) throws BusErrorException;

    /**
     * Reads one big-endian word (2 bytes) at {@code address}.
     * The result is zero-extended to 32 bits.
     *
     * @param address the bus address; must be even
     * @return the word value in bits [15:0]
     * @throws AddressErrorException if {@code address} is odd
     * @throws BusErrorException     if the address is unmapped
     */
    int readWord(int address) throws BusErrorException, AddressErrorException;

    /**
     * Reads one big-endian long word (4 bytes) at {@code address}.
     *
     * @param address the bus address; must be even
     * @return the 32-bit value
     * @throws AddressErrorException if {@code address} is odd
     * @throws BusErrorException     if the address is unmapped
     */
    int readLong(int address) throws BusErrorException, AddressErrorException;

    // -------------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------------

    /**
     * Writes the low byte of {@code value} to {@code address}.
     *
     * @param address the bus address
     * @param value   only bits [7:0] are written; upper bits are ignored
     * @throws BusErrorException if the address is unmapped
     */
    void writeByte(int address, int value) throws BusErrorException;

    /**
     * Writes the low 16 bits of {@code value} as a big-endian word to
     * {@code address}.
     *
     * @param address the bus address; must be even
     * @param value   only bits [15:0] are written; upper bits are ignored
     * @throws AddressErrorException if {@code address} is odd
     * @throws BusErrorException     if the address is unmapped
     */
    void writeWord(int address, int value) throws BusErrorException, AddressErrorException;

    /**
     * Writes all 32 bits of {@code value} as a big-endian long word to
     * {@code address}.
     *
     * @param address the bus address; must be even
     * @throws AddressErrorException if {@code address} is odd
     * @throws BusErrorException     if the address is unmapped
     */
    void writeLong(int address, int value) throws AddressErrorException, BusErrorException;

    // -------------------------------------------------------------------------
    // Default helpers — convenience wrappers used by executors
    // -------------------------------------------------------------------------

    /**
     * Reads one byte and sign-extends it to 32 bits.
     * Equivalent to {@code (byte) readByte(address)}.
     */
    default int readByteSigned(int address) throws BusErrorException {
        return (byte) readByte(address);
    }

    /**
     * Reads one word and sign-extends it to 32 bits.
     * Equivalent to {@code (short) readWord(address)}.
     */
    default int readWordSigned(int address) throws AddressErrorException, BusErrorException {
        return (short) readWord(address);
    }
}
