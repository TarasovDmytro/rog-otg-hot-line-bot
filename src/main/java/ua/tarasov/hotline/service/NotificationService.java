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

    public Boolean isExist(String newsDate) {
        return repository.existsNotificationByDate(newsDate);
    }

    @Transactional
    public void saveNotification(Notification notification) {
        repository.save(notification);
        if (repository.count() > 20) {
            repository.deleteById(notification.getId() - 20);
        }
    }
}
