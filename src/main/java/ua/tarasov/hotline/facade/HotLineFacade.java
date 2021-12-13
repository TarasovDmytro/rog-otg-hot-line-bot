package ua.tarasov.hotline.facade;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ua.tarasov.hotline.handlers.CallBackQueryHandler;
import ua.tarasov.hotline.handlers.MessageHandler;
import ua.tarasov.hotline.models.model.RogOTGHotLineBot;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class HotLineFacade {
    private final MessageHandler messageHandler;
    private final CallBackQueryHandler callBackQueryHandler;
    private final RogOTGHotLineBot telegramBot;

    public HotLineFacade(MessageHandler messageHandler, CallBackQueryHandler callBackQueryHandler, RogOTGHotLineBot telegramBot) {
        this.messageHandler = messageHandler;
        this.callBackQueryHandler = callBackQueryHandler;
        this.telegramBot = telegramBot;
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
            try {
                telegramBot.execute(message);
                Thread.sleep(35);
            } catch (TelegramApiException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        return botApiMethod.get();
    }
}

