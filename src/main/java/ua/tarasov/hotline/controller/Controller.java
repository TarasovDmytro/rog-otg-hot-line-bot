package ua.tarasov.hotline.controller;

import com.google.gson.Gson;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ua.tarasov.hotline.service.ChatPropertyModeService;
import ua.tarasov.hotline.service.impl.ChatPropertyModeServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public interface Controller {
//    String START_TEXT = "\uD83D\uDC4C Дякую, давайте почнемо";
    AtomicReference<String> stateText = new AtomicReference<>("null");
    String TRUE_ACTION_STATE_TEXT = "✅  Виконана";
    String FALSE_ACTION_STATE_TEXT = "⭕️ На виконанні";
//    String WRONG_ACTION_TEXT = "Вибачте, але Ви додали данні, які я не в змозі обробити, виконайте, будьласка коректну дію";

    Gson jsonConverter = new Gson();

    ChatPropertyModeService chatPropertyModeService = ChatPropertyModeServiceImpl.getChatProperties();

    static List<BotApiMethod<?>> getSimpleResponseToRequest(Message message, String textMessage) {
        return Collections.singletonList(SendMessage.builder()
                .chatId(String.valueOf(message.getChatId()))
                .text(textMessage)
                .parseMode("HTML")
                .build());
    }
}
