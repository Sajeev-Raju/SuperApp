package com.example.idgeneration.service;

import com.example.idgeneration.dto.ApiResponse;
import com.example.idgeneration.dto.GenerateUsernameRequest;
import com.example.idgeneration.dto.SuggestedIdsResponse;
import com.example.idgeneration.dto.UsernameCheckRequest;
import com.example.idgeneration.model.Username;
import com.example.idgeneration.repository.UsernameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IdGenerationService {

    private final UsernameRepository usernameRepository;
    private final Random random = new Random();

    @Value("${fancy.id.price.repeated}")
    private double repeatedPrice;

    @Value("${fancy.id.price.sequential}")
    private double sequentialPrice;

    @Value("${fancy.id.price.palindrome}")
    private double palindromePrice;

    @Value("${fancy.id.price.alternating}")
    private double alternatingPrice;

    @Value("${fancy.id.price.premium}")
    private double premiumPrice;

    @Value("${fancy.id.price.special}")
    private double specialPrice;

    @Transactional
    public ApiResponse<String> generateUsername(GenerateUsernameRequest request) {
        String username;
        
        // Keep generating until we find an available one
        do {
            username = generateRandomUsername(request);
        } while (usernameRepository.existsByUsername(username));

        // Save the username as not assigned
        Username newUsername = Username.builder()
                .username(username)
                .isAssigned(false)
                .build();
        usernameRepository.save(newUsername);

        return ApiResponse.success("Username generated successfully", username);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> checkUsername(UsernameCheckRequest request) {
        String username = request.getUsername();
        
        // Check if username exists
        boolean isAvailable = !usernameRepository.existsByUsername(username);
        
        // Check if it's a fancy username
        Map<String, Object> fancyResult = detectFancyPattern(username);
        boolean isFancy = (boolean) fancyResult.get("isFancy");
        String fancyType = (String) fancyResult.get("fancyType");
        double fancyPrice = (double) fancyResult.get("fancyPrice");
        
        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("available", isAvailable);
        result.put("isFancy", isFancy);
        result.put("fancyType", fancyType);
        result.put("fancyPrice", fancyPrice);
        
        return ApiResponse.success("Username checked successfully", result);
    }

    @Transactional
    public void markUsernameAsAssigned(String username) {
        // First, check if the username exists
        if (usernameRepository.existsByUsername(username)) {
            usernameRepository.markAsAssigned(username);
        } else {
            // If it doesn't exist, create and mark as assigned
            Username newUsername = Username.builder()
                    .username(username)
                    .isAssigned(true)
                    .build();
            usernameRepository.save(newUsername);
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<SuggestedIdsResponse> generateSuggestedIds(String email, String phone, int attempt) {
        List<String> fancyIds = generateFancyIds(10);
        List<String> randomIds = generateRandomIds(10);

        SuggestedIdsResponse response = SuggestedIdsResponse.builder()
                .fancyIds(fancyIds)
                .randomIds(randomIds)
                .attempt(attempt)
                .build();

        return ApiResponse.success("Suggested IDs generated successfully", response);
    }

    private String generateRandomUsername(GenerateUsernameRequest request) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String digits = "0123456789";
        StringBuilder sb = new StringBuilder();
        
        // Use prefix if provided
        if (request != null && request.getPrefix() != null && !request.getPrefix().isEmpty()) {
            sb.append(request.getPrefix());
        } else {
            // Generate 3 random letters
            for (int i = 0; i < 3; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
        }
        
        // Use suffix if provided
        if (request != null && request.getSuffix() != null && !request.getSuffix().isEmpty()) {
            sb.append(request.getSuffix());
        } else {
            // Generate 3 random digits
            for (int i = 0; i < 3; i++) {
                sb.append(digits.charAt(random.nextInt(digits.length())));
            }
        }
        
        return sb.toString();
    }

    private Map<String, Object> detectFancyPattern(String username) {
        Map<String, Object> result = new HashMap<>();
        result.put("isFancy", false);
        result.put("fancyType", "");
        result.put("fancyPrice", 0.0);
        
        // Convert to uppercase for pattern checking
        String upperUsername = username.toUpperCase();
        
        // Check for repeated letters or digits (e.g., AAA111)
        if (upperUsername.matches(".*?(.)\\1{2,}.*?")) {
            result.put("isFancy", true);
            result.put("fancyType", "Repeated Characters");
            result.put("fancyPrice", repeatedPrice);
            return result;
        }
        
        // Check for sequential patterns (e.g., ABC123)
        String[] sequences = {
            "ABC", "BCD", "CDE", "DEF", "EFG", "FGH", "GHI", "HIJ", "IJK", "JKL",
            "KLM", "LMN", "MNO", "NOP", "OPQ", "PQR", "QRS", "RST", "STU", "TUV",
            "UVW", "VWX", "WXY", "XYZ", "YZA", "ZAB", "123", "234", "345", "456",
            "567", "678", "789", "890"
        };
        
        for (String seq : sequences) {
            if (upperUsername.contains(seq)) {
                result.put("isFancy", true);
                result.put("fancyType", "Sequential Characters");
                result.put("fancyPrice", sequentialPrice);
                return result;
            }
        }
        
        // Check for palindrome pattern (e.g., ABCCBA)
        if (upperUsername.length() >= 6) {
            boolean isPalindrome = true;
            for (int i = 0; i < upperUsername.length() / 2; i++) {
                if (upperUsername.charAt(i) != upperUsername.charAt(upperUsername.length() - 1 - i)) {
                    isPalindrome = false;
                    break;
                }
            }
            
            if (isPalindrome) {
                result.put("isFancy", true);
                result.put("fancyType", "Palindrome");
                result.put("fancyPrice", palindromePrice);
                return result;
            }
        }
        
        // Check for alternating pattern (e.g., ABCABC)
        if (upperUsername.length() >= 6) {
            String first3 = upperUsername.substring(0, 3);
            String second3 = upperUsername.substring(3, 6);
            if (first3.equals(second3)) {
                result.put("isFancy", true);
                result.put("fancyType", "Alternating Pattern");
                result.put("fancyPrice", alternatingPrice);
                return result;
            }
        }
        
        // Check for premium words (e.g., VIP, CEO)
        String[] premiumWords = {"VIP", "CEO", "GOD", "BOSS", "KING", "QUEEN"};
        for (String word : premiumWords) {
            if (upperUsername.contains(word)) {
                result.put("isFancy", true);
                result.put("fancyType", "Premium Word");
                result.put("fancyPrice", premiumPrice);
                return result;
            }
        }
        
        // Check for special patterns like AAA123 or ABC111
        if (upperUsername.length() >= 6) {
            String first3 = upperUsername.substring(0, 3);
            String second3 = upperUsername.substring(3, 6);
            
            // Check if first3 has repeated characters and second3 has sequential pattern
            if ((first3.charAt(0) == first3.charAt(1) && first3.charAt(1) == first3.charAt(2)) &&
                    containsSequential(second3)) {
                result.put("isFancy", true);
                result.put("fancyType", "Special Pattern");
                result.put("fancyPrice", specialPrice);
                return result;
            }
            
            // Check if first3 has sequential pattern and second3 has repeated characters
            if (containsSequential(first3) &&
                    (second3.charAt(0) == second3.charAt(1) && second3.charAt(1) == second3.charAt(2))) {
                result.put("isFancy", true);
                result.put("fancyType", "Special Pattern");
                result.put("fancyPrice", specialPrice);
                return result;
            }
        }
        
        return result;
    }

    private boolean containsSequential(String str) {
        String[] sequences = {
            "ABC", "BCD", "CDE", "DEF", "EFG", "FGH", "GHI", "HIJ", "IJK", "JKL",
            "KLM", "LMN", "MNO", "NOP", "OPQ", "PQR", "QRS", "RST", "STU", "TUV",
            "UVW", "VWX", "WXY", "XYZ", "YZA", "ZAB", "123", "234", "345", "456",
            "567", "678", "789", "890"
        };
        
        for (String seq : sequences) {
            if (str.equals(seq)) {
                return true;
            }
        }
        
        return false;
    }

    private List<String> generateFancyIds(int count) {
        List<String> fancyIds = new ArrayList<>();
        Set<String> generatedIds = new HashSet<>();

        // Premium patterns (e.g., VIP, CEO)
        String[] premiumWords = {"VIP", "CEO", "GOD", "BOSS", "KING", "QUEEN"};
        
        while (fancyIds.size() < count) {
            String id = generateFancyId(premiumWords);
            if (!generatedIds.contains(id) && !usernameRepository.existsByUsername(id)) {
                fancyIds.add(id);
                generatedIds.add(id);
            }
        }

        return fancyIds;
    }

    private String generateFancyId(String[] premiumWords) {
        int pattern = random.nextInt(6); // 6 different patterns

        switch (pattern) {
            case 0: // Repeated pattern (e.g., AAA111)
                return generateRepeatedPattern();
            case 1: // Sequential pattern (e.g., ABC123)
                return generateSequentialPattern();
            case 2: // Premium word pattern (e.g., VIP888)
                return generatePremiumPattern(premiumWords);
            case 3: // Palindrome pattern (e.g., ABA123)
                return generatePalindromePattern();
            case 4: // Alternating pattern (e.g., ABCABC)
                return generateAlternatingPattern();
            default: // Special pattern (e.g., AAA123)
                return generateSpecialPattern();
        }
    }

    private String generateRepeatedPattern() {
        char letter = (char) ('A' + random.nextInt(26));
        int number = random.nextInt(900) + 100;
        return String.format("%c%c%c%03d", letter, letter, letter, number);
    }

    private String generateSequentialPattern() {
        int startLetter = random.nextInt(24); // Leave room for 3 sequential letters
        int number = random.nextInt(900) + 100;
        return String.format("%c%c%c%03d", 
                (char)('A' + startLetter),
                (char)('A' + startLetter + 1),
                (char)('A' + startLetter + 2),
                number);
    }

    private String generatePremiumPattern(String[] premiumWords) {
        String word = premiumWords[random.nextInt(premiumWords.length)];
        int number = random.nextInt(900) + 100;
        return String.format("%s%03d", word, number);
    }

    private String generatePalindromePattern() {
        char first = (char) ('A' + random.nextInt(26));
        char middle = (char) ('A' + random.nextInt(26));
        int number = random.nextInt(900) + 100;
        return String.format("%c%c%c%03d", first, middle, first, number);
    }

    private String generateAlternatingPattern() {
        char first = (char) ('A' + random.nextInt(26));
        char second = (char) ('A' + random.nextInt(26));
        char third = (char) ('A' + random.nextInt(26));
        return String.format("%c%c%c%c%c%c", first, second, third, first, second, third);
    }

    private String generateSpecialPattern() {
        char letter = (char) ('A' + random.nextInt(26));
        int number = random.nextInt(900) + 100;
        return String.format("%c%c%c%03d", letter, letter, letter, number);
    }

    private List<String> generateRandomIds(int count) {
        List<String> randomIds = new ArrayList<>();
        Set<String> generatedIds = new HashSet<>();

        while (randomIds.size() < count) {
            String id = generateRandomUsername(null);
            if (!generatedIds.contains(id) && !usernameRepository.existsByUsername(id)) {
                randomIds.add(id);
                generatedIds.add(id);
            }
        }

        return randomIds;
    }
}