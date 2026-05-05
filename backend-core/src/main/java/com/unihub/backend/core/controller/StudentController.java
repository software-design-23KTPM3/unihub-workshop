package com.unihub.backend.core.controller;

import com.unihub.backend.core.model.dto.FcmTokenRequest;
import com.unihub.backend.core.model.entity.Student;
import com.unihub.backend.core.repository.StudentRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentRepository studentRepository;

    public StudentController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @PostMapping("/fcm-token")
    public void updateFcmToken(@RequestBody FcmTokenRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        student.setFcmToken(request.getToken());
        studentRepository.save(student);
    }
}
