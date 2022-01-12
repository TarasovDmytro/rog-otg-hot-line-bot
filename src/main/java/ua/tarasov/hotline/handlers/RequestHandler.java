package ua.tarasov.hotline.handlers;

import com.google.gson.Gson;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public interface RequestHandler {
    String START_TEXT = "\uD83D\uDC4C Дякую, давайте почнемо";
    AtomicReference<String> stateText = new AtomicReference<>("null");
    String TRUE_ACTION_STATE_TEXT = "✅  Виконана";
    String FALSE_ACTION_STATE_TEXT = "⭕️ На виконанні";
    String WRONG_ACTION_TEXT = "Вибачте, але Ви додали данні, які я не в змозі обробити, виконайте, будьласка коректну дію";

    Gson jsonConverter = new Gson();

    List<BotApiMethod<?>> getHandlerUpdate(Update update);

    default List<BotApiMethod<?>> getSimpleResponseToRequest(Message message, String textMessage) {
        return Collections.singletonList(SendMessage.builder()
                .chatId(String.valueOf(message.getChatId()))
                .text(textMessage)
                .parseMode("HTML")
                .build());
    }
}
