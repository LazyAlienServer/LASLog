package com.las.backend.service.announcement;

import com.las.backend.model.announcement.Article;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

@Service
public interface AnnouncementService extends MongoRepository<Article,String> {
}
