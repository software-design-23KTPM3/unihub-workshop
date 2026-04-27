package com.unihub.backend.core.repository;

import com.unihub.backend.core.model.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, String> {
}
