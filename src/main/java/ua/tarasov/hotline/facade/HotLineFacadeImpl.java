package ua.tarasov.hotline.facade;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.tarasov.hotline.handlers.RequestHandler;
import ua.tarasov.hotline.handlers.impl.CallBackQueryHandler;
import ua.tarasov.hotline.handlers.impl.MessageHandler;
import ua.tarasov.hotline.listener.WebSiteListener;
import ua.tarasov.hotline.listener.NotificationListener;

import java.util.List;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotLineFacadeImpl implements HotLineFacade {
    final RequestHandler messageHandler;
    final RequestHandler callBackQueryHandler;
    final WebSiteListener webSiteListener;
    @Value("${notification.urls}")
    String notificationUrl;


    public HotLineFacadeImpl(MessageHandler messageHandler, CallBackQueryHandler callBackQueryHandler,
                             NotificationListener notificationListener) {
        this.messageHandler = messageHandler;
        this.callBackQueryHandler = callBackQueryHandler;
        this.webSiteListener = notificationListener;
    }

    @Override
    public List<BotApiMethod<?>> handleUpdate(@NotNull Update update) {
        log.info("facade get update = {}", update);
        if (update.hasCallbackQuery()) {
            return callBackQueryHandler.getHandlerUpdate(update);
        } else {
            log.info("return messageHandler action");
            return messageHandler.getHandlerUpdate(update);
        }
    }

    @Override
    public List<BotApiMethod<?>> notificationUpdate() {
        return webSiteListener.getWebSiteUpdate(notificationUrl);
    }
}

