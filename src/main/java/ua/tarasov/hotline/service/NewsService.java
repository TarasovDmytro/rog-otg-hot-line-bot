package ua.tarasov.hotline.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@Getter
@Setter
public class NewsService {
    @Value("https://roganska-gromada.gov.ua/more_news/")
    private String url;

    @Scheduled(fixedDelayString = "60000")
    public void getNews() {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla")
                    .timeout(5000)
                    .referrer("https://google.com")
                    .get();
            Elements news = doc.getElementsByClass("news_title");
            for (Element element : news){
                String title = String.valueOf(element.getElementsByAttribute("href"));
                log.info(title);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}