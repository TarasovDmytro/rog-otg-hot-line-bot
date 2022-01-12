package ua.tarasov.hotline.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ua.tarasov.hotline.models.entities.BotUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Getter
@Setter
public class NewsService {
    @Value("https://roganska-gromada.gov.ua/more_news/")
    private String url;

    private final BotUserService botUserService;
    private final List<BotApiMethod<?>> answerMessages = new ArrayList<>();

    public NewsService(BotUserService botUserService) {
        this.botUserService = botUserService;
    }

    public List<BotApiMethod<?>> getNews() {
        try {
            Document doc = Jsoup.connect("https://roganska-gromada.gov.ua/more_news/")
                    .userAgent("Mozilla")
                    .timeout(5000)
                    .referrer("https://google.com")
                    .get();
            Elements news = doc.getElementsByClass("news_title");
            for (Element element : news){
                String title = element.html();
                log.info(title);
                List<BotUser> botUsers = botUserService.findAll();
                botUsers.forEach(botUser -> answerMessages.add(SendMessage.builder()
                        .chatId(String.valueOf(botUser.getId()))
                        .text(title)
                        .parseMode("HTML")
                        .build()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return answerMessages;
    }
}