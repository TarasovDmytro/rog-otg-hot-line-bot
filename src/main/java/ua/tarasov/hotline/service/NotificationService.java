package ua.tarasov.hotline.service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ua.tarasov.hotline.entities.Notification;
import ua.tarasov.hotline.repository.NotificationRepository;

import java.util.List;

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

    public void saveNotification(Notification notification) {
        repository.save(notification);
    }

    public void cleanNotificationDB(){
        while (repository.count() > 20) {
            List<Notification> notifications = repository.findAll(Sort.by("id"));
            repository.delete(notifications.get(0));
        }
    }
}
