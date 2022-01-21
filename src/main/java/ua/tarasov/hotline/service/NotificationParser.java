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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ua.tarasov.hotline.models.entities.BotUser;
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
    @Value("${notifications.url}")
    String notificationUrl;
//    @Value("${news.url}")
//    String newsUrl;

    final BotUserService botUserService;
    final NotificationService notificationService;
    List<Notification> newNotifications = new ArrayList<>();

    public NotificationParser(@Autowired BotUserService botUserService, @Autowired NotificationService notificationService) {
        this.botUserService = botUserService;
        this.notificationService = notificationService;
    }

    public List<BotApiMethod<?>> getMethodsForSendNews() {
        List<String> notificationUrls = new ArrayList<>(List.of(notificationUrl.split(";")));
        log.info("notificationUrls: {}", notificationUrls);
        List<BotApiMethod<?>> answerMessages = new ArrayList<>();
        notificationUrls.forEach(this::getNewNotifications);
//        getNewNotifications(newsUrl);
        log.info("New notifications: {}", newNotifications);
        newNotifications.forEach(notification -> {
            notificationService.saveNotification(notification);
            List<BotUser> botUsers = botUserService.findAll();
            botUsers.forEach(botUser -> answerMessages.add(SendMessage.builder()
                    .chatId(String.valueOf(botUser.getId()))
                    .text(notification.getDate() + "\n" + notification.getLink())
                    .parseMode("HTML")
                    .build()));
        });
        return answerMessages;
    }

    private void getNewNotifications(String url) {
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
                newNotifications.add(notification);
            }
        });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}