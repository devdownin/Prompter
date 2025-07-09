package com.example.mediagenerator.dto.openai;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
public class ChatGPTRequest {
    private String model;
    private List<ChatMessage> messages;
    private Integer max_tokens;
    private Double temperature; // Optionnel: 0.0 à 2.0, défaut 1.0
    // private Integer n; // Optionnel: combien de completions générer, défaut 1

    public ChatGPTRequest(String model, String systemMessageContent, String userMessageContent, Integer maxTokens, Double temperature) {
        this.model = model;
        this.messages = new ArrayList<>();
        if (systemMessageContent != null && !systemMessageContent.isEmpty()) {
            this.messages.add(new ChatMessage("system", systemMessageContent));
        }
        this.messages.add(new ChatMessage("user", userMessageContent));
        this.max_tokens = maxTokens;
        this.temperature = temperature;
    }
}
