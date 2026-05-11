package com.digital.wallet.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    @Value("classpath:/prompts/category-prompt.st")
    private Resource systemMessage;

    private final ChatClient chatClient;

    private final Logger log = LoggerFactory.getLogger(AiService.class);

    public AiService(ChatClient chatClient){
        this.chatClient = chatClient;
    }

    public String autoCategorization(String transactionNote) {
        log.info("Transaction Note: {}", transactionNote);

        return chatClient
                .prompt()
                .system(systemMessage -> systemMessage.text(this.systemMessage).param("note", transactionNote))
                .call()
                .content();
    }

}
