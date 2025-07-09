package com.example.mediagenerator.service;

import com.google.genai.Client; // From com.google.genai:google-genai
import com.google.genai.Models; // From com.google.genai:google-genai
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import com.google.genai.types.Candidate;
import com.google.genai.types.FinishReason;

import com.example.mediagenerator.model.MediaType;
// import lombok.extern.slf4j.Slf4j; // Removing Lombok
import org.slf4j.Logger; // Manual SLF4J
import org.slf4j.LoggerFactory; // Manual SLF4J
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.Collections;
// import java.util.List; // Not needed if using response.text()
// import java.util.Optional; // Not needed if using response.text()

@Service
// @Slf4j // Removing Lombok
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class); // Manual logger

    private final Models modelsClient; // Public field in com.google.genai.Client
    private final String modelName;

    @Value("${gemini.max_output_tokens:8192}")
    private Integer maxOutputTokens;

    @Value("${gemini.temperature:0.7f}")
    private Float temperature;

    public GeminiService(@Value("${gemini.api.key}") String apiKey,
                         @Value("${gemini.model:gemini-1.5-flash-latest}") String modelName) {
        this.modelName = modelName;
        Client client = Client.builder().apiKey(apiKey).build();
        this.modelsClient = client.models; // Accessing as a public field per GitHub README
    }

    public Mono<String> generateFormattedPrompt(String scenario, MediaType mediaType) {
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

        // Using Content.fromParts and Part.fromText as per GitHub README examples
        Part part = Part.fromText(userMessageContent);
        Content content = Content.fromParts(part); // This creates Content with role "user" by default for a single part

        GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder();
        if (maxOutputTokens != null) {
            configBuilder.maxOutputTokens(maxOutputTokens);
        }
        if (temperature != null) {
            configBuilder.temperature(temperature);
        }
        // configBuilder.candidateCount(1); // Optional

        GenerateContentConfig generateContentConfig = configBuilder.build();

        log.info("Sending request to Gemini API model {} for scenario excerpt: {}", modelName, scenario.substring(0, Math.min(scenario.length(), 50)) + "...");

        return Mono.fromCallable(() -> {
            try {
                // Models.generateContent takes List<Content> or a single Content object.
                // The example shows passing a single string for simple text, or a Content object for multimodal.
                // Let's use the Content object directly (it might also accept List<Content>).
                // The Javadoc for Models.generateContent shows overloads for String, Content, and List<Content>
                GenerateContentResponse response = modelsClient.generateContent(
                    this.modelName,
                    content, // Passing single Content object
                    generateContentConfig
                );

                // Check for safety ratings first, as response.text() might be null if blocked.
                if (response.promptFeedback() != null && response.promptFeedback().isPresent() &&
                    response.promptFeedback().get().blockReason().isPresent()) {
                    log.warn("Gemini request blocked due to prompt feedback: {}", response.promptFeedback().get().blockReason().get());
                    return "Erreur: Prompt bloqué par les filtres de sécurité Gemini.";
                }

                if (response.candidates() != null && response.candidates().isPresent() && !response.candidates().get().isEmpty()) {
                    Candidate firstCandidate = response.candidates().get().get(0);
                    if (firstCandidate.finishReason().isPresent() && firstCandidate.finishReason().get().equals(FinishReason.Known.SAFETY)) {
                        log.warn("Gemini response candidate blocked due to safety reasons for model {}.", modelName);
                        return "Erreur: Contenu du candidat bloqué par les filtres de sécurité Gemini.";
                    }
                }

                String responseText = response.text(); // Helper method to get text from first candidate

                if (responseText != null && !responseText.isEmpty()) {
                    log.info("Successfully received response from Gemini API model {}.", modelName);
                    return responseText;
                }

                log.warn("Received empty or malformed response (or no text) from Gemini API model {}.", modelName);
                return "Erreur: Réponse vide ou malformée de Gemini.";
            // IOException is a subclass of Exception, so the general catch will handle it.
            } catch (Exception e) {
                log.error("Error calling Gemini API model {}", modelName, e);
                // We can check instanceof IOException if specific logging/handling is needed
                // For now, general message covers it.
                return "Erreur lors de la communication avec Gemini: " + e.getMessage();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
