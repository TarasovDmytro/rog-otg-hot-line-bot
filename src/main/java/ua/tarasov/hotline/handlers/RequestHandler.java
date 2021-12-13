package ua.tarasov.hotline.handlers;

import com.google.gson.Gson;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public interface RequestHandler {
    String START_TEXT = "\uD83D\uDC4C Дякую, давайте почнемо";
    AtomicReference<String> stateText = new AtomicReference<>("null");
    AtomicReference<Location> location = new AtomicReference<>(null);
    String TRUE_ACTION_STATE_TEXT = "✅  Виконана";
    String FALSE_ACTION_STATE_TEXT = "⭕️ На виконанні";

    Gson jsonConverter = new Gson();


    List<BotApiMethod<?>> getHandlerUpdate(Update update);
}
