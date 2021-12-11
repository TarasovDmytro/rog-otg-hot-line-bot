package ua.tarasov.hotline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@Slf4j
public class HotLineController {
    private final HotLineFacade facade;

    public HotLineController(HotLineFacade facade) {
        this.facade = facade;
    }

    @PostMapping(value = "/")
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        log.info("got update{}", update);
        return facade.onWebhookUpdateReceived(update);
    }
}
