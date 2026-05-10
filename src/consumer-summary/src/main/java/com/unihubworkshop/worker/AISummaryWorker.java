package com.unihubworkshop.worker;

import com.unihubworkshop.worker.AISummary.AISummaryService;
import com.unihubworkshop.worker.config.RabbitConfig;
import com.unihubworkshop.worker.entity.Workshop;
import com.unihubworkshop.worker.enums.SummaryStatus;
import com.unihubworkshop.worker.repo.WorkshopRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class AISummaryWorker {

    private static final Logger log = LoggerFactory.getLogger(AISummaryWorker.class);
    private static final int TEXT_THRESHOLD = 100000;

    private final WorkshopRepository workshopRepository;
    private final AISummaryService aiSummaryService;

    public AISummaryWorker(WorkshopRepository workshopRepository, AISummaryService aiSummaryService) {
        this.workshopRepository = workshopRepository;
        this.aiSummaryService = aiSummaryService;
    }

    @RabbitListener(queues = RabbitConfig.AI_SUMMARY_QUEUE)
    public void processPdf(String workshopId) throws InterruptedException {
        String cleanId = workshopId.replace("\"", "");
        log.info(">>> [START] AI Processing for Workshop: {}", cleanId);

        Workshop workshop = workshopRepository.findById(UUID.fromString(cleanId)).orElse(null);
        if (workshop == null || workshop.getPdfUrl() == null) {
            log.warn("!!! Workshop or PDF path not found for ID: {}", cleanId);
            return;
        }

        updateStatus(workshop, SummaryStatus.PROCESSING);

        try {
            String summary = generateSummaryForWorkshop(workshop);

            workshop.setSummaryText(summary);
            workshop.setSummaryStatus(SummaryStatus.COMPLETED);
            log.info("--- SUMMARY RESULT:\n{}\n---", summary);
            log.info(">>> [SUCCESS] AI Summary completed for ID: {}", cleanId);

        } catch (Exception e) {
            log.error("!!! AI PROCESSING ERROR for ID {}: ", cleanId, e);
            workshop.setSummaryStatus(SummaryStatus.FAILED);
        }

        workshopRepository.save(workshop);
    }

    private String generateSummaryForWorkshop(Workshop workshop) throws IOException, InterruptedException {
        String extractedText = extractTextFromPdf(workshop.getPdfUrl());
        String cleanText = cleanText(extractedText);
        log.info("--- Text extracted: {} chars (Cleaned: {} chars)", extractedText.length(), cleanText.length());

        if (cleanText.length() <= TEXT_THRESHOLD) {
            return aiSummaryService.generateResponse(cleanText);
        } else {
            return processChunkedText(cleanText);
        }
    }

    private String processChunkedText(String text) throws InterruptedException {
        log.info("--- Large text detected, processing in chunks...");
        List<String> chunks = chunkText(text, TEXT_THRESHOLD);
        StringBuilder combinedSummary = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            log.info("--- Processing chunk {}/{}", i + 1, chunks.size());
            combinedSummary.append(aiSummaryService.generateResponse(chunks.get(i))).append("\n");
            if (i < chunks.size() - 1)
                Thread.sleep(5000);
        }
        return combinedSummary.toString();
    }

    private void updateStatus(Workshop workshop, SummaryStatus status) {
        workshop.setSummaryStatus(status);
        workshopRepository.save(workshop);
    }

    private String cleanText(String text) {
        if (text == null)
            return "";
        return text.replaceAll("(?m)^\\s*[0-9]+\\s*$", "")
                .replaceAll("(?i)trang\\s+[0-9]+/[0-9]+", "")
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s{2,}", " ")
                .replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "")
                .trim();
    }

    private List<String> chunkText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        return chunks;
    }

    private String extractTextFromPdf(String path) throws IOException {
        try (InputStream is = getInputStream(path)) {
            byte[] bytes = is.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (!document.isEncrypted()) {
                    return new PDFTextStripper().getText(document);
                }
                return "[PDF Encrypted - Text extraction failed]";
            }
        }
    }

    private InputStream getInputStream(String path) throws IOException {
        if (path.startsWith("http")) {
            return new BufferedInputStream(URI.create(path).toURL().openStream());
        } else {
            File file = new File(path);
            if (!file.exists()) {
                File parentFile = new File("..", path);
                if (parentFile.exists())
                    return new FileInputStream(parentFile);
            }
            return new FileInputStream(file);
        }
    }
}