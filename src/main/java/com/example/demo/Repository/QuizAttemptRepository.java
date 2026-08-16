package com.example.demo.Repository;

import com.example.demo.Entity.QuizAttempt;
import com.example.demo.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUser(User user);
    List<QuizAttempt> findAllByOrderByScoreDesc();
    List<QuizAttempt> findTop10ByOrderByScoreDesc();
}