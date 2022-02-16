package ua.tarasov.hotline.service.impl;

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
import ua.tarasov.hotline.entities.Notification;
import ua.tarasov.hotline.service.NotificationParseService;
import ua.tarasov.hotline.service.NotificationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationParseServiceImpl implements NotificationParseService {
    final NotificationService notificationService;
    List<Notification> updateNotifications;

    public NotificationParseServiceImpl(@Autowired NotificationServiceImpl notificationService) {
        this.notificationService = notificationService;
    }

    public List<Notification> getUpdateNotifications(String url) {
        updateNotifications = new ArrayList<>();
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
                if (notificationService.isExist(date, title)) {
                    Notification updateNotification = notificationService.findByDateAndTitle(date, title);
                    if (!updateNotification.getLink().equals(link)) {
                        updateNotification.setLink(link);
                        notificationService.saveUpdateNotification(updateNotification);
                        updateNotifications.add(updateNotification);
                    }
                } else {
                    Notification notification = new Notification();
                    notification.setLink(link);
                    notification.setTitle(title);
                    notification.setDate(date);
                    notificationService.saveUpdateNotification(notification);
                    updateNotifications.add(notification);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        notificationService.cleanNotificationDB(10);
        return updateNotifications;
    }
}