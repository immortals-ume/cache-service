package com.immortals.cacheservice.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

public final class MockValueUtils {
    private static final Random random = new Random();

    // Generate a random string
    public static String generateRandomString() {
        return UUID.randomUUID().toString();
    }

    // Generate a random integer
    public static int generateRandomInt() {
        return random.nextInt();
    }

    // Generate a random long
    public static long generateRandomLong() {
        return random.nextLong();
    }

    // Generate a random float
    public static float generateRandomFloat() {
        return random.nextFloat();
    }

    // Generate a random double
    public static double generateRandomDouble() {
        return random.nextDouble();
    }

    // Generate a random boolean
    public static boolean generateRandomBoolean() {
        return random.nextBoolean();
    }

    // Generate a random date in the format YYYY-MM-DD
    public static String generateRandomDate() {
        int minDay = (int) LocalDate.of(2000, 1, 1).toEpochDay();
        int maxDay = (int) LocalDate.of(2030, 12, 31).toEpochDay();
        long randomDay = minDay + random.nextInt(maxDay - minDay);

        LocalDate randomDate = LocalDate.ofEpochDay(randomDay);
        return randomDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    // Generate a random date in a custom format
    public static String generateRandomDate(String format) {
        int minDay = (int) LocalDate.of(2000, 1, 1).toEpochDay();
        int maxDay = (int) LocalDate.of(2030, 12, 31).toEpochDay();
        long randomDay = minDay + random.nextInt(maxDay - minDay);

        LocalDate randomDate = LocalDate.ofEpochDay(randomDay);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return randomDate.format(formatter);
    }

    // Convert object to string
    public static String convertToString(Object value) {
        return value == null ? null : value.toString();
    }

    // Convert object to int
    public static int convertToInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0; // Default value
        }
    }

    // Convert object to long
    public static long convertToLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L; // Default value
        }
    }

    // Convert object to float
    public static float convertToFloat(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            return 0.0f; // Default value
        }
    }

    // Convert object to double
    public static double convertToDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0; // Default value
        }
    }
}