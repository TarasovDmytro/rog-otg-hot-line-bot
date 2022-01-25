package ua.tarasov.hotline.service;

import ua.tarasov.hotline.entities.Notification;

import java.util.List;

public interface NotificationParseService {
    List<Notification> getNewNotifications(String url);
}
