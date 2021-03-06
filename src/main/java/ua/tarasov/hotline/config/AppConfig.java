package ua.tarasov.hotline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import ua.tarasov.hotline.facade.HotLineFacadeImpl;
import ua.tarasov.hotline.models.RogOTGHotLineBot;

@Configuration
public class AppConfig {
    private final RogOTGHotLineBotConfig botConfig;

    public AppConfig(RogOTGHotLineBotConfig botConfig) {
        this.botConfig = botConfig;
    }

    @Bean
    public SetWebhook setWebhookInstance() {
        return SetWebhook.builder().url(botConfig.getWebHookPath()).build();
    }

    @Bean
    public RogOTGHotLineBot springWebhookBot(SetWebhook setWebhook, HotLineFacadeImpl hotLineFacadeImpl) {
        RogOTGHotLineBot bot = new RogOTGHotLineBot(hotLineFacadeImpl, setWebhook);
        bot.setBotToken(botConfig.getBotToken());
        bot.setBotUsername(botConfig.getUserName());
        bot.setBotPath(botConfig.getWebHookPath());
        return bot;
    }
}
