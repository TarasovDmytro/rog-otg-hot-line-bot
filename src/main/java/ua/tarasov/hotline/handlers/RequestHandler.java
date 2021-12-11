package ua.tarasov.hotline.handlers;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public interface RequestHandler {
    List<BotApiMethod<?>> getResponseContext(Update update);
}
