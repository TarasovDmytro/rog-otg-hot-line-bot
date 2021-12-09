package ua.tarasov.hotline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ua.tarasov.hotline.handlers.CallBackQueryHandler;
import ua.tarasov.hotline.handlers.MessageHandler;
import ua.tarasov.hotline.models.model.ResponseContext;

import java.util.List;

@Component
public class HotLineController extends TelegramLongPollingBot {
    private final MessageHandler messageHandler;
    private final CallBackQueryHandler callBackQueryHandler;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    public HotLineController(MessageHandler messageHandler, CallBackQueryHandler callBackQueryHandler) {
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
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            sendAnswerMessages(callBackQueryHandler.getResponseContext(update));
        }

        if (update.hasMessage()) {
            sendAnswerMessages(messageHandler.getResponseContext(update));
        }
    }

    public void sendAnswerMessages(ResponseContext response) {
        List<SendMessage> answerMessages = response.getAnswerMessages();
        List<AnswerCallbackQuery> answerCallbackQueries = response.getAnswerCallbackQueries();
        List<EditMessageReplyMarkup> editMessageReplyMarkups = response.getEditMessageReplyMarkups();
        List<SendLocation> sendLocations = response.getSendLocations();
        if (answerMessages != null) {
            answerMessages.forEach(message -> {
                try {
                    execute(message);
                    Thread.sleep(35);
                } catch (TelegramApiException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        if (answerCallbackQueries != null) {
            answerCallbackQueries.forEach(message -> {
                try {
                    execute(message);
                    Thread.sleep(35);
                } catch (TelegramApiException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        if (editMessageReplyMarkups != null) {
            editMessageReplyMarkups.forEach(message -> {
                try {
                    execute(message);
                    Thread.sleep(35);
                } catch (TelegramApiException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        if (sendLocations != null) {
            sendLocations.forEach(message -> {
                try {
                    execute(message);
                    Thread.sleep(35);
                } catch (TelegramApiException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
