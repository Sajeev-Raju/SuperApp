package com.example.registration.service;

import com.example.registration.dto.PaymentRequest;
import com.example.registration.model.UsernameValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IdGenerationApiClient {

    private final RestTemplate restTemplate;

    @Value("${id.generation.service.url}")
    private String idGenerationServiceUrl;

    public String generateUsername() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                idGenerationServiceUrl + "/generate",
                Map.class
        );
        
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("data")) {
            return (String) responseBody.get("data");
        }
        
        throw new RuntimeException("Failed to generate username");
    }

    public UsernameValidationResult checkUsername(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                idGenerationServiceUrl + "/check",
                request,
                Map.class
        );
        
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("data")) {
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
            
            UsernameValidationResult result = new UsernameValidationResult();
            result.setUsername(username);
            result.setAvailable(Boolean.TRUE.equals(data.get("available")));
            result.setFancy(Boolean.TRUE.equals(data.get("isFancy")));
            result.setFancyType((String) data.get("fancyType"));
            
            if (data.get("fancyPrice") instanceof Integer) {
                result.setFancyPrice(((Integer) data.get("fancyPrice")).doubleValue());
            } else {
                result.setFancyPrice((Double) data.get("fancyPrice"));
            }
            
            return result;
        }
        
        throw new RuntimeException("Failed to check username");
    }

    public String initiatePayment(PaymentRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PaymentRequest> httpEntity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                idGenerationServiceUrl + "/payment/initiate",
                httpEntity,
                Map.class
        );
        
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("data")) {
            return (String) responseBody.get("data");
        }
        
        throw new RuntimeException("Failed to initiate payment");
    }
}