package com.unihub.backend.core.worker;

import com.unihub.backend.core.model.entity.Student;
import com.unihub.backend.core.model.entity.SyncLog;
import com.unihub.backend.core.model.enums.SyncStatus;
import com.unihub.backend.core.repository.StudentRepository;
import com.unihub.backend.core.repository.SyncLogRepository;
import com.unihub.backend.core.repository.WorkshopRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class IntegrationWorker {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IntegrationWorker.class);

    private final StudentRepository studentRepository;
    private final SyncLogRepository syncLogRepository;
    private final WorkshopRepository workshopRepository;

    public IntegrationWorker(StudentRepository studentRepository, 
                             SyncLogRepository syncLogRepository, 
                             WorkshopRepository workshopRepository) {
        this.studentRepository = studentRepository;
        this.syncLogRepository = syncLogRepository;
        this.workshopRepository = workshopRepository;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduleCsvSync() {
        log.info("Starting scheduled CSV Student Sync...");
        
        SyncLog syncLog = SyncLog.builder()
                .status(SyncStatus.RUNNING)
                .build();
        syncLog = syncLogRepository.save(syncLog);

        int successCount = 0;
        int errorCount = 0;
        int total = 0;

        try (BufferedReader br = new BufferedReader(new FileReader("data/students_latest.csv"))) {
            String line;
            List<Student> batch = new ArrayList<>();
            br.readLine();
            
            while ((line = br.readLine()) != null) {
                total++;
                try {
                    String[] data = line.split(",");
                    Student student = Student.builder()
                            .mssv(data[0].trim())
                            .email(data[1].trim())
                            .name(data[2].trim())
                            .status("ACTIVE")
                            .build();
                    batch.add(student);
                    
                    if (batch.size() >= 500) {
                        studentRepository.saveAll(batch);
                        successCount += batch.size();
                        batch.clear();
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.error("Error parsing CSV line: " + line, e);
                }
            }
            
            if (!batch.isEmpty()) {
                studentRepository.saveAll(batch);
                successCount += batch.size();
            }

            syncLog.setStatus(errorCount == 0 ? SyncStatus.SUCCESS : SyncStatus.PARTIAL);
        } catch (Exception e) {
            log.error("CSV Sync failed", e);
            syncLog.setStatus(SyncStatus.FAILED);
        } finally {
            syncLog.setTotalRecords(total);
            syncLog.setSuccessCount(successCount);
            syncLog.setErrorCount(errorCount);
            syncLog.setFinishedAt(ZonedDateTime.now());
            syncLogRepository.save(syncLog);
        }
    }
}
