package com.autowizard.ai.internal;

import org.springframework.stereotype.Service;

@Service
public class AIService {
    
    public String getAIResponse(String query) {
        // Mocked response for now
        return "This is a mocked AI response for the query: " + query;
    }
} 