package ru.practicum.mainservice.util;

public class LogColorizeUtil {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";

    public static String colorizeClass(String className) {
        return ANSI_BLUE + className + ANSI_RESET;
    }

    public static String colorizeMethod(String methodName) {
        return ANSI_GREEN + methodName + ANSI_RESET;
    }

    public static String colorizeError(String error) {
        return ANSI_RED + error + ANSI_RESET;
    }
}
