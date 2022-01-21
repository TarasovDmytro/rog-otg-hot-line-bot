package ua.tarasov.hotline.facade;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.tarasov.hotline.handlers.CallBackQueryHandler;
import ua.tarasov.hotline.handlers.MessageHandler;
import ua.tarasov.hotline.handlers.NotificationHandler;

import java.util.List;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotLineFacade {
    final MessageHandler messageHandler;
    final CallBackQueryHandler callBackQueryHandler;
    final NotificationHandler notificationHandler;

    public HotLineFacade(MessageHandler messageHandler, CallBackQueryHandler callBackQueryHandler,
                         NotificationHandler notificationHandler) {
        this.messageHandler = messageHandler;
        this.callBackQueryHandler = callBackQueryHandler;
        this.notificationHandler = notificationHandler;
    }

    public List<BotApiMethod<?>> handleUpdate(@NotNull Update update) {
        log.info("facade get update = {}", update);
        if (update.hasCallbackQuery()) {
            log.info("return callBackQueryHandler action");
           return callBackQueryHandler.getHandlerUpdate(update);
        } else {
            log.info("return messageHandler action");
            return messageHandler.getHandlerUpdate(update);
        }
    }

    public List<BotApiMethod<?>> getUpdateNotification() {
        return notificationHandler.getNewNotifications();
    }
}

