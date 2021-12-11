package ua.tarasov.hotline.facade;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.tarasov.hotline.handlers.CallBackQueryHandler;
import ua.tarasov.hotline.handlers.MessageHandler;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class HotLineFacade {
    private final MessageHandler messageHandler;
    private final CallBackQueryHandler callBackQueryHandler;

    public HotLineFacade(MessageHandler messageHandler, CallBackQueryHandler callBackQueryHandler) {
        this.messageHandler = messageHandler;
        this.callBackQueryHandler = callBackQueryHandler;
    }

    public BotApiMethod<?> handleUpdate(Update update) {
        BotApiMethod<?> botApiMethod = null;
        if (update.hasCallbackQuery()) {
            botApiMethod = sendAnswerMessages(callBackQueryHandler.getHandlerUpdate(update));
        }

        if (update.hasMessage()) {
            botApiMethod = sendAnswerMessages(messageHandler.getHandlerUpdate(update));
        }
        return botApiMethod;
    }

    public BotApiMethod<?> sendAnswerMessages(List<BotApiMethod<?>> response) {
        AtomicReference<BotApiMethod<?>> botApiMethod = new AtomicReference<>(null);
        response.forEach(message -> {
                    botApiMethod.set(message);
            try {
                Thread.sleep(35);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        return botApiMethod.get();
    }
}

