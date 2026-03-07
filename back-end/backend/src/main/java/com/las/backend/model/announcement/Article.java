package com.las.backend.model.announcement;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Article {
    @Id
    private String id;
    private String title;
    private String content;

    public Article(String id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }
}
