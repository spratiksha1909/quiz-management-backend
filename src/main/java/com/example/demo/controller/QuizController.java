package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.Entity.Category;
import com.example.demo.Entity.Question;
import com.example.demo.Entity.Quiz;
import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.QuestionRepository;
import com.example.demo.Repository.QuizRepository;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = "http://localhost:5173")
public class QuizController {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private QuestionRepository questionRepository;


    // --- CATEGORY CRUD ---
    @GetMapping("/categories")
    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    @PostMapping("/categories")
    public Category createCategory(@RequestBody Category category) {
        return categoryRepository.save(category);
    }

    // --- QUIZ CRUD ---
    // Fetch all quizzes (For Admin)
    @GetMapping
    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }

    // Fetch published quizzes only (For Student)
    @GetMapping("/active")
    public List<Quiz> getActiveQuizzes() {
        return quizRepository.findByPublished(true);
    }

    // Create Quiz
    @PostMapping
    public Quiz createQuiz(@RequestBody Quiz quiz) {
        return quizRepository.save(quiz);
    }

    // Update Quiz
    @PutMapping("/{id}")
    public ResponseEntity<Quiz> updateQuiz(@PathVariable Long id, @RequestBody Quiz quizDetails) {
        return quizRepository.findById(id).map(quiz -> {
            quiz.setTitle(quizDetails.getTitle());
            quiz.setDescription(quizDetails.getDescription());
            quiz.setMaxMarks(quizDetails.getMaxMarks());
            quiz.setNumberOfQuestions(quizDetails.getNumberOfQuestions());
            quiz.setDurationMinutes(quizDetails.getDurationMinutes());
            quiz.setCategory(quizDetails.getCategory());
            return ResponseEntity.ok(quizRepository.save(quiz));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Delete Quiz
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id) {
        if (!quizRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        quizRepository.deleteById(id);
        return ResponseEntity.ok("Quiz deleted successfully.");
    }

    // Toggle Publish Status (Publish / Unpublish)
    @PutMapping("/{id}/toggle-publish")
    public ResponseEntity<Quiz> togglePublish(@PathVariable Long id) {
        return quizRepository.findById(id).map(quiz -> {
            quiz.setPublished(!quiz.isPublished());
            return ResponseEntity.ok(quizRepository.save(quiz));
        }).orElse(ResponseEntity.notFound().build());
    }
    
  
    // Get questions of a quiz
    @GetMapping("/{quizId}/questions")
    public List<Question> getQuestionsByQuiz(@PathVariable Long quizId) {
        return questionRepository.findByQuizId(quizId);
    }

    // Add Question to Quiz
    @PostMapping("/{quizId}/questions")
    public Question addQuestion(@PathVariable Long quizId, @RequestBody Question question) {
        return quizRepository.findById(quizId).map(quiz -> {
            question.setQuiz(quiz);
            return questionRepository.save(question);
        }).orElseThrow(() -> new RuntimeException("Quiz not found"));
    }

    // Delete Question
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long questionId) {
        questionRepository.deleteById(questionId);
        return ResponseEntity.ok("Question deleted.");
    }
}