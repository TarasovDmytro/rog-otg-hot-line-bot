package ua.tarasov.hotline.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ua.tarasov.hotline.models.entities.BotUser;
import ua.tarasov.hotline.models.entities.News;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewsParser {
    @Value("https://roganska-gromada.gov.ua/more_news/")
    String url;

    final BotUserService botUserService;
    final NewsService newsService;

    public NewsParser(@Autowired BotUserService botUserService, @Autowired NewsService newsService) {
        this.botUserService = botUserService;
        this.newsService = newsService;
    }

    public List<BotApiMethod<?>> getNews() {
        List<BotApiMethod<?>> answerMessages = new ArrayList<>();
        try {
            Document doc = Jsoup.connect("https://roganska-gromada.gov.ua/more_news/")
                    .userAgent("Mozilla")
                    .timeout(5000)
                    .referrer("https://google.com")
                    .get();
            Elements newsTitles = doc.getElementsByClass("news_title");
            for (Element element : newsTitles){
                String link = element.text();
                log.info(link);
                if (!newsService.isExist(link)) {
                    News news = new News();
                    news.setLink(link);
                    newsService.saveNews(news);
                    List<BotUser> botUsers = botUserService.findAll();
                    botUsers.forEach(botUser -> answerMessages.add(SendMessage.builder()
                            .chatId(String.valueOf(botUser.getId()))
                            .text(news.getLink())
                            .parseMode("HTML") 
                            .build()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return answerMessages;
    }
}