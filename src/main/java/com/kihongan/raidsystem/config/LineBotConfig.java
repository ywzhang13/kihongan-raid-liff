package com.kihongan.raidsystem.config;

import com.linecorp.bot.client.LineMessagingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LINE Bot SDK Configuration
 */
@Configuration
public class LineBotConfig {
    
    @Value("${line.bot.channel-token:}")
    private String channelToken;
    
    @Bean
    public LineMessagingClient lineMessagingClient() {
        if (channelToken == null || channelToken.isEmpty()) {
            // Return a dummy client if token is not configured
            return LineMessagingClient.builder("dummy-token").build();
        }
        return LineMessagingClient.builder(channelToken).build();
    }
}
