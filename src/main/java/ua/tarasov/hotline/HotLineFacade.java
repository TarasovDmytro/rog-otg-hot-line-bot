package ua.tarasov.hotline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ua.tarasov.hotline.handlers.CallBackQueryHandler;
import ua.tarasov.hotline.handlers.MessageHandler;
import ua.tarasov.hotline.models.model.ResponseContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class HotLineFacade extends TelegramWebhookBot {
//public class HotLineFacade extends TelegramLongPollingBot {
    private final MessageHandler messageHandler;
    private final CallBackQueryHandler callBackQueryHandler;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.path}")
    private String botPath;

    public HotLineFacade(MessageHandler messageHandler, CallBackQueryHandler callBackQueryHandler) {
        this.messageHandler = messageHandler;
        this.callBackQueryHandler = callBackQueryHandler;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotPath() {
        return botPath;
    }

//    @Override
//    public void onUpdateReceived(Update update) {
//        if (update.hasCallbackQuery()) {
//            sendAnswerMessages(callBackQueryHandler.getResponseContext(update));
//        }
//
//        if (update.hasMessage()) {
//            sendAnswerMessages(messageHandler.getResponseContext(update));
//        }
//    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        BotApiMethod<?> botApiMethod = null;
        if (update.hasCallbackQuery()) {
            botApiMethod = sendAnswerMessages(callBackQueryHandler.getResponseContext(update));
        }

        if (update.hasMessage()) {
            botApiMethod = sendAnswerMessages(messageHandler.getResponseContext(update));
        }
        return botApiMethod;
    }

    public BotApiMethod<?> sendAnswerMessages(List<BotApiMethod<?>> response) {
        AtomicReference<BotApiMethod<?>> botApiMethod = new AtomicReference<>(null);
        response.forEach(message -> {
                    botApiMethod.set(message);
                    try {
//                        execute(message);
                        Thread.sleep(35);
                    } catch ( InterruptedException e) {
                        e.printStackTrace();
                    }
                });
        return botApiMethod.get();
    }
}

