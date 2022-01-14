package ua.tarasov.hotline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.tarasov.hotline.models.entities.News;

public interface NewsRepository extends JpaRepository<News, Long> {
    Boolean existsNewsByLink(String newsLink);
}
