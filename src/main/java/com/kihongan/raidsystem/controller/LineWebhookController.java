package com.kihongan.raidsystem.controller;

import com.kihongan.raidsystem.domain.raid.Raid;
import com.kihongan.raidsystem.domain.raid.RaidService;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LINE Webhook Controller
 * Handles incoming LINE events and provides command-based interactions
 */
@LineMessageHandler
public class LineWebhookController {
    
    private static final Logger log = LoggerFactory.getLogger(LineWebhookController.class);
    
    @Autowired
    private LineMessagingClient lineMessagingClient;
    
    @Autowired
    private RaidService raidService;
    
    /**
     * Handle text messages
     */
    @EventMapping
    public void handleTextMessage(MessageEvent<TextMessageContent> event) {
        String messageText = event.getMessage().getText().trim();
        
        // Only handle messages from groups
        if (!(event.getSource() instanceof GroupSource)) {
            return;
        }
        
        GroupSource groupSource = (GroupSource) event.getSource();
        String groupId = groupSource.getGroupId();
        String replyToken = event.getReplyToken();
        
        log.info("收到群組訊息 - 群組ID: {}, 內容: {}", groupId, messageText);
        
        try {
            // Handle different commands
            if ("!groupid".equalsIgnoreCase(messageText)) {
                handleGroupIdCommand(replyToken, groupId);
            } else if ("!raids".equalsIgnoreCase(messageText) || "!遠征".equalsIgnoreCase(messageText)) {
                handleRaidsCommand(replyToken);
            } else if ("!help".equalsIgnoreCase(messageText) || "!指令".equalsIgnoreCase(messageText)) {
                handleHelpCommand(replyToken);
            }
        } catch (Exception e) {
            log.error("處理指令失敗: {}", messageText, e);
        }
    }
    
    /**
     * Handle !groupid command
     */
    private void handleGroupIdCommand(String replyToken, String groupId) {
        try {
            lineMessagingClient.replyMessage(
                new ReplyMessage(
                    replyToken,
                    Arrays.asList(
                        TextMessage.builder()
                            .text("📋 群組 ID:\n" + groupId)
                            .build()
                    )
                )
            ).get();
        } catch (Exception e) {
            log.error("回覆群組ID失敗", e);
        }
    }
    
    /**
     * Handle !raids command - show current week's raids
     */
    private void handleRaidsCommand(String replyToken) {
        try {
            List<Raid> raids = raidService.listRaids();
            
            if (raids.isEmpty()) {
                lineMessagingClient.replyMessage(
                    new ReplyMessage(
                        replyToken,
                        Arrays.asList(
                            TextMessage.builder()
                                .text("📅 本周還沒有遠征隊\n\n請到 LIFF 建立遠征隊！")
                                .build()
                        )
                    )
                ).get();
                return;
            }
            
            // Format raid list
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm");
            StringBuilder message = new StringBuilder("📅 本周遠征列表\n");
            message.append("━━━━━━━━━━━━━━\n\n");
            
            for (int i = 0; i < raids.size(); i++) {
                Raid raid = raids.get(i);
                LocalDateTime startTime = LocalDateTime.ofInstant(raid.getStartTime(), ZoneId.of("Asia/Taipei"));
                
                message.append(String.format("%d. %s\n", i + 1, raid.getTitle()));
                message.append(String.format("   ⏰ %s\n", startTime.format(formatter)));
                if (raid.getSubtitle() != null && !raid.getSubtitle().isEmpty()) {
                    message.append(String.format("   📝 %s\n", raid.getSubtitle()));
                }
                message.append("\n");
            }
            
            message.append("━━━━━━━━━━━━━━\n");
            message.append("💡 到 LIFF 查看詳情和報名");
            
            lineMessagingClient.replyMessage(
                new ReplyMessage(
                    replyToken,
                    Arrays.asList(
                        TextMessage.builder()
                            .text(message.toString())
                            .build()
                    )
                )
            ).get();
            
        } catch (Exception e) {
            log.error("回覆遠征列表失敗", e);
        }
    }
    
    /**
     * Handle !help command - show available commands
     */
    private void handleHelpCommand(String replyToken) {
        try {
            String helpMessage = "🤖 KiHongan 遠征隊 Bot\n" +
                    "━━━━━━━━━━━━━━\n\n" +
                    "📋 可用指令：\n\n" +
                    "!raids 或 !遠征\n" +
                    "  → 查看本周遠征列表\n\n" +
                    "!groupid\n" +
                    "  → 顯示群組 ID\n\n" +
                    "!help 或 !指令\n" +
                    "  → 顯示此說明\n\n" +
                    "━━━━━━━━━━━━━━\n" +
                    "💡 使用 LIFF 建立遠征和報名";
            
            lineMessagingClient.replyMessage(
                new ReplyMessage(
                    replyToken,
                    Arrays.asList(
                        TextMessage.builder()
                            .text(helpMessage)
                            .build()
                    )
                )
            ).get();
        } catch (Exception e) {
            log.error("回覆說明訊息失敗", e);
        }
    }
    
    /**
     * Handle other events
     */
    @EventMapping
    public void handleDefaultEvent(Event event) {
        log.debug("收到 LINE 事件: {}", event.getClass().getSimpleName());
    }
}
