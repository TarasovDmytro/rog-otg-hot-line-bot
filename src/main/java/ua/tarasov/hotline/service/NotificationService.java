package ua.tarasov.hotline.service;

import ua.tarasov.hotline.entities.Notification;

import java.util.List;

public interface NotificationService {
    Boolean isExist(String notificationParameter);
    void saveNewNotification(Notification notification);
    void cleanNotificationDB(long countNotificationByPage);
    List<Notification> findAll();
}
