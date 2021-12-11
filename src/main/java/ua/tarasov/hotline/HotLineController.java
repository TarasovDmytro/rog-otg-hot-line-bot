package ua.tarasov.hotline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@Slf4j
public class HotLineController {
//private  final RogOTGHotLineBot bot;
private final HotLineFacade facade;

    public HotLineController(HotLineFacade facade) {
        this.facade = facade;
    }

//    public HotLineController(RogOTGHotLineBot bot) {
//        this.bot = bot;
//    }

    @PostMapping("/")
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        log.info("got update{}", update);
        return facade.onWebhookUpdateReceived(update);
//        return bot.onWebhookUpdateReceived(update);
    }
}
