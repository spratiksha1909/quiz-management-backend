package com.example.demo.controller;

import com.example.demo.Entity.Question;
import com.example.demo.Entity.Quiz;
import com.example.demo.Entity.QuizAttempt;
import com.example.demo.Entity.User;
import com.example.demo.Repository.QuestionRepository;
import com.example.demo.Repository.QuizAttemptRepository;
import com.example.demo.Repository.QuizRepository;
import com.example.demo.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "*")
public class StudentQuizController {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private UserRepository userRepository;
   

    // 1. Dashboard Statistics & Recent Attempts API (Updated for Safe Null Handling)
    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(Principal principal) {
        List<QuizAttempt> attempts = new ArrayList<>();

        try {
            if (principal != null && principal.getName() != null) {
                User student = userRepository.findByEmail(principal.getName()).orElse(null);
                if (student != null) {
                    attempts = quizAttemptRepository.findByUser(student);
                } else {
                    attempts = quizAttemptRepository.findAll();
                }
            } else {
                // Fallback: Fetch all attempts if principal is null
                attempts = quizAttemptRepository.findAll();
            }
        } catch (Exception e) {
            attempts = quizAttemptRepository.findAll();
        }

        int totalAttempted = attempts.size();
        int passed = 0;
        int failed = 0;
        double totalScorePercentage = 0;
        double highestScore = 0;
        int totalAnswered = 0;

        List<Map<String, Object>> recentAttempts = new ArrayList<>();

        for (QuizAttempt attempt : attempts) {
            if (attempt.getStatus() == QuizAttempt.AttemptStatus.PASSED) {
                passed++;
            } else {
                failed++;
            }

            double score = attempt.getPercentage() != null ? attempt.getPercentage() : 0.0;
            totalScorePercentage += score;
            if (score > highestScore) {
                highestScore = score;
            }

            int answeredInThisAttempt = (attempt.getCorrectAnswers() != null ? attempt.getCorrectAnswers() : 0) 
                                      + (attempt.getIncorrectAnswers() != null ? attempt.getIncorrectAnswers() : 0);
            totalAnswered += answeredInThisAttempt;

            Map<String, Object> map = new HashMap<>();
            map.put("quizTitle", attempt.getQuiz() != null ? attempt.getQuiz().getTitle() : "Quiz");
            map.put("date", attempt.getCompletedAt() != null ? attempt.getCompletedAt().toLocalDate().toString() : "Today");
            map.put("score", Math.round(score));
            map.put("status", attempt.getStatus() != null ? attempt.getStatus().name() : "FAILED");
            recentAttempts.add(map);
        }

        double avgScore = totalAttempted > 0 ? (totalScorePercentage / totalAttempted) : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("attempted", totalAttempted);
        stats.put("passed", passed);
        stats.put("failed", failed);
        stats.put("avgScore", Math.round(avgScore));
        stats.put("highestScore", Math.round(highestScore));
        stats.put("totalAnswered", totalAnswered);

        Map<String, Object> response = new HashMap<>();
        response.put("stats", stats);
        response.put("recentAttempts", recentAttempts);

        return ResponseEntity.ok(response);
    }

    // 2. Get Published Quizzes
    @GetMapping("/quizzes")
    public ResponseEntity<List<Quiz>> getPublishedQuizzes() {
        return ResponseEntity.ok(quizRepository.findByPublished(true));
    }

    // 3. Get Specific Quiz Details
    @GetMapping("/quizzes/{id}")
    public ResponseEntity<Quiz> getQuizDetails(@PathVariable Long id) {
        return quizRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. Start Quiz
    @PostMapping("/quizzes/{id}/start")
    public ResponseEntity<Map<String, Object>> startQuiz(@PathVariable Long id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        List<Question> questions = questionRepository.findByQuizId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("quiz", quiz);
        response.put("questions", questions);
        response.put("totalQuestions", questions.size());

        return ResponseEntity.ok(response);
    }

    // 5. Submit Quiz & Save Attempt
    @PostMapping("/quizzes/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitQuiz(
            @PathVariable Long id,
            @RequestBody Map<Long, String> userAnswers,
            Principal principal) {

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        List<Question> questions = questionRepository.findByQuizId(id);

        int correctAnswers = 0;
        int incorrectAnswers = 0;
        int unanswered = 0;
        int totalQuestions = questions.size();
        int obtainedMarks = 0;

        List<Map<String, Object>> questionReviews = new ArrayList<>();

        for (Question q : questions) {
            String selectedOption = userAnswers.get(q.getId());
            boolean isCorrect = false;

            if (selectedOption == null || selectedOption.trim().isEmpty()) {
                unanswered++;
            } else if (q.getAnswer() != null && selectedOption.trim().equalsIgnoreCase(q.getAnswer().trim())) {
                correctAnswers++;
                isCorrect = true;
                obtainedMarks += 1;
            } else {
                incorrectAnswers++;
            }

            Map<String, Object> review = new HashMap<>();
            review.put("questionId", q.getId());
            review.put("questionText", q.getContent());
            review.put("selectedOption", selectedOption != null ? selectedOption : "Not Answered");
            review.put("correctAnswer", q.getAnswer());
            review.put("explanation", "No explanation provided");
            review.put("isCorrect", isCorrect);

            questionReviews.add(review);
        }

        double percentage = totalQuestions > 0 ? ((double) correctAnswers / totalQuestions) * 100 : 0;
        boolean isPassed = percentage >= 40.0;

        try {
            QuizAttempt attempt = new QuizAttempt();
            attempt.setQuiz(quiz);

            if (principal != null && principal.getName() != null) {
                User student = userRepository.findByEmail(principal.getName()).orElse(null);
                attempt.setUser(student);
            }

            attempt.setScore((double) obtainedMarks);
            attempt.setPercentage(percentage);
            attempt.setCorrectAnswers(correctAnswers);
            attempt.setIncorrectAnswers(incorrectAnswers);
            attempt.setUnanswered(unanswered);
            attempt.setStatus(isPassed ? QuizAttempt.AttemptStatus.PASSED : QuizAttempt.AttemptStatus.FAILED);
            attempt.setCompletedAt(LocalDateTime.now());

            quizAttemptRepository.save(attempt);
        } catch (Exception e) {
            System.err.println("Error saving quiz attempt: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("quizTitle", quiz.getTitle());
        result.put("totalQuestions", totalQuestions);
        result.put("correctAnswers", correctAnswers);
        result.put("incorrectAnswers", incorrectAnswers);
        result.put("unanswered", unanswered);
        result.put("obtainedMarks", obtainedMarks);
        result.put("percentage", Math.round(percentage * 100.0) / 100.0);
        result.put("status", isPassed ? "PASSED" : "FAILED");
        result.put("reviews", questionReviews);

        return ResponseEntity.ok(result);
    }
   

    // Leaderboard API Endpoint
 // 🏆 Leaderboard API Endpoint
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard() {
        List<QuizAttempt> topPerformers = quizAttemptRepository.findTop10ByOrderByScoreDesc();
        return ResponseEntity.ok(topPerformers);
    }
}
