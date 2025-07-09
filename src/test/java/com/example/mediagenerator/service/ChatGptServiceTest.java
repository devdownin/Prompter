package com.example.mediagenerator.service;

import com.example.mediagenerator.config.AppConfig;
import com.example.mediagenerator.dto.openai.ChatGPTRequest;
import com.example.mediagenerator.dto.openai.ChatGPTResponse;
import com.example.mediagenerator.dto.openai.ChatMessage;
import com.example.mediagenerator.dto.openai.ChatGPTChoice;
import com.example.mediagenerator.model.MediaType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Using SpringBootTest to easily get WebClient.Builder and other context if needed
// but could be done with @ExtendWith(MockitoExtension.class) and manual WebClient setup.
@SpringBootTest(classes = {AppConfig.class, ChatGptService.class}) // Load AppConfig to get WebClient.Builder and the service itself
@ActiveProfiles("test") // Assuming you might have a test application.properties
class ChatGptServiceTest {

    public static MockWebServer mockBackEnd;

    @Autowired
    private WebClient.Builder webClientBuilder; // Autowire builder from AppConfig context

    private ChatGptService chatGptService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUpServer() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @AfterAll
    static void tearDownServer() throws IOException {
        mockBackEnd.shutdown();
    }

    @BeforeEach
    void initialize() {
        String baseUrl = String.format("http://localhost:%s", mockBackEnd.getPort());
        WebClient testWebClient = webClientBuilder.baseUrl(baseUrl).build();

        // Create a new instance of ChatGptService for each test, injecting the mock server based WebClient
        chatGptService = new ChatGptService(testWebClient);
        // Manually set other @Value fields or ensure they have test-friendly defaults
        ReflectionTestUtils.setField(chatGptService, "apiKey", "test-api-key"); // Use a real-looking key for tests
        ReflectionTestUtils.setField(chatGptService, "chatModel", "gpt-test-model");
        ReflectionTestUtils.setField(chatGptService, "maxTokens", 150);
        ReflectionTestUtils.setField(chatGptService, "temperature", 0.5);
    }

    @Test
    void generateFormattedPrompt_whenApiCallSuccessful_returnsPromptContent() throws JsonProcessingException {
        // Arrange
        ChatGPTResponse mockApiResponse = new ChatGPTResponse();
        ChatGPTChoice choice = new ChatGPTChoice();
        ChatMessage assistantMessage = new ChatMessage("assistant", "Generated prompt content here.");
        choice.setMessage(assistantMessage);
        mockApiResponse.setChoices(List.of(choice));

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(mockApiResponse))
                .addHeader("Content-Type", "application/json"));

        // Act
        Mono<String> resultMono = chatGptService.generateFormattedPrompt("Test scenario", MediaType.VIDEO);

        // Assert
        StepVerifier.create(resultMono)
                .expectNext("Generated prompt content here.")
                .verifyComplete();
    }

    @Test
    void generateFormattedPrompt_whenApiKeyIsPlaceholder_returnsSimulatedPrompt() {
        // Arrange
        ReflectionTestUtils.setField(chatGptService, "apiKey", "SIMULATED_KEY_PLACEHOLDER");

        // Act
        Mono<String> resultMono = chatGptService.generateFormattedPrompt("Test scenario for simulation", MediaType.IMAGES);

        // Assert
        StepVerifier.create(resultMono)
                .consumeNextWith(response -> {
                    assertTrue(response.contains("--- PROMPT SIMULÉ"));
                    assertTrue(response.contains("Test scenario for simulation"));
                })
                .verifyComplete();
    }

    @Test
    void generateFormattedPrompt_whenApiReturnsError_returnsErrorMessage() {
        // Arrange
        mockBackEnd.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\": {\"message\": \"Internal Server Error\"}}")
        .addHeader("Content-Type", "application/json"));

        // Act
        Mono<String> resultMono = chatGptService.generateFormattedPrompt("Scenario leading to error", MediaType.COMIC);

        // Assert
        StepVerifier.create(resultMono)
                .consumeNextWith(response -> {
                    assertTrue(response.startsWith("Erreur API ChatGPT: 500 INTERNAL_SERVER_ERROR"));
                    assertTrue(response.contains("Internal Server Error"));
                })
                .verifyComplete();
    }

    @Test
    void generateFormattedPrompt_whenApiReturnsEmptyChoices_returnsErrorMessage() throws JsonProcessingException {
        // Arrange
        ChatGPTResponse mockApiResponse = new ChatGPTResponse();
        mockApiResponse.setChoices(List.of()); // Empty choices

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(mockApiResponse))
                .addHeader("Content-Type", "application/json"));

        // Act
        Mono<String> resultMono = chatGptService.generateFormattedPrompt("Scenario for empty choices", MediaType.VIDEO);

        // Assert
        StepVerifier.create(resultMono)
                .expectNext("Erreur: Réponse vide ou malformée de ChatGPT.")
                .verifyComplete();
    }
}
