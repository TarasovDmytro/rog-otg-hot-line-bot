package ua.tarasov.hotline.facade;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ua.tarasov.hotline.controller.impl.BotUserController;
import ua.tarasov.hotline.handlers.RequestHandler;
import ua.tarasov.hotline.handlers.impl.CallBackQueryHandler;
import ua.tarasov.hotline.handlers.impl.MessageHandler;
import ua.tarasov.hotline.listener.NotificationListener;
import ua.tarasov.hotline.listener.WebSiteListener;

import java.util.List;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotLineFacadeImpl implements HotLineFacade {
    final RequestHandler messageHandler;
    final RequestHandler callBackQueryHandler;
    final WebSiteListener webSiteListener;
    final BotUserController botUserController;
    @Value("${notification.urls}")
    String notificationUrl;


    public HotLineFacadeImpl(MessageHandler messageHandler, CallBackQueryHandler callBackQueryHandler,
                             NotificationListener notificationListener, BotUserController botUserController) {
        this.messageHandler = messageHandler;
        this.callBackQueryHandler = callBackQueryHandler;
        this.webSiteListener = notificationListener;
        this.botUserController = botUserController;
    }

    @Override
    public List<BotApiMethod<?>> handleUpdate(@NotNull Update update) {
        log.info("facade get update = {}", update);
        if (update.hasCallbackQuery()) {
            return callBackQueryHandler.getHandlerUpdate(update);
        }
        if (update.hasMessage()) {
            log.info("return messageHandler action");
            return messageHandler.getHandlerUpdate(update);
        }
        User telegramUser = update.getMyChatMember().getFrom();
        return botUserController.setStartProperties(telegramUser);
    }
    @Override
    public List<BotApiMethod<?>> notificationUpdate() {
        return webSiteListener.getWebSiteUpdate(notificationUrl);
    }
}

