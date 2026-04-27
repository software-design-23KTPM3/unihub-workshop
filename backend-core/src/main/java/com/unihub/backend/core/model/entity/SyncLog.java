package com.unihub.backend.core.model.entity;

import com.unihub.backend.core.model.enums.SyncStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_logs")
public class SyncLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "total_records")
    private Integer totalRecords;
    @Column(name = "success_count")
    private Integer successCount;
    @Column(name = "error_count")
    private Integer errorCount;
    @Enumerated(EnumType.STRING)
    private SyncStatus status;
    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private ZonedDateTime startedAt;
    @Column(name = "finished_at")
    private ZonedDateTime finishedAt;

    public SyncLog() {}
    public SyncLog(SyncStatus status) {
        this.status = status;
    }

    public UUID getId() { return id; }
    public Integer getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Integer totalRecords) { this.totalRecords = totalRecords; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public SyncStatus getStatus() { return status; }
    public void setStatus(SyncStatus status) { this.status = status; }
    public ZonedDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(ZonedDateTime finishedAt) { this.finishedAt = finishedAt; }

    public static SyncLogBuilder builder() { return new SyncLogBuilder(); }
    public static class SyncLogBuilder {
        private SyncStatus status;
        public SyncLogBuilder status(SyncStatus status) { this.status = status; return this; }
        public SyncLog build() { return new SyncLog(status); }
    }
}
