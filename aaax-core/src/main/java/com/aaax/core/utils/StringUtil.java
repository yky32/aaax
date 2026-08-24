package com.aaax.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Random;

@Slf4j
public class StringUtil {

    public static long ipToLong(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < ipAddressInArray.length; i++) {
            int power = 3 - i;
            int ip = Integer.parseInt(ipAddressInArray[i]);
            result += (long) (ip * Math.pow(256, power));
        }
        return result;
    }

    public static String randomDigit(int digits) {
        Random random = new Random();
        int minValue = (int) Math.pow(10, digits - 1);
        int maxValue = (int) Math.pow(10, digits) - 1;
        int randomNumber = random.nextInt(maxValue - minValue + 1) + minValue;
        return String.format("%0" + digits + "d", randomNumber);
    }

    public static String randomAlphanumeric(int digits) {
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        return random(digits, characters);
    }

    public static String randomLetters(int digits) {
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        return random(digits, characters);
    }

    public static @NotNull String random(int digits, String characters) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    public static String maskString(String input, int numDigitsToMask, char maskCharacter) {
        int length = input.length();
        if (length <= numDigitsToMask) {
            return input;
        } else {
            String maskedPart = input.substring(0, numDigitsToMask).replaceAll(".", String.valueOf(maskCharacter));
            String remainingPart = input.substring(numDigitsToMask);
            return maskedPart + remainingPart;
        }
    }

    public static String maskString(String input, int start, int end, char maskCharacter) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        int inputLength = input.length();
        if (start < 0 || start >= inputLength || end < 0 || end > inputLength || start >= end) {
            return input;
        }

        char[] maskedChars = new char[end - start];
        Arrays.fill(maskedChars, maskCharacter);
        String maskedString = new String(maskedChars);

        return input.substring(0, start) + maskedString + input.substring(end);
    }

    /**
     * @param input variable format: "ABC_DEF"
     * @return variable format: "abcDef"
     */
    public static String toCamelFormat(String input) {
        String[] parts = input.split("_");
        if (parts.length == 0) {
            return input.toLowerCase();
        }
        String result = parts[0].toLowerCase();
        for (int i = 1; i < parts.length; i++) {
            result = result.concat(parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1).toLowerCase());
        }
        return result;
    }
}
