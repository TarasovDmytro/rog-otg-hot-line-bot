package ua.tarasov.hotline.models.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.starter.SpringWebhookBot;
import ua.tarasov.hotline.facade.HotLineFacade;

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
        return hotLineFacade.handleUpdate(update);
    }
}
