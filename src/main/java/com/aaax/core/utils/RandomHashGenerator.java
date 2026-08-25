package com.aaax.core.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RandomHashGenerator {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static String generateRandomHash(int length) {
        try {
            // Generate a random salt
            byte[] salt = generateRandomSalt();

            // Concatenate the input string and salt
            byte[] inputBytes = (generateRandomString(5) + byteArrayToHexString(salt)).getBytes(StandardCharsets.UTF_8);

            // Create an instance of the SHA-256 algorithm
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            // Compute the hash value of the input string and salt
            byte[] hash = digest.digest(inputBytes);

            // Truncate the hash to the desired length
            byte[] truncatedHash = new byte[length / 2];
            System.arraycopy(hash, 0, truncatedHash, 0, truncatedHash.length);

            // Convert the byte array to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : truncatedHash) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }

            // Return the hash value
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String generateRandomHash(String input, int length) {
        try {
            // Generate a random salt
            byte[] salt = generateRandomSalt();

            // Concatenate the input string and salt
            byte[] inputBytes = (input + byteArrayToHexString(salt)).getBytes(StandardCharsets.UTF_8);

            // Create an instance of the SHA-256 algorithm
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            // Compute the hash value of the input string and salt
            byte[] hash = digest.digest(inputBytes);

            // Truncate the hash to the desired length
            byte[] truncatedHash = new byte[length / 2];
            System.arraycopy(hash, 0, truncatedHash, 0, truncatedHash.length);

            // Convert the byte array to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : truncatedHash) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }

            // Return the hash value
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] generateRandomSalt() {
        byte[] salt = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(salt);
        return salt;
    }

    private static String byteArrayToHexString(byte[] array) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : array) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            char randomChar = CHARACTERS.charAt(randomIndex);
            sb.append(randomChar);
        }
        return sb.toString();
    }
}
