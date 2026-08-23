package com.mindq.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai.groq")
public class GroqProperties {

    private String apiKey;
    private String baseUrl = "https://api.groq.com/openai/v1";
    private String defaultModel = "llama-3.3-70b-versatile";
    private int maxTokens = 4096;
    private double temperature = 0.7;
}
