package ua.tarasov.hotline.handlers;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public interface RequestHandler {
    String START_TEXT = "\uD83D\uDC4C Дякую, давайте почнемо";
    String WRONG_ACTION_TEXT = "Вибачте, але Ви додали данні, які я не в змозі обробити, виконайте, будьласка коректну дію";

    List<PartialBotApiMethod<?>> getHandlerUpdate(Update update);
}
