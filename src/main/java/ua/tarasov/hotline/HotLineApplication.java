package ua.tarasov.hotline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotLineApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotLineApplication.class, args);
    }

}
