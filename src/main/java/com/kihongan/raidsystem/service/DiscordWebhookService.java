package com.kihongan.raidsystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DiscordWebhookService {

    private final String webhookUrl;
    private final String proxyUrl;
    private final String proxySecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DiscordWebhookService(
            @Value("${discord.webhook.url:}") String webhookUrl,
            @Value("${discord.proxy.url:}") String proxyUrl,
            @Value("${discord.proxy.secret:kihongan-raid-2026}") String proxySecret) {
        this.webhookUrl = webhookUrl;
        this.proxyUrl = proxyUrl;
        this.proxySecret = proxySecret;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    private boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }

    public void sendRaidCreatedNotification(String raidTitle, String creatorName,
            LocalDateTime startTime, String subtitle, String characterName,
            String job, Integer level) {
        if (!isEnabled()) return;

        String timeStr = startTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
        String[] weekDays = {"日", "一", "二", "三", "四", "五", "六"};
        String weekDay = "週" + weekDays[startTime.getDayOfWeek().getValue() % 7];

        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "⚔️ 新遠征隊：" + raidTitle);
        embed.put("color", 6717674);

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(makeField("👤 建立人", creatorName, true));
        fields.add(makeField("⏰ 時間", timeStr + " " + weekDay, true));
        if (subtitle != null && !subtitle.isEmpty()) {
            fields.add(makeField("📝 備註", subtitle, false));
        }
        if (characterName != null) {
            String jobLevel = (job != null ? job : "未設定") + (level != null ? " Lv." + level : "");
            fields.add(makeField("⚔️ 參加角色", characterName + " (" + jobLevel + ")", false));
            fields.add(makeField("👥 人數", "1/6 人", true));
        }
        embed.put("fields", fields);
        embed.put("footer", Map.of("text", "KiHongan 遠征報名系統"));
        sendEmbed(embed);
    }

    public void sendSignupNotification(String raidTitle, String userName,
            String characterName, String job, Integer level,
            int currentCount, int maxCount, String creatorName) {
        if (!isEnabled()) return;
        String jobLevel = (job != null ? job : "未設定") + (level != null ? " Lv." + level : "");
        boolean isFull = currentCount >= maxCount;
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "✅ 報名成功：" + raidTitle);
        embed.put("color", isFull ? 15277667 : 2600544);
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(makeField("🎯 隊長", creatorName, true));
        fields.add(makeField("👤 玩家", userName, true));
        fields.add(makeField("⚔️ 角色", characterName + " (" + jobLevel + ")", false));
        fields.add(makeField("👥 人數", currentCount + "/" + maxCount + " 人" + (isFull ? " 🔴 已滿員" : ""), true));
        embed.put("fields", fields);
        embed.put("footer", Map.of("text", "KiHongan 遠征報名系統"));
        sendEmbed(embed);
    }

    public void sendCancelSignupNotification(String raidTitle, String userName,
            String characterName, int currentCount, int maxCount, String creatorName) {
        if (!isEnabled()) return;
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "❌ 取消報名：" + raidTitle);
        embed.put("color", 15158332);
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(makeField("🎯 隊長", creatorName, true));
        fields.add(makeField("👤 玩家", userName, true));
        fields.add(makeField("⚔️ 角色", characterName, false));
        fields.add(makeField("👥 人數", currentCount + "/" + maxCount + " 人", true));
        embed.put("fields", fields);
        embed.put("footer", Map.of("text", "KiHongan 遠征報名系統"));
        sendEmbed(embed);
    }

    private Map<String, Object> makeField(String name, String value, boolean inline) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("value", value);
        field.put("inline", inline);
        return field;
    }

    private void sendEmbed(Map<String, Object> embed) {
        // Run in a separate thread to not block the main request
        new Thread(() -> sendEmbedWithRetry(embed, 0)).start();
    }

    private boolean useProxy() {
        return proxyUrl != null && !proxyUrl.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void sendEmbedWithRetry(Map<String, Object> embed, int attempt) {
        if (attempt >= 5) {
            System.err.println("Discord webhook failed after 5 attempts, giving up.");
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("embeds", List.of(embed));
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder requestBuilder;
            if (useProxy()) {
                // 透過 Cloudflare Worker 代理發送
                requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(proxyUrl))
                        .header("Content-Type", "application/json")
                        .header("X-Proxy-Secret", proxySecret)
                        .header("X-Target-Url", webhookUrl);
                System.out.println("Sending Discord notification via proxy: " + proxyUrl);
            } else {
                // 直接發送到 Discord
                requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json");
            }

            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                long retryMs = 10000;
                try {
                    Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
                    Object retryAfter = body.get("retry_after");
                    if (retryAfter instanceof Number) {
                        retryMs = (long)(((Number) retryAfter).doubleValue() * 1000) + 500;
                    }
                } catch (Exception ignored) {}

                System.err.println("Discord rate limited, retrying in " + retryMs + "ms (attempt " + (attempt + 1) + "/5)");
                Thread.sleep(retryMs);
                sendEmbedWithRetry(embed, attempt + 1);
            } else if (response.statusCode() >= 400) {
                System.err.println("Discord webhook failed: " + response.statusCode() + " " + response.body());
            } else {
                System.out.println("Discord notification sent successfully (status: " + response.statusCode() + ")");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Failed to send Discord notification: " + e.getMessage());
        }
    }
}
