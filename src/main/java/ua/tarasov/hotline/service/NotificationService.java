package ua.tarasov.hotline.service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.tarasov.hotline.models.entities.Notification;
import ua.tarasov.hotline.repository.NotificationRepository;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationService {
    final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Boolean isExist(String newsTitle) {
        return repository.existsNotificationByTitle(newsTitle);
    }

    @Transactional
    public void saveNotification(Notification notification) {
        repository.save(notification);
        if (repository.count() > 10) {
            repository.deleteById(notification.getId() - 10);
        }
    }
}
