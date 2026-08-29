package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.EducationModuleResponse;
import com.financialfraudassistant.dto.LiteracyResponse;
import com.financialfraudassistant.dto.QuizQuestionResponse;
import com.financialfraudassistant.dto.QuizAttemptRequest;
import com.financialfraudassistant.dto.QuizAttemptResponse;
import com.financialfraudassistant.dto.QuizAttemptResponse.QuestionResult;
import com.financialfraudassistant.model.EducationAttempt;
import com.financialfraudassistant.model.EducationModule;
import com.financialfraudassistant.model.QuizQuestion;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.EducationAttemptRepository;
import com.financialfraudassistant.repository.EducationModuleRepository;
import com.financialfraudassistant.repository.QuizQuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EducationService {

    private final EducationModuleRepository moduleRepository;
    private final QuizQuestionRepository questionRepository;
    private final EducationAttemptRepository attemptRepository;

    public EducationService(EducationModuleRepository moduleRepository, QuizQuestionRepository questionRepository,
                            EducationAttemptRepository attemptRepository) {
        this.moduleRepository = moduleRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
    }

    public List<EducationModuleResponse> list(User user) {
        Map<Integer, Integer> bestPerModule = bestScores(user);
        return moduleRepository.findAllByOrderByOrderIndexAsc().stream()
                .map(module -> new EducationModuleResponse(module.getId(), module.getTitle(), module.getTopic(),
                        module.getCategory(), module.getContent(), module.getDurationMins(),
                        bestPerModule.getOrDefault(module.getId(), null)))
                .toList();
    }

    public EducationModuleResponse detail(User user, Integer moduleId) {
        EducationModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        Map<Integer, Integer> bestPerModule = bestScores(user);
        return new EducationModuleResponse(module.getId(), module.getTitle(), module.getTopic(), module.getCategory(),
                module.getContent(), module.getDurationMins(), bestPerModule.getOrDefault(moduleId, null));
    }

    public List<QuizQuestionResponse> quiz(Integer moduleId) {
        return questionRepository.findByModuleId(moduleId).stream()
                .map(question -> new QuizQuestionResponse(question.getId(), question.getQuestion(), splitOptions(question.getOptions())))
                .toList();
    }

    public QuizAttemptResponse attempt(User user, Integer moduleId, QuizAttemptRequest request) {
        EducationModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        List<QuizQuestion> questions = questionRepository.findByModuleId(moduleId);
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This lesson has no quiz yet");
        }
        List<Integer> answers = request.answers();
        if (answers == null || answers.size() != questions.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer every question to submit the quiz");
        }

        int correct = 0;
        List<QuestionResult> results = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion question = questions.get(i);
            int chosen = answers.get(i) == null ? -1 : answers.get(i);
            boolean isCorrect = chosen == question.getCorrectIndex();
            if (isCorrect) correct++;
            results.add(new QuestionResult(question.getId(), question.getQuestion(),
                    optionAt(question.getOptions(), chosen),
                    optionAt(question.getOptions(), question.getCorrectIndex()),
                    isCorrect, question.getExplanation() == null ? "" : question.getExplanation()));
        }
        int scorePct = (int) Math.round(correct * 100.0 / questions.size());
        attemptRepository.save(new EducationAttempt(user, module, scorePct));
        return new QuizAttemptResponse(scorePct, correct, questions.size(), literacyScore(user), results);
    }

    public LiteracyResponse literacy(User user) {
        int score = literacyScore(user);
        List<EducationAttempt> attempts = attemptRepository.findByUserId(user.getId());
        List<String> summary = new ArrayList<>();
        summary.add("You answered " + attempts.size() + " quiz attempt(s) across the learning library.");
        if (score >= 80) summary.add("Strong financial safety awareness - keep reviewing new scam patterns regularly.");
        else if (score >= 60) summary.add("Good awareness. Try the fraud-focused lessons to sharpen detection skills.");
        else summary.add("Learning more is the best defence. Start with the fraud and safety lessons below.");
        String level = score >= 80 ? "Advanced" : score >= 60 ? "Good awareness" : score >= 40 ? "Building awareness" : "Getting started";
        return new LiteracyResponse(score, level, attempts.size(), summary);
    }

    private int literacyScore(User user) {
        Map<Integer, Integer> best = bestScores(user);
        if (best.isEmpty()) return 0;
        double average = best.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        return (int) Math.round(average);
    }

    private Map<Integer, Integer> bestScores(User user) {
        Map<Integer, Integer> best = new HashMap<>();
        attemptRepository.findByUserId(user.getId()).forEach(attempt ->
                best.merge(attempt.getModule().getId(), attempt.getScorePct(), Math::max));
        return best;
    }

    private List<String> splitOptions(String options) {
        return Arrays.stream(options.split("\\R")).map(String::trim).filter(line -> !line.isBlank()).toList();
    }

    private String optionAt(String options, int index) {
        List<String> list = splitOptions(options);
        return index >= 0 && index < list.size() ? list.get(index) : "Not answered";
    }
}