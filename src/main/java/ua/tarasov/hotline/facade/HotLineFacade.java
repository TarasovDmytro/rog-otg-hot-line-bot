package ua.tarasov.hotline.facade;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public interface HotLineFacade {
    List<BotApiMethod<?>> handleUpdate(Update update);
    List<BotApiMethod<?>> notificationUpdate();
}
