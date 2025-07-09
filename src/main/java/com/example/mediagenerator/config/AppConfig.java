package com.example.mediagenerator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Value("${openai.api.url:https://api.openai.com/v1}") // Default URL if not set in properties
    private String openApiBaseUrl;

    // La clé API sera injectée directement dans le service qui l'utilise,
    // car elle est spécifique à cet usage et ne doit pas être un header par défaut pour tous les appels WebClient.

    @Bean
    public WebClient openAIWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(openApiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // Autres configurations par défaut si nécessaire (timeouts, etc.)
                // Exemple de timeout:
                // .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(java.time.Duration.ofSeconds(30))))
                .build();
    }
}
