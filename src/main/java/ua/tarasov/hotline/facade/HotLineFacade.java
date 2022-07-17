package ua.tarasov.hotline.facade;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public interface HotLineFacade {
    List<PartialBotApiMethod<?>> handleUpdate(Update update);
    List<PartialBotApiMethod<?>> notificationUpdate();
}
