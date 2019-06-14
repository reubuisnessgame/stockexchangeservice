package com.github.reubuisnessgame.gamebank.stockexchangeservice.model;

import javax.persistence.*;

@Entity
@Table(name = "news")
public class NewsModel {

    @Id
    @GeneratedValue
    @Column(name = "news_id", nullable = false, unique = true)
    private Long id;

    @Column(name = "heading")
    private String heading;

    @Column(name = "article")
    private String article;

    @Column(name = "create_date")
    private String createDate;

    @Column(name = "create_millis")
    private Long timeMillis;

    public NewsModel() {
    }

    public NewsModel(String heading, String article, String createDate, Long timeMillis) {
        this.heading = heading;
        this.article = article;
        this.createDate = createDate;
        this.timeMillis = timeMillis;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getArticle() {
        return article;
    }

    public void setArticle(String article) {
        this.article = article;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public Long getTimeMillis() {
        return timeMillis;
    }

    public void setTimeMillis(Long timeMillis) {
        this.timeMillis = timeMillis;
    }
}
