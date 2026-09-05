package com.financialfraudassistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_questions", indexes = @Index(name = "idx_quiz_module", columnList = "module_id"))
public class QuizQuestion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false) @JoinColumn(name = "module_id", nullable = false)
    private EducationModule module;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String options;
    @Column(name = "correct_index", nullable = false)
    private Integer correctIndex;
    @Column(columnDefinition = "TEXT")
    private String explanation;

    protected QuizQuestion() { }

    public QuizQuestion(EducationModule module, String question, String options, int correctIndex, String explanation) {
        this.module = module;
        this.question = question;
        this.options = options;
        this.correctIndex = correctIndex;
        this.explanation = explanation;
    }

    public Integer getId() { return id; }
    public EducationModule getModule() { return module; }
    public String getQuestion() { return question; }
    public String getOptions() { return options; }
    public Integer getCorrectIndex() { return correctIndex; }
    public String getExplanation() { return explanation; }
}