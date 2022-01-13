package ua.tarasov.hotline.service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.tarasov.hotline.models.entities.News;
import ua.tarasov.hotline.repository.NewsRepository;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewsService {
    final NewsRepository repository;

    public NewsService(NewsRepository repository) {
        this.repository = repository;
    }

    public Boolean isExist(String newsTitle) {
        return repository.existsNewsByTitle(newsTitle);
    }

    @Transactional
    public void saveNews(News news) {
        repository.save(news);
        if (repository.count() > 10) {
            repository.deleteById(news.getId() - 10);
        }
    }

    public List<News> findAll() {
        return repository.findAll();
    }
}
