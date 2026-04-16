package com.JMPE.harness;

import com.JMPE.cpu.m68k.M68kCpu;

public final class DiffPrinter {
    private DiffPrinter() {
    }

    public static String cpuMismatch(SingleStepLoader.CaseSpec caseSpec,
                                     String field,
                                     long expected,
                                     long actual,
                                     M68kCpu.StepReport report,
                                     String lastLog) {
        return "Single-step case '" + caseSpec.displayName() + "' mismatched " + field
            + ": expected " + formatLong(expected) + " but was " + formatLong(actual)
            + ". Cycles=" + report.cycles() + ". Last log: " + lastLog(lastLog);
    }

    public static String wordMismatch(SingleStepLoader.CaseSpec caseSpec,
                                      String field,
                                      int expected,
                                      int actual,
                                      M68kCpu.StepReport report,
                                      String lastLog) {
        return "Single-step case '" + caseSpec.displayName() + "' mismatched " + field
            + ": expected " + formatWord(expected) + " but was " + formatWord(actual)
            + ". Cycles=" + report.cycles() + ". Last log: " + lastLog(lastLog);
    }

    public static String cycleMismatch(SingleStepLoader.CaseSpec caseSpec,
                                       int expected,
                                       int actual,
                                       String lastLog) {
        return "Single-step case '" + caseSpec.displayName() + "' mismatched cycles"
            + ": expected " + expected + " but was " + actual
            + ". Last log: " + lastLog(lastLog);
    }

    public static String memoryMismatch(SingleStepLoader.CaseSpec caseSpec,
                                        int address,
                                        int expected,
                                        int actual,
                                        String lastLog) {
        return "Single-step case '" + caseSpec.displayName() + "' mismatched memory at "
            + formatLong(address & 0xFFFF_FFFFL)
            + ": expected " + formatByte(expected) + " but was " + formatByte(actual)
            + ". Last log: " + lastLog(lastLog);
    }

    private static String formatByte(int value) {
        return String.format("0x%02X", value & 0xFF);
    }

    private static String formatWord(int value) {
        return String.format("0x%04X", value & 0xFFFF);
    }

    private static String formatLong(long value) {
        return String.format("0x%08X", value & 0xFFFF_FFFFL);
    }

    private static String lastLog(String lastLog) {
        return lastLog == null ? "<none>" : lastLog;
    }
}
