package ua.tarasov.hotline.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.starter.SpringWebhookBot;
import ua.tarasov.hotline.facade.HotLineFacade;
import ua.tarasov.hotline.facade.HotLineFacadeImpl;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.ChatPropertyModeService;

import java.util.List;

@Slf4j
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RogOTGHotLineBot extends SpringWebhookBot {
    @Autowired
    final HotLineFacade hotLineFacade;
    String botPath;
    String botUsername;
    String botToken;
    @Qualifier("getChatProperties")
    @Autowired
    ChatPropertyModeService chatPropertyModeService;
    @Autowired
    BotUserService botUserService;

    public RogOTGHotLineBot(HotLineFacadeImpl hotLineFacade, DefaultBotOptions options, SetWebhook setWebhook) {
        super(options, setWebhook);
        this.hotLineFacade = hotLineFacade;
    }

    @Autowired
    public RogOTGHotLineBot(HotLineFacadeImpl hotLineFacade, SetWebhook setWebhook) {
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

    @Scheduled(fixedDelayString = "${notification.check.period}")
    public void sendNotification() {
        List<BotApiMethod<?>> methods = hotLineFacade.notificationUpdate();
        log.info(String.valueOf(methods));
        if (methods != null && !methods.isEmpty()) {
            methods.forEach(botApiMethod -> {
                try {
                    execute(botApiMethod);
                    Thread.sleep(35);
                } catch (TelegramApiException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
//            for (BotApiMethod<?> botApiMethod : methods) {
//                try {
//                    execute(botApiMethod);
//                    Thread.sleep(35);
//                } catch (TelegramApiException | InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
        }
    }
}
