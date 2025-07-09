package com.example.mediagenerator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    // The WebClient for OpenAI is no longer needed as Gemini uses its own SDK client.
    // If other WebClient instances are needed in the future, they can be defined here.
}
