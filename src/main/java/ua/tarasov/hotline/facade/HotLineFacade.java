package ua.tarasov.hotline.facade;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.tarasov.hotline.handlers.CallBackQueryHandler;
import ua.tarasov.hotline.handlers.MessageHandler;

import java.util.ArrayList;
import java.util.List;

@Component
public class HotLineFacade {
    private final MessageHandler messageHandler;
    private final CallBackQueryHandler callBackQueryHandler;

    public HotLineFacade(MessageHandler messageHandler, CallBackQueryHandler callBackQueryHandler) {
        this.messageHandler = messageHandler;
        this.callBackQueryHandler = callBackQueryHandler;
    }

    public List<BotApiMethod<?>> handleUpdate(@NotNull Update update) {
        List<BotApiMethod<?>> methods = new ArrayList<>();
        if (update.hasCallbackQuery()) {
           methods = callBackQueryHandler.getHandlerUpdate(update);
        }

        if (update.hasMessage()) {
            methods = messageHandler.getHandlerUpdate(update);
        }
        return methods;
    }
}

