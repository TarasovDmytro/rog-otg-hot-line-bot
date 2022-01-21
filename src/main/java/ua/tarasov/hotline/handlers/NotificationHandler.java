package ua.tarasov.hotline.handlers;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ua.tarasov.hotline.models.entities.BotUser;
import ua.tarasov.hotline.models.entities.Notification;
import ua.tarasov.hotline.service.BotUserService;
import ua.tarasov.hotline.service.NotificationParser;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationHandler {
    @Value("${notifications.url}")
    String notificationUrl;
    List<Notification> newNotifications = new ArrayList<>();
    final NotificationParser parser;
    final BotUserService botUserService;

    public NotificationHandler(NotificationParser parser, BotUserService botUserService) {
        this.parser = parser;
        this.botUserService = botUserService;
    }

    public List<BotApiMethod<?>> getNewNotifications() {
        List<String> notificationUrls = new ArrayList<>(List.of(notificationUrl.split(";")));
        log.info("notificationUrls: {}", notificationUrls);
        List<BotApiMethod<?>> answerMessages = new ArrayList<>();
        notificationUrls.forEach(url -> newNotifications = parser.getNewNotifications(url));
        log.info("New notifications: {}", newNotifications);
        newNotifications.forEach(notification -> {
            List<BotUser> botUsers = botUserService.findAll();
            botUsers.forEach(botUser -> answerMessages.add(SendMessage.builder()
                    .chatId(String.valueOf(botUser.getId()))
                    .text(notification.getDate() + "\n" + notification.getLink())
                    .parseMode("HTML")
                    .build()));
        });
        return answerMessages;
    }
}
