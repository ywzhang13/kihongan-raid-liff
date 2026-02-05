package com.kihongan.raidsystem.controller;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

/**
 * LINE Webhook Controller
 * Handles incoming LINE events and logs group IDs
 */
@LineMessageHandler
public class LineWebhookController {
    
    private static final Logger log = LoggerFactory.getLogger(LineWebhookController.class);
    
    @Autowired
    private LineMessagingClient lineMessagingClient;
    
    /**
     * Handle text messages
     */
    @EventMapping
    public void handleTextMessage(MessageEvent<TextMessageContent> event) {
        String messageText = event.getMessage().getText();
        
        // Log the event source
        if (event.getSource() instanceof GroupSource) {
            GroupSource groupSource = (GroupSource) event.getSource();
            String groupId = groupSource.getGroupId();
            
            log.info("========================================");
            log.info("📱 收到群組訊息");
            log.info("群組 ID: {}", groupId);
            log.info("訊息內容: {}", messageText);
            log.info("========================================");
            
            // If message is "!groupid", reply with the group ID
            if ("!groupid".equalsIgnoreCase(messageText.trim())) {
                try {
                    lineMessagingClient.replyMessage(
                        com.linecorp.bot.model.ReplyMessage.builder()
                            .replyToken(event.getReplyToken())
                            .messages(Arrays.asList(
                                com.linecorp.bot.model.message.TextMessage.builder()
                                    .text("群組 ID: " + groupId)
                                    .build()
                            ))
                            .build()
                    ).get();
                } catch (Exception e) {
                    log.error("Failed to reply message", e);
                }
            }
        }
    }
    
    /**
     * Handle other events
     */
    @EventMapping
    public void handleDefaultEvent(Event event) {
        log.info("收到 LINE 事件: {}", event);
    }
}
