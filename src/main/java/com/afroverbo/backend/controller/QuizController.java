package com.afroverbo.backend.controller;

import com.afroverbo.backend.model.QuizQuestion;
import com.afroverbo.backend.repository.LessonContentRepository;
import com.afroverbo.backend.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QuizController {

    private final QuizQuestionRepository quizQuestionRepository;
    private final LessonContentRepository lessonContentRepository;

    // GET all questions for a lesson
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<List<QuizQuestion>> getQuizByLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(quizQuestionRepository.findByLessonId(lessonId));
    }

    // POST create question
    @PostMapping
    public ResponseEntity<?> createQuestion(@RequestBody QuizQuestion question,
                                             @RequestParam Long lessonId) {
        question.setLesson(lessonContentRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found")));
        return ResponseEntity.ok(quizQuestionRepository.save(question));
    }

    // PUT update question
    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable Long id,
                                             @RequestBody QuizQuestion updatedQuestion) {
        QuizQuestion question = quizQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        question.setQuestion(updatedQuestion.getQuestion());
        question.setOptionA(updatedQuestion.getOptionA());
        question.setOptionB(updatedQuestion.getOptionB());
        question.setOptionC(updatedQuestion.getOptionC());
        question.setOptionD(updatedQuestion.getOptionD());
        question.setCorrectAnswer(updatedQuestion.getCorrectAnswer());
        question.setExplanation(updatedQuestion.getExplanation());
        question.setType(updatedQuestion.getType());
        return ResponseEntity.ok(quizQuestionRepository.save(question));
    }

    // DELETE question
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        quizQuestionRepository.deleteById(id);
        return ResponseEntity.ok("Question deleted successfully");
    }
}