package ua.tarasov.hotline.service;

import ua.tarasov.hotline.entities.Notification;

public interface NotificationService {
    Boolean isExist(String notificationParameter);
    void saveNewNotification(Notification notification);
    void cleanNotificationDB(long countNotificationByPage);
}
