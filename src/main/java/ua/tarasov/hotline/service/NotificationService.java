package ua.tarasov.hotline.service;

import ua.tarasov.hotline.entities.Notification;

import java.util.List;

public interface NotificationService {
    Boolean isExist(String date, String title);
    void saveUpdateNotification(Notification notification);
    void cleanNotificationDB(long countNotificationByPage);
    List<Notification> findAll();
    Notification findByDateAndTitle(String date, String title);
}
