package ua.tarasov.hotline.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.tarasov.hotline.models.entities.UserRequest;

import java.util.List;

@Service
@Slf4j
@Getter
@Setter
public class PingService {
    @Value("https://roganska-gromada.gov.ua/news/")
    private String url;

    @Scheduled(fixedRateString = "600000")
    public void pingMe() {
//        try {
//            URL url = new URL(getUrl());
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.connect();
//            log.info("Ping {}, OK: response headers {}", url.getHost(), connection.getHeaderFields());
//            connection.disconnect();
//        } catch (IOException e) {
//            log.error("Ping FAILED");
//            e.printStackTrace();
//        }
    }
}