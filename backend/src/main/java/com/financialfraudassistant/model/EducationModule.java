package com.financialfraudassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "education_modules")
public class EducationModule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String topic;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(name = "duration_mins", nullable = false)
    private Integer durationMins;
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    protected EducationModule() { }

    public EducationModule(String title, String topic, String category, String content, int durationMins, int orderIndex) {
        this.title = title;
        this.topic = topic;
        this.category = category;
        this.content = content;
        this.durationMins = durationMins;
        this.orderIndex = orderIndex;
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getTopic() { return topic; }
    public String getCategory() { return category; }
    public String getContent() { return content; }
    public Integer getDurationMins() { return durationMins; }
    public Integer getOrderIndex() { return orderIndex; }
}