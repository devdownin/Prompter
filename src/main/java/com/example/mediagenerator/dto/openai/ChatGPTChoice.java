package com.example.mediagenerator.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatGPTChoice {
    private int index;
    private ChatMessage message; // Contient le message de l'assistant
    @JsonProperty("finish_reason")
    private String finishReason;
    // Si vous utilisez le streaming (delta), la structure ici serait différente
    // private ChatMessage delta;
}
