package com.unihub.backend.core.worker;

import com.unihub.backend.core.model.entity.Workshop;
import com.unihub.backend.core.model.enums.SummaryStatus;
import com.unihub.backend.core.repository.WorkshopRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
public class AIWorker {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AIWorker.class);

    private final WorkshopRepository workshopRepository;

    public AIWorker(WorkshopRepository workshopRepository) {
        this.workshopRepository = workshopRepository;
    }

    @RabbitListener(queues = "workshop.queue")
    public void processPdf(String workshopId) {
        log.info("AI Worker processing PDF for workshop: {}", workshopId);
        
        Workshop workshop = workshopRepository.findById(UUID.fromString(workshopId))
                .orElse(null);

        if (workshop == null || workshop.getPdfUrl() == null) {
            log.warn("Workshop or PDF path not found for ID: {}", workshopId);
            return;
        }

        workshop.setSummaryStatus(SummaryStatus.PROCESSING);
        workshopRepository.save(workshop);

        try {
            String extractedText = extractTextFromPdf("data" + workshop.getPdfUrl());
            log.info("Extracted {} characters from PDF", extractedText.length());

            String summary = mockAiSummary(extractedText);

            workshop.setSummaryText(summary);
            workshop.setSummaryStatus(SummaryStatus.COMPLETED);
            
        } catch (Exception e) {
            log.error("AI Processing failed", e);
            workshop.setSummaryStatus(SummaryStatus.FAILED);
        }

        workshopRepository.save(workshop);
    }

    private String extractTextFromPdf(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
            return "PDF is encrypted";
        }
    }

    private String mockAiSummary(String text) {
        return "AI SUMMARY: " + (text.length() > 100 ? text.substring(0, 100) + "..." : text);
    }
}
