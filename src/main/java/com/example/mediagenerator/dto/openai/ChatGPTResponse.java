package com.example.mediagenerator.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ChatGPTResponse {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<ChatGPTChoice> choices;
    private ChatGPTUsage usage;
    @JsonProperty("system_fingerprint")
    private String systemFingerprint; // Peut être nul
}
