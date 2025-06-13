package com.example.idgeneration.controller;

import com.example.idgeneration.dto.ApiResponse;
import com.example.idgeneration.dto.GenerateUsernameRequest;
import com.example.idgeneration.dto.SuggestedIdsResponse;
import com.example.idgeneration.dto.UsernameCheckRequest;
import com.example.idgeneration.service.IdGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/id-generation")
@RequiredArgsConstructor
public class IdGenerationController {

    private final IdGenerationService idGenerationService;

    @GetMapping("/generate")
    public ResponseEntity<ApiResponse<String>> generateUsername(
            @RequestParam(required = false) String prefix,
            @RequestParam(required = false) String suffix) {
        
        GenerateUsernameRequest request = GenerateUsernameRequest.builder()
                .prefix(prefix)
                .suffix(suffix)
                .build();
        
        ApiResponse<String> response = idGenerationService.generateUsername(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkUsername(@Valid @RequestBody UsernameCheckRequest request) {
        ApiResponse<Map<String, Object>> response = idGenerationService.checkUsername(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<SuggestedIdsResponse>> getSuggestedIds(
            @RequestParam(defaultValue = "1") int attempt,
            @RequestHeader String email,
            @RequestHeader String phone) {
        
        if (attempt < 1 || attempt > 3) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Attempt must be between 1 and 3"));
        }

        ApiResponse<SuggestedIdsResponse> response = idGenerationService.generateSuggestedIds(email, phone, attempt);
        return ResponseEntity.ok(response);
    }
}