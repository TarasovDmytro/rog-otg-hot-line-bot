package ua.tarasov.hotline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.tarasov.hotline.models.model.RogOTGHotLineBot;

@RestController
@Slf4j
public class HotLineController {
private  final RogOTGHotLineBot bot;
private final HotLineFacade facade;

    public HotLineController(RogOTGHotLineBot bot, HotLineFacade facade) {
        this.bot = bot;
        this.facade = facade;
    }

    @PostMapping("/")
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        log.info("got update{}", update);
        return facade.onWebhookUpdateReceived(update);
//        return bot.onWebhookUpdateReceived(update);
    }
}
