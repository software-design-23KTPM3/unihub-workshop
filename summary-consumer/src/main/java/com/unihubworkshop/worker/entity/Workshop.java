package com.unihubworkshop.worker.entity;

import com.unihubworkshop.worker.enums.SummaryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "workshops")
public class Workshop {
    @Id
    private UUID id;

    @Column(name = "summary_text")
    private String summaryText;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_status", columnDefinition = "summary_status")
    private SummaryStatus summaryStatus;

    @Column(name = "pdf_url")
    private String pdfUrl;

    public UUID getId() {
        return id;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public SummaryStatus getSummaryStatus() {
        return summaryStatus;
    }

    public void setSummaryStatus(SummaryStatus summaryStatus) {
        this.summaryStatus = summaryStatus;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }
}
