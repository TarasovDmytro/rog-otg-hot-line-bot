package ua.tarasov.hotline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.tarasov.hotline.entities.Notification;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Boolean existsNotificationByDateAndTitle(String date, String title);
    Notification findNotificationByDateAndTitle(String date, String title);
}
