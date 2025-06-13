package com.example.registration.service;

import com.example.registration.model.UsernameValidationResult;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class UsernameFancyDetectorService {

    // Patterns for fancy username detection
    private final Pattern repeatedLettersDigits = Pattern.compile("(.)\\1{2,}"); // AAA, 111
    private final Pattern sequentialPattern = Pattern.compile("(ABC|BCD|CDE|DEF|EFG|FGH|GHI|HIJ|IJK|JKL|KLM|LMN|MNO|NOP|OPQ|PQR|QRS|RST|STU|TUV|UVW|VWX|WXY|XYZ|YZA|ZAB|123|234|345|456|567|678|789|890)");
    private final Pattern palindromePattern = Pattern.compile("(.)(.)(.)\\3\\2\\1"); // ABCCBA
    private final Pattern alternatingPattern = Pattern.compile("(.)(.)(.)\\1\\2\\3"); // ABCABC
    private final Pattern premiumWords = Pattern.compile("(VIP|CEO|GOD|BOSS|KING|QUEEN)");

    public UsernameValidationResult detectFancyPattern(String username) {
        UsernameValidationResult result = new UsernameValidationResult();
        result.setUsername(username);
        result.setAvailable(true);
        result.setFancy(false);
        result.setFancyPrice(0);

        // Convert to uppercase for pattern checking
        String upperUsername = username.toUpperCase();

        // Repeated letters or digits (e.g., AAA111)
        if (repeatedLettersDigits.matcher(upperUsername).find()) {
            result.setFancy(true);
            result.setFancyType("Repeated Characters");
            result.setFancyPrice(50);
            return result;
        }

        // Sequential letters/digits (e.g., ABC123)
        if (sequentialPattern.matcher(upperUsername).find()) {
            result.setFancy(true);
            result.setFancyType("Sequential Characters");
            result.setFancyPrice(50);
            return result;
        }

        // Palindrome (e.g., ABCCBA)
        if (palindromePattern.matcher(upperUsername).find()) {
            result.setFancy(true);
            result.setFancyType("Palindrome");
            result.setFancyPrice(50);
            return result;
        }

        // Alternating pattern (e.g., ABCABC)
        if (alternatingPattern.matcher(upperUsername).find()) {
            result.setFancy(true);
            result.setFancyType("Alternating Pattern");
            result.setFancyPrice(50);
            return result;
        }

        // Premium words (e.g., VIP, CEO)
        if (premiumWords.matcher(upperUsername).find()) {
            result.setFancy(true);
            result.setFancyType("Premium Word");
            result.setFancyPrice(100);
            return result;
        }

        // Special patterns like AAA123 or ABC111
        if ((repeatedLettersDigits.matcher(upperUsername.substring(0, 3)).find() && 
             sequentialPattern.matcher(upperUsername.substring(3)).find()) ||
            (sequentialPattern.matcher(upperUsername.substring(0, 3)).find() && 
             repeatedLettersDigits.matcher(upperUsername.substring(3)).find())) {
            result.setFancy(true);
            result.setFancyType("Special Pattern");
            result.setFancyPrice(50);
            return result;
        }

        return result;
    }
}