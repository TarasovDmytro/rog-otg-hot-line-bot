package ua.tarasov.hotline.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RogOTGHotLineBotConfig {
    @Value("${telegram.bot.username}")
    String userName;

    @Value("${telegram.bot.token}")
    String botToken;

    @Value("${telegram.bot.path}")
    String webHookPath;
}
