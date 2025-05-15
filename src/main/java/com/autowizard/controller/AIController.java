package com.autowizard.controller;

import com.autowizard.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/query")
    public ResponseEntity<String> queryAI(@RequestBody String query) {
        String response = aiService.getAIResponse(query);
        return ResponseEntity.ok(response);
    }
} 