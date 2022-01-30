package ua.tarasov.hotline.service;

import ua.tarasov.hotline.entities.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationService {
    Boolean isExist(String notificationParameter);
    void saveUpdateNotification(Notification notification);
    void cleanNotificationDB(long countNotificationByPage);
    List<Notification> findAll();
    Optional<Notification> findByDate(String date);
}
