package ua.tarasov.hotline.models.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.starter.SpringWebhookBot;
import ua.tarasov.hotline.facade.HotLineFacade;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.ChatPropertyModeService;
import ua.tarasov.hotline.service.NotificationParser;
import ua.tarasov.hotline.service.NotificationService;

import java.util.List;

@Slf4j
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RogOTGHotLineBot extends SpringWebhookBot {
    String botPath;
    String botUsername;
    String botToken;

    @Autowired
    final HotLineFacade hotLineFacade;
    @Autowired
    ChatPropertyModeService chatPropertyModeService;
    @Autowired
    BotUserService botUserService;
    @Autowired
    NotificationService notificationService;

    public RogOTGHotLineBot(HotLineFacade hotLineFacade, DefaultBotOptions options, SetWebhook setWebhook) {
        super(options, setWebhook);
        this.hotLineFacade = hotLineFacade;
    }

    @Autowired
    public RogOTGHotLineBot(HotLineFacade hotLineFacade, SetWebhook setWebhook) {
        super(setWebhook);
        this.hotLineFacade = hotLineFacade;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        log.info("getUpdate = {}", update);
        List<BotApiMethod<?>> methods = hotLineFacade.handleUpdate(update);
        log.info("getMethods = {}", methods);
        if (methods != null && !methods.isEmpty()) {
            if (methods.size() > 1) {
                methods.forEach(botApiMethod -> {
                    try {
                        if (botApiMethod != methods.get(methods.size() - 1)) {
                            execute(botApiMethod);
                            Thread.sleep(35);
                        }
                    } catch (TelegramApiException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
            return methods.get(methods.size() - 1);
        } else return SendMessage.builder()
                .chatId(update.getMessage().getChatId().toString())
                .text("Something wrong...")
                .build();
    }

    @Scheduled(fixedDelayString = "60000")
    public void sendNews() {
        NotificationParser parser = new NotificationParser(botUserService, notificationService);
        List<BotApiMethod<?>> methods = parser.getNews();
        log.info(String.valueOf(methods));
        if (methods != null && !methods.isEmpty()) {
            chatPropertyModeService.setBotState(1138897828, BotState.WAIT_MESSAGE_TO_ALL);
            for (BotApiMethod<?> botApiMethod : methods) {
                try {
                        execute(botApiMethod);
                        Thread.sleep(35);
                } catch (TelegramApiException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            chatPropertyModeService.setBotState(1138897828, BotState.WAIT_BUTTON);
        }
    }
}
