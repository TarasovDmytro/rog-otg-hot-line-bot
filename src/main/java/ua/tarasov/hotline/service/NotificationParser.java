package ua.tarasov.hotline.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ua.tarasov.hotline.models.entities.Notification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationParser {
    final NotificationService notificationService;
    List<Notification> newNotifications = new ArrayList<>();

    public NotificationParser(@Autowired NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public List<Notification> getNewNotifications(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla")
                    .timeout(5000)
                    .referrer("https://google.com")
                    .get();
        Elements newsTitles = doc.getElementsByClass("one_news_col");
        newsTitles.forEach(element -> {
            String link = element.getElementsByClass("news_title").get(0).html();
            String title = element.getElementsByClass("news_title").get(0).text();
            String date = element.getElementsByClass("news_date").get(0).text();
            log.info("link: " + link);
            log.info("title: " + title);
            log.info("date: {}", date);
            if (!notificationService.isExist(date)) {
                Notification notification = new Notification();
                notification.setLink(link);
                notification.setTitle(title);
                notification.setDate(date);
                notificationService.saveNotification(notification);
                newNotifications.add(notification);
            }
        });
        } catch (IOException e) {
            e.printStackTrace();
        }
        notificationService.cleanNotificationDB();
        return newNotifications;
    }
}