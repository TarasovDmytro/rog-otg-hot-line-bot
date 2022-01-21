package ua.tarasov.hotline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.tarasov.hotline.models.entities.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Boolean existsNotificationByDate(String date);
}
