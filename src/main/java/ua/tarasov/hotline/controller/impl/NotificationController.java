package ua.tarasov.hotline.controller.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import ua.tarasov.hotline.controller.Controller;
import ua.tarasov.hotline.entities.Notification;
import ua.tarasov.hotline.service.NotificationService;
import ua.tarasov.hotline.service.impl.NotificationServiceImpl;

import java.util.ArrayList;
import java.util.List;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationController implements Controller {
    final NotificationService notificationService;

    public NotificationController(NotificationServiceImpl notificationService) {
        this.notificationService = notificationService;
    }

    public List<PartialBotApiMethod<?>> getNotifications(Message message) {
        List<PartialBotApiMethod<?>> responseMessages = new ArrayList<>();
        List<Notification> notifications = notificationService.findAll();
        long countLastNotifications = 10;
        if (notifications.size() > countLastNotifications){
            long skipParameter = notifications.size() - countLastNotifications;
            notifications = notifications.stream().skip(skipParameter).toList();
        }
        notifications.forEach(notification -> {
            String messageText = notification.getDate() + "\n" + notification.getLink();
            responseMessages.add(Controller.getSimpleResponseToRequest(message, messageText).get(0));
        });
        return responseMessages;
    }
}
