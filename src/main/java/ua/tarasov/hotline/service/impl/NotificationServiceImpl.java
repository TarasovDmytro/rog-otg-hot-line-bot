package ua.tarasov.hotline.service.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ua.tarasov.hotline.entities.Notification;
import ua.tarasov.hotline.repository.NotificationRepository;
import ua.tarasov.hotline.service.NotificationService;

import java.util.List;

@Log4j2
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationServiceImpl implements NotificationService {
    final NotificationRepository repository;

    public NotificationServiceImpl(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Boolean isExist(String notificationDate, String notificationTitle) {
        return repository.existsNotificationByDateAndTitle(notificationDate, notificationTitle);
    }

    @Override
    public void saveUpdateNotification(Notification notification) {
        repository.save(notification);
    }

    @Override
    public void cleanNotificationDB(long countNotificationByPage) {
        while (repository.count() > countNotificationByPage * 2) {
            List<Notification> notifications = repository.findAll(Sort.by("id"));
            log.info("all notification in the db: {}", notifications);
            repository.delete(notifications.get(0));
        }
    }

    @Override
    public List<Notification> findAll() {
        return repository.findAll(Sort.by("link"));
    }

    @Override
    public Notification findByDateAndTitle(String date, String title) {
        return repository.findNotificationByDateAndTitle(date, title);
    }
}
