package ua.tarasov.hotline.listener;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ua.tarasov.hotline.entities.BotUser;
import ua.tarasov.hotline.entities.Notification;
import ua.tarasov.hotline.service.NotificationParseService;
import ua.tarasov.hotline.service.impl.BotUserServiceImpl;
import ua.tarasov.hotline.service.impl.NotificationParseServiceImpl;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationListener implements WebSiteListener {
    final NotificationParseService parser;
    final BotUserServiceImpl botUserService;

    public NotificationListener(NotificationParseServiceImpl parser, BotUserServiceImpl botUserService) {
        this.parser = parser;
        this.botUserService = botUserService;
    }

    @Override
    public List<PartialBotApiMethod<?>> getWebSiteUpdate(@NotNull String notificationUrl) {
        List<PartialBotApiMethod<?>> answerMessages = new ArrayList<>();
        List<Notification> newNotifications = parser.getUpdateNotifications(notificationUrl);
        log.info("New notifications: {}", newNotifications);
        if (!newNotifications.isEmpty()) {
            newNotifications.forEach(notification -> {
                List<BotUser> botUsers = botUserService.findAll();
                botUsers.forEach(botUser -> answerMessages.add(SendMessage.builder()
                        .chatId(String.valueOf(botUser.getId()))
                        .text(notification.getDate() + "\n" + notification.getLink())
                        .parseMode("HTML")
                        .build()));
            });
        }
        return answerMessages;
    }
}
