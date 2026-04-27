package com.unihub.backend.core.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.ZonedDateTime;

@Entity
@Table(name = "students")
public class Student {
    @Id
    private String mssv;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String name;
    private String status;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public Student() {}
    public Student(String mssv, String email, String name, String status) {
        this.mssv = mssv;
        this.email = email;
        this.name = name;
        this.status = status;
    }

    public String getMssv() { return mssv; }
    public void setMssv(String mssv) { this.mssv = mssv; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public static StudentBuilder builder() { return new StudentBuilder(); }
    public static class StudentBuilder {
        private String mssv;
        private String email;
        private String name;
        private String status;
        public StudentBuilder mssv(String mssv) { this.mssv = mssv; return this; }
        public StudentBuilder email(String email) { this.email = email; return this; }
        public StudentBuilder name(String name) { this.name = name; return this; }
        public StudentBuilder status(String status) { this.status = status; return this; }
        public Student build() { return new Student(mssv, email, name, status); }
    }
}
