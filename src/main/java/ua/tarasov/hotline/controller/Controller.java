package ua.tarasov.hotline.controller;

import com.google.gson.Gson;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import ua.tarasov.hotline.service.ChatPropertyModeService;
import ua.tarasov.hotline.service.impl.ChatPropertyModeServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public interface Controller {
    AtomicReference<String> stateText = new AtomicReference<>("null");
    String TRUE_ACTION_STATE_TEXT = "✅  Виконана";
    String FALSE_ACTION_STATE_TEXT = "⭕️ На виконанні";

    Gson jsonConverter = new Gson();

    ChatPropertyModeService chatPropertyModeService = ChatPropertyModeServiceImpl.getChatProperties();

    @Contract("_, _ -> new")
    static List<BotApiMethod<?>> getSimpleResponseToRequest(@NotNull Message message, String textMessage) {
        return Collections.singletonList(SendMessage.builder()
                .chatId(String.valueOf(message.getChatId()))
                .text(textMessage)
                .parseMode("HTML")
                .build());
    }
}
