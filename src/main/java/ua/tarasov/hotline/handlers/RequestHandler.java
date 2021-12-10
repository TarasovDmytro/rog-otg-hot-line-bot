package ua.tarasov.hotline.handlers;

import org.telegram.telegrambots.meta.api.objects.Update;
import ua.tarasov.hotline.models.model.ResponseContext;

public interface RequestHandler {
    ResponseContext getResponseContext(Update update);
}
