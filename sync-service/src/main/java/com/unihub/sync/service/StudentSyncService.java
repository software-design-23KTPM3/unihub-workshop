package com.unihub.sync.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.unihub.sync.model.entity.Student;
import com.unihub.sync.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentSyncService {

    @Value("${app.sync.csv-path}")
    private String csvPath;

    @Value("${app.sync.archive-path}")
    private String archivePath;

    private final StudentRepository studentRepository;
    private final KeycloakIntegrationService keycloakIntegrationService;

    @Scheduled(cron = "${app.sync.cron}")
    @Transactional
    public void scheduleSync() {
        log.info("Starting scheduled Student Synchronization...");
        
        File folder = new File(csvPath);
        if (!folder.exists()) {
            log.warn("CSV path does not exist: {}", csvPath);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            log.info("No CSV files found for processing.");
            return;
        }

        // Create archive folder if not exists
        new File(archivePath).mkdirs();

        for (File file : files) {
            processCsvFile(file);
            // archiveFile(file); // Comment lại để không xóa file sau khi sync (tiện cho việc test)
        }
        
        log.info("Student Synchronization completed.");
    }

    private void processCsvFile(File file) {
        log.info("Processing file: {}", file.getName());
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) return;

            // Skip header: mssv,email,name,birthday
            rows.remove(0);

            List<Student> dbBatch = new ArrayList<>();
            for (String[] data : rows) {
                if (data.length < 4) continue;

                String mssv = data[0].trim();
                String email = data[1].trim();
                String name = data[2].trim();
                String birthday = data[3].trim(); // Format: ddMMyyyy

                try {
                    // 1. Sync to UniHub DB
                    Student student = studentRepository.findById(mssv)
                            .orElse(new Student(mssv, email, name, "ACTIVE"));
                    student.setEmail(email);
                    student.setName(name);
                    student.setBirthday(birthday);
                    dbBatch.add(student);

                    if (dbBatch.size() >= 100) {
                        studentRepository.saveAll(dbBatch);
                        dbBatch.clear();
                    }

                    // 2. Sync to Keycloak
                    keycloakIntegrationService.createOrUpdateUser(mssv, email, name, birthday);

                } catch (Exception e) {
                    log.error("Error syncing student {}: {}", mssv, e.getMessage());
                }
            }
            
            if (!dbBatch.isEmpty()) {
                studentRepository.saveAll(dbBatch);
            }

        } catch (IOException | CsvException e) {
            log.error("Failed to read CSV file {}: {}", file.getName(), e.getMessage());
        }
    }

    private void archiveFile(File file) {
        try {
            Path target = Paths.get(archivePath, file.getName() + "." + System.currentTimeMillis() + ".bak");
            Files.move(file.toPath(), target);
            log.info("Archived file: {}", file.getName());
        } catch (IOException e) {
            log.error("Failed to archive file {}: {}", file.getName(), e.getMessage());
        }
    }
}
