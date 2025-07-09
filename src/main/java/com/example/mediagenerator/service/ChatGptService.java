package com.example.mediagenerator.service;

import com.example.mediagenerator.dto.openai.ChatMessage;
import com.example.mediagenerator.dto.openai.ChatGPTRequest;
import com.example.mediagenerator.dto.openai.ChatGPTResponse;
import com.example.mediagenerator.model.MediaType; // Assurez-vous que c'est le bon import pour MediaType
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry; // Import pour Retry
import java.time.Duration; // Import pour Duration

@Service
@Slf4j
public class ChatGptService {

    private final WebClient openAIWebClient;

    @Value("${openai.api.key:SIMULATED_KEY_PLACEHOLDER}") // Valeur par défaut si non définie
    private String apiKey;

    @Value("${openai.model:gpt-3.5-turbo}") // Modèle par défaut
    private String chatModel;

    @Value("${openai.max_tokens:500}")
    private Integer maxTokens;

    @Value("${openai.temperature:0.7}")
    private Double temperature;

    @Autowired
    public ChatGptService(@Qualifier("openAIWebClient") WebClient openAIWebClient) {
        this.openAIWebClient = openAIWebClient;
    }

    public Mono<String> generateFormattedPrompt(String scenario, com.example.mediagenerator.model.MediaType mediaType) {
        if ("SIMULATED_KEY_PLACEHOLDER".equals(apiKey) || apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("OpenAI API key is not configured or is a placeholder. Returning simulated prompt.");
            String simulatedPromptContent = String.format(
                    "--- PROMPT SIMULÉ (Clé API OpenAI non configurée) ---\n" +
                    "Objectif Média: %s\n" +
                    "Scénario Initial:\n\"%s\"\n\n" +
                    "Instructions (simulées):\n" +
                    "1. Analyser le scénario.\n" +
                    "2. Proposer une structure de prompt détaillée pour générer un média de type '%s'.\n" +
                    "--- FIN DE LA SIMULATION DE PROMPT ---",
                    mediaType, scenario, mediaType
            );
            return Mono.delay(Duration.ofMillis(500)).thenReturn(simulatedPromptContent); // Simuler un petit délai
        }

        String systemMessageContent = "Tu es un assistant expert en création de prompts pour IA génératives d'images et de vidéos. Ton rôle est de transformer des scénarios bruts en prompts hautement efficaces et détaillés.";
        String userMessageContent = String.format(
                "Crée un prompt détaillé et optimisé pour une IA générative (images/vidéo) basé sur le scénario suivant. " +
                "Le média à produire est de type '%s'. " +
                "Le scénario est :\n\"%s\"\n\n" +
                "Le prompt doit inclure des suggestions claires et exploitables pour les éléments suivants :\n" +
                "- Personnages (apparence, expressions, actions clés).\n" +
                "- Composition visuelle et cadrage (ex: gros plan, plan d'ensemble, angle de vue).\n" +
                "- Ambiance et éclairage (ex: sombre et mystérieux, lumineux et joyeux, couleurs dominantes).\n" +
                "- Style artistique (ex: photoréaliste, dessin animé, peinture à l'huile, cyberpunk, fantasy épique).\n" +
                "- Éléments clés du décor et objets importants.\n" +
                "Assure-toi que le prompt soit structuré de manière à être facilement interprétable par une IA, en utilisant des mots-clés pertinents et en évitant les ambiguïtés.",
                mediaType.toString().toLowerCase(), scenario
        );

        ChatGPTRequest request = new ChatGPTRequest(chatModel, systemMessageContent, userMessageContent, maxTokens, temperature);
        log.info("Sending request to OpenAI API for scenario excerpt: {}", scenario.substring(0, Math.min(scenario.length(), 50)) + "...");

        return openAIWebClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatGPTResponse.class)
                .map(response -> {
                    if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                        log.info("Successfully received response from OpenAI API. Tokens used: {}", response.getUsage() != null ? response.getUsage().getTotalTokens() : "N/A");
                        return response.getChoices().get(0).getMessage().getContent();
                    }
                    log.warn("Received empty or malformed response from OpenAI API.");
                    return "Erreur: Réponse vide ou malformée de ChatGPT.";
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10))
                    .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests) // Retry sur 429
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                        log.error("Retry exhausted for OpenAI API call: {}", retrySignal.failure().getMessage());
                        return new RuntimeException("Trop de tentatives d'appel à l'API OpenAI échouées.", retrySignal.failure());
                    }))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("OpenAI API Error: Status {}, Body {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
                    return Mono.just("Erreur API ChatGPT: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Unexpected error during OpenAI API call", ex);
                    return Mono.just("Erreur inattendue lors de la communication avec ChatGPT: " + ex.getMessage());
                });
    }
}
