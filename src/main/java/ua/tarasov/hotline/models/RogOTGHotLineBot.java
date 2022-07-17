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
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.starter.SpringWebhookBot;
import ua.tarasov.hotline.entities.BotUser;
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
    String botPath;
    String botUsername;
    String botToken;

    @Autowired
    final HotLineFacade hotLineFacade;
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
        List<PartialBotApiMethod<?>> methods = hotLineFacade.handleUpdate(update);
        log.info("getMethods = {}", methods);
        if (methods != null && !methods.isEmpty()) {
            if (methods.size() > 1) {
                methods.forEach(botApiMethod -> {
                    try {
                        if (botApiMethod != methods.get(methods.size() - 1)) {
                            execute((SendVideo) botApiMethod);
                            Thread.sleep(35);
                        }
                    } catch (TelegramApiException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
            return (BotApiMethod<?>) methods.get(methods.size() - 1);
        } else return SendMessage.builder()
                .chatId(update.getMessage().getChatId().toString())
                .text("Something wrong...")
                .build();
    }

    @Scheduled(fixedDelayString = "${notification.check.period}")
    public void sendNotification() {
        List<PartialBotApiMethod<?>> methods = hotLineFacade.notificationUpdate();
        log.info(String.valueOf(methods));
        BotUser superAdmin = botUserService.findByRole(Role.SUPER_ADMIN);
        Long chatId = superAdmin.getId();
        BotState currentBotState = chatPropertyModeService.getCurrentBotState(chatId);
        if (methods != null && !methods.isEmpty()) {
            chatPropertyModeService.setCurrentBotState(chatId, BotState.WAIT_MESSAGE_TO_ALL);
            for (PartialBotApiMethod<?> botApiMethod : methods) {
                try {
                        execute((SendDocument) botApiMethod);
                        Thread.sleep(35);
                } catch (TelegramApiException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            chatPropertyModeService.setCurrentBotState(chatId, currentBotState);
        }
    }
}
