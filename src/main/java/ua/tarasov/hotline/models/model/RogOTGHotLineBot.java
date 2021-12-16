package ua.tarasov.hotline.models.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.starter.SpringWebhookBot;
import ua.tarasov.hotline.facade.HotLineFacade;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RogOTGHotLineBot extends SpringWebhookBot {
    String botPath;
    String botUsername;
    String botToken;

    private HotLineFacade hotLineFacade;

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
        List<BotApiMethod<?>> methods = hotLineFacade.handleUpdate(update);
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
}
