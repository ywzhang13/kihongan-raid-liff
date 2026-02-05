package com.kihongan.raidsystem.service;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.component.*;
import com.linecorp.bot.model.message.flex.unit.FlexAlign;
import com.linecorp.bot.model.message.flex.unit.FlexFontSize;
import com.linecorp.bot.model.message.flex.unit.FlexLayout;
import com.linecorp.bot.model.message.flex.unit.FlexMarginSize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LineMessagingService {
    
    private final LineMessagingClient lineMessagingClient;
    private final String groupId;
    
    public LineMessagingService(
            LineMessagingClient lineMessagingClient,
            @Value("${line.webhook.group-id:}") String groupId) {
        this.lineMessagingClient = lineMessagingClient;
        this.groupId = groupId;
    }
    
    /**
     * 發送遠征隊建立通知
     */
    public void sendRaidCreatedNotification(String raidTitle, String creatorName, LocalDateTime startTime, String subtitle) {
        if (groupId == null || groupId.isEmpty()) {
            return; // 未設定群組 ID，跳過通知
        }
        
        FlexMessage flexMessage = FlexMessage.builder()
                .altText("🎯 新遠征隊：" + raidTitle)
                .contents(createRaidCreatedBubble(raidTitle, creatorName, startTime, subtitle))
                .build();
        
        PushMessage pushMessage = new PushMessage(groupId, flexMessage);
        
        try {
            lineMessagingClient.pushMessage(pushMessage).get();
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to send LINE notification: " + e.getMessage());
        }
    }
    
    /**
     * 發送遠征隊建立並加入通知（合併版）
     */
    public void sendRaidCreatedWithSignupNotification(String raidTitle, String creatorName, LocalDateTime startTime, String subtitle, String characterName, String job, Integer level) {
        if (groupId == null || groupId.isEmpty()) {
            return;
        }
        
        FlexMessage flexMessage = FlexMessage.builder()
                .altText("🎯 " + creatorName + " 建立遠征隊：" + raidTitle)
                .contents(createRaidCreatedWithSignupBubble(raidTitle, creatorName, startTime, subtitle, characterName, job, level))
                .build();
        
        PushMessage pushMessage = new PushMessage(groupId, flexMessage);
        
        try {
            lineMessagingClient.pushMessage(pushMessage).get();
        } catch (Exception e) {
            System.err.println("Failed to send LINE notification: " + e.getMessage());
        }
    }
    
    /**
     * 發送報名成功通知
     */
    public void sendSignupNotification(String raidTitle, String userName, String characterName, String job, Integer level, int currentCount, int maxCount, String creatorName) {
        if (groupId == null || groupId.isEmpty()) {
            return;
        }
        
        FlexMessage flexMessage = FlexMessage.builder()
                .altText("✅ " + userName + " 加入 " + creatorName + " 的遠征隊：" + raidTitle)
                .contents(createSignupBubble(raidTitle, userName, characterName, job, level, currentCount, maxCount, creatorName))
                .build();
        
        PushMessage pushMessage = new PushMessage(groupId, flexMessage);
        
        try {
            lineMessagingClient.pushMessage(pushMessage).get();
        } catch (Exception e) {
            System.err.println("Failed to send LINE notification: " + e.getMessage());
        }
    }
    
    /**
     * 發送取消報名通知
     */
    public void sendCancelSignupNotification(String raidTitle, String userName, String characterName, int currentCount, int maxCount, String creatorName) {
        if (groupId == null || groupId.isEmpty()) {
            return;
        }
        
        FlexMessage flexMessage = FlexMessage.builder()
                .altText("❌ " + userName + " 取消 " + creatorName + " 的遠征隊：" + raidTitle)
                .contents(createCancelSignupBubble(raidTitle, userName, characterName, currentCount, maxCount, creatorName))
                .build();
        
        PushMessage pushMessage = new PushMessage(groupId, flexMessage);
        
        try {
            lineMessagingClient.pushMessage(pushMessage).get();
        } catch (Exception e) {
            System.err.println("Failed to send LINE notification: " + e.getMessage());
        }
    }
    
    /**
     * 建立遠征隊通知的 Flex Message Bubble
     */
    private Bubble createRaidCreatedBubble(String raidTitle, String creatorName, LocalDateTime startTime, String subtitle) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm");
        String timeStr = startTime.format(formatter);
        
        List<FlexComponent> bodyContents = Arrays.asList(
                createInfoRow("👤 建立人", creatorName),
                createInfoRow("⏰ 時間", timeStr),
                subtitle != null && !subtitle.isEmpty() ? createInfoRow("📝 備註", subtitle) : null
        ).stream().filter(c -> c != null).collect(Collectors.toList());
        
        return Bubble.builder()
                .header(Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(
                                Text.builder()
                                        .text("🎯 新遠征隊")
                                        .weight(Text.TextWeight.BOLD)
                                        .size(FlexFontSize.LG)
                                        .color("#FFFFFF")
                                        .build()
                        ))
                        .backgroundColor("#667eea")
                        .paddingAll("13px")
                        .build())
                .body(Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(
                                Text.builder()
                                        .text(raidTitle)
                                        .weight(Text.TextWeight.BOLD)
                                        .size(FlexFontSize.XL)
                                        .margin(FlexMarginSize.MD)
                                        .build(),
                                Box.builder()
                                        .layout(FlexLayout.VERTICAL)
                                        .margin(FlexMarginSize.LG)
                                        .spacing(FlexMarginSize.SM)
                                        .contents(bodyContents)
                                        .build()
                        ))
                        .build())
                .footer(Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(
                                Text.builder()
                                        .text("點擊 LIFF 連結報名參加！")
                                        .size(FlexFontSize.SM)
                                        .color("#999999")
                                        .align(FlexAlign.CENTER)
                                        .build()
                        ))
                        .build())
                .build();
    }
    
    /**
     * 建立遠征隊並加入通知的 Flex Message Bubble（合併版）
     */
    private Bubble createRaidCreatedWithSignupBubble(String raidTitle, String creatorName, LocalDateTime startTime, String subtitle, String characterName, String job, Integer level) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm");
        String timeStr = startTime.format(formatter);
        
        String jobLevel = job != null ? job : "未設定";
        if (level != null) {
            jobLevel += " Lv." + level;
        }
        
        List<FlexComponent> bodyContents = Arrays.asList(
                createInfoRow("👤 建立人", creatorName),
                createInfoRow("⏰ 時間", timeStr),
                subtitle != null && !subtitle.isEmpty() ? createInfoRow("📝 備註", subtitle) : null,
                // 分隔線
                Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(
                                Separator.builder().margin(FlexMarginSize.MD).build()
                        ))
                        .margin(FlexMarginSize.MD)
                        .build(),
                // 加入資訊
                createInfoRow("⚔️ 角色", characterName),
                createInfoRow("💼 職業", jobLevel),
                createInfoRow("👥 人數", "1/6 人")
        ).stream().filter(c -> c != null).collect(Collectors.toList());
        
        return Bubble.builder()
                .header(Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(
                                Text.builder()
                                        .text("🎯 新遠征隊（已加入）")
                                        .weight(Text.TextWeight.BOLD)
                                        .size(FlexFontSize.LG)
                                        .color("#FFFFFF")
                                        .build()
                        ))
                        .backgroundColor("#667eea")
                        .paddingAll("13px")
                        .build())
                .body(Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(
                                Text.builder()
                                        .text(raidTitle)
                                        .weight(Text.TextWeight.BOLD)
                                        .size(FlexFontSize.XL)
                                        .margin(FlexMarginSize.MD)
                                        .build(),
                                Box.builder()
                                        .layout(FlexLayout.VERTICAL)
                                        .margin(FlexMarginSize.LG)
                                        .spacing(FlexMarginSize.SM)
                                        .contents(bodyContents)
                                        .build()
                        ))
                        .build())
                .footer(Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(
                                Text.builder()
                                        .text("點擊 LIFF 連結報名參加！")
                                        .size(FlexFontSize.SM)
                                        .color("#999999")
                                        .align(FlexAlign.CENTER)
                                        .build()
                        ))
                        .build())
                .build();
    }
    
    /**
     * 建立報名通知的 Flex Message Bubble
     */
    private Bubble createSignupBubble(String raidTitle, String userName, String characterName, String job, Integer level, int currentCount, int maxCount, String creatorName) {
        String jobLevel = job != null ? job : "未設定";
        if (level != null) {
            jobLevel += " Lv." + level;
        }
        
        boolean isFull = currentCount >= maxCount;
        String statusColor = isFull ? "#e74c3c" : "#27ae60";
        String statusText = isFull ? "已滿員" : currentCount + "/" + maxCount + " 人";
        
        return Bubble.builder()
                .header(Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(
                                Text.builder()
                                        .text("✅ 報名成功")
                                        .weight(Text.TextWeight.BOLD)
                                        .size(FlexFontSize.LG)
                                        .color("#FFFFFF")
                                        .build()
                        ))
                        .backgroundColor("#27ae60")
                        .paddingAll("13px")
                        .build())
                .body(Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(
                                Text.builder()
                                        .text(raidTitle)
                                        .weight(Text.TextWeight.BOLD)
                                        .size(FlexFontSize.XL)
                                        .margin(FlexMarginSize.MD)
                                        .build(),
                                Box.builder()
                                        .layout(FlexLayout.VERTICAL)
                                        .margin(FlexMarginSize.LG)
                                        .spacing(FlexMarginSize.SM)
                                        .contents(Arrays.asList(
                                                createInfoRow("🎯 隊長", creatorName),
                                                createInfoRow("👤 玩家", userName),
                                                createInfoRow("⚔️ 角色", characterName),
                                                createInfoRow("💼 職業", jobLevel),
                                                Box.builder()
                                                        .layout(FlexLayout.HORIZONTAL)
                                                        .contents(Arrays.asList(
                                                                Text.builder()
                                                                        .text("👥 人數")
                                                                        .size(FlexFontSize.SM)
                                                                        .color("#555555")
                                                                        .flex(0)
                                                                        .build(),
                                                                Text.builder()
                                                                        .text(statusText)
                                                                        .size(FlexFontSize.SM)
                                                                        .color(statusColor)
                                                                        .weight(Text.TextWeight.BOLD)
                                                                        .align(FlexAlign.END)
                                                                        .build()
                                                        ))
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();
    }
    
    /**
     * 建立取消報名通知的 Flex Message Bubble
     */
    private Bubble createCancelSignupBubble(String raidTitle, String userName, String characterName, int currentCount, int maxCount, String creatorName) {
        String statusColor = "#999999";
        String statusText = currentCount + "/" + maxCount + " 人";
        
        return Bubble.builder()
                .header(Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(
                                Text.builder()
                                        .text("❌ 取消報名")
                                        .weight(Text.TextWeight.BOLD)
                                        .size(FlexFontSize.LG)
                                        .color("#FFFFFF")
                                        .build()
                        ))
                        .backgroundColor("#e74c3c")
                        .paddingAll("13px")
                        .build())
                .body(Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(Arrays.asList(
                                Text.builder()
                                        .text(raidTitle)
                                        .weight(Text.TextWeight.BOLD)
                                        .size(FlexFontSize.XL)
                                        .margin(FlexMarginSize.MD)
                                        .build(),
                                Box.builder()
                                        .layout(FlexLayout.VERTICAL)
                                        .margin(FlexMarginSize.LG)
                                        .spacing(FlexMarginSize.SM)
                                        .contents(Arrays.asList(
                                                createInfoRow("🎯 隊長", creatorName),
                                                createInfoRow("👤 玩家", userName),
                                                createInfoRow("⚔️ 角色", characterName),
                                                Box.builder()
                                                        .layout(FlexLayout.HORIZONTAL)
                                                        .contents(Arrays.asList(
                                                                Text.builder()
                                                                        .text("👥 人數")
                                                                        .size(FlexFontSize.SM)
                                                                        .color("#555555")
                                                                        .flex(0)
                                                                        .build(),
                                                                Text.builder()
                                                                        .text(statusText)
                                                                        .size(FlexFontSize.SM)
                                                                        .color(statusColor)
                                                                        .weight(Text.TextWeight.BOLD)
                                                                        .align(FlexAlign.END)
                                                                        .build()
                                                        ))
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();
    }
    
    /**
     * 建立資訊列
     */
    private FlexComponent createInfoRow(String label, String value) {
        return Box.builder()
                .layout(FlexLayout.HORIZONTAL)
                .contents(Arrays.asList(
                        Text.builder()
                                .text(label)
                                .size(FlexFontSize.SM)
                                .color("#555555")
                                .flex(0)
                                .build(),
                        Text.builder()
                                .text(value)
                                .size(FlexFontSize.SM)
                                .color("#111111")
                                .align(FlexAlign.END)
                                .build()
                ))
                .build();
    }
}
