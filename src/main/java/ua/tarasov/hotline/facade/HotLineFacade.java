package ua.tarasov.hotline.facade;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.tarasov.hotline.handlers.CallBackQueryHandler;
import ua.tarasov.hotline.handlers.MessageHandler;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class HotLineFacade {
    private final MessageHandler messageHandler;
    private final CallBackQueryHandler callBackQueryHandler;

    public HotLineFacade(MessageHandler messageHandler, CallBackQueryHandler callBackQueryHandler) {
        this.messageHandler = messageHandler;
        this.callBackQueryHandler = callBackQueryHandler;
    }

    public List<BotApiMethod<?>> handleUpdate(@NotNull Update update) {
        List<BotApiMethod<?>> methods = new ArrayList<>();
        log.info("facade get update = {}", update);
        if (update.hasCallbackQuery()) {
            log.info("return callBackQueryHandler action");
           return callBackQueryHandler.getHandlerUpdate(update);
        }

        if (update.hasMessage()) {
            log.info("return messageHandler action");
            return messageHandler.getHandlerUpdate(update);
        }
        log.info("return simple message");
        return List.of(SendMessage.builder()
                .chatId(String.valueOf(1138897828))
                .text("Something wrong...")
                .build());
    }
}

