package com.unihubworkshop.worker.AISummary;

import org.springframework.stereotype.Service;
import org.springframework.ai.chat.model.ChatModel;

@Service
public class AISummaryService {

    private final ChatModel chatModel;

    public AISummaryService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String generateResponse(String cleanedText) {
        String prompt = "Please provide a concise and professional summary of the following workshop introduction for our detail page. Focus on the main topics, target audience, and key takeaways:\n\n"
                + cleanedText;
        return this.chatModel.call(prompt);
    }
}