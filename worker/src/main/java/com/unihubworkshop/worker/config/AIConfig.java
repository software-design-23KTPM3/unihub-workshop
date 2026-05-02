package com.unihubworkshop.worker.config;

import com.google.genai.Client;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Value("${spring.ai.googlegenai.api-key}")
    private String apiKey;

    @Bean
    public ChatModel chatModel() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API Key for Google Gen AI is empty");
        }
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model("gemini-2.0-flash")
                .temperature(0.)
                .build();

        Client client = Client.builder()
                .apiKey(apiKey)
                .build();

        return GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .defaultOptions(options)
                .build();
    }
}