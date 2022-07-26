package ua.tarasov.hotline.controller.impl;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.tarasov.hotline.models.RogOTGHotLineBot;

@RestController
public class HotLineController {
    private final RogOTGHotLineBot bot;

    public HotLineController(RogOTGHotLineBot bot) {
        this.bot = bot;
    }

    @PostMapping("/")
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        return bot.onWebhookUpdateReceived(update);
    }

    @GetMapping("/")
    public ResponseEntity<?> get() {
        return ResponseEntity.ok().build();
    }
}
