package ua.tarasov.hotline.service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.tarasov.hotline.models.entities.News;
import ua.tarasov.hotline.repository.NewsRepository;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewsService {
    final NewsRepository repository;

    public NewsService(NewsRepository repository) {
        this.repository = repository;
    }

    public Boolean isExist(String newsLink) {
        return repository.existsNewsByLink(newsLink);
    }

    @Transactional
    public void saveNews(News news) {
        repository.save(news);
        if (repository.count() > 10) {
            repository.deleteById(news.getId() - 10);
        }
    }
}
