package com.aaax.utils;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;

public class PasswordUtil {

    public static String generateCommonLangPassword() {
        String upperCaseLetters = RandomStringUtils.random(8, 65, 90, true, true);
        String lowerCaseLetters = RandomStringUtils.random(8, 97, 122, true, true);
        String numbers = RandomStringUtils.randomNumeric(8);
//        String specialChar = RandomStringUtils.random(4, 33, 47, false, false);
        String totalChars = RandomStringUtils.randomAlphanumeric(8);
        String combinedChars = upperCaseLetters.concat(lowerCaseLetters)
                .concat(numbers)
//                .concat(specialChar)
                .concat(totalChars);
        List<Character> pwdChars = combinedChars.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(pwdChars);
        String password = pwdChars.stream()
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
        return password;
    }
}