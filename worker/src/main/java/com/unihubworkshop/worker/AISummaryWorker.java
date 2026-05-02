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

    private final WorkshopRepository workshopRepository;
    private final AISummaryService aiSummaryService;

    public AISummaryWorker(WorkshopRepository workshopRepository, AISummaryService aiSummaryService) {
        this.workshopRepository = workshopRepository;
        this.aiSummaryService = aiSummaryService;
    }

    @RabbitListener(queues = RabbitConfig.AI_SUMMARY_QUEUE)
    public void processPdf(String workshopId) throws InterruptedException {
        // 1. Làm sạch ID và tìm kiếm Workshop
        String cleanId = workshopId.replace("\"", "");
        log.info(">>> [BẮT ĐẦU] Xử lý AI cho Workshop ID: {}", cleanId);

        Workshop workshop = workshopRepository.findById(UUID.fromString(cleanId)).orElse(null);

        if (workshop == null || workshop.getPdfUrl() == null) {
            log.warn("!!! Không tìm thấy Workshop hoặc đường dẫn PDF cho ID: {}", cleanId);
            return;
        }

        // 2. Cập nhật trạng thái đang xử lý
        workshop.setSummaryStatus(SummaryStatus.PROCESSING);
        workshopRepository.save(workshop);

        try {
            // 3. Trích xuất văn bản từ PDF
            String extractedText = extractTextFromPdf(workshop.getPdfUrl());
            log.info("--- Đã trích xuất ban đầu: {} ký tự", extractedText.length());

            String cleanText = cleanText(extractedText);
            log.info("--- Sau khi làm sạch còn: {} ký tự", cleanText.length());
            String summary;

            // Đặt ngưỡng an toàn là 5000 ký tự (~1200 chữ)
            int threshold = 5000;

            // 4. Gọi AI để tóm tắt
            if (cleanText.length() <= threshold) {
                log.info("--- Gửi yêu cầu tóm tắt (Văn bản ngắn)...");
                summary = aiSummaryService.generateResponse(cleanText);

                // Tăng thời gian nghỉ lên 5s cho bản 2.0-flash
                Thread.sleep(5000);
            } else {
                log.info("--- Văn bản dài, bắt đầu chia nhỏ (mỗi phần {} ký tự)...", threshold);
                List<String> chunks = chunkText(cleanText, threshold);
                StringBuilder combinedSummary = new StringBuilder();

                for (int i = 0; i < chunks.size(); i++) {
                    log.info("--- Đang xử lý phần {}/{}", i + 1, chunks.size());
                    String chunkSummary = aiSummaryService.generateResponse(chunks.get(i));
                    combinedSummary.append(chunkSummary).append("\n");

                    // Nghỉ 10 giây giữa các phần để đảm bảo an toàn cho Quota
                    Thread.sleep(10000);
                }
                summary = combinedSummary.toString();
            }

            // 5. Lưu kết quả thành công
            workshop.setSummaryText(summary);
            workshop.setSummaryStatus(SummaryStatus.COMPLETED);
            log.info(">>> [THÀNH CÔNG] Workshop ID: {} đã hoàn thành tóm tắt.", cleanId);

        } catch (Exception e) {
            // 6. Xử lý lỗi đặc biệt: Truy tìm lỗi 429 ẩn sâu bên trong (Nested Exception)
            boolean isQuotaError = false;
            Throwable cause = e;
            while (cause != null) {
                String msg = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
                if (msg.contains("429") || msg.contains("quota")) {
                    isQuotaError = true;
                    break;
                }
                cause = cause.getCause();
            }

            if (isQuotaError) {
                log.error("!!! CHẠM NGƯỠNG QUOTA: Google đang chặn yêu cầu của bạn.");
                log.error("--- Worker sẽ ngủ đông 65 giây để hồi Quota...");

                workshop.setSummaryStatus(SummaryStatus.FAILED);
                workshopRepository.save(workshop);

                Thread.sleep(65000); // Bắt buộc ngủ 65 giây
                return; // Thoát để không lưu đè trạng thái
            } else {
                log.error("!!! LỖI XỬ LÝ AI KHÁC: ", e);
                workshop.setSummaryStatus(SummaryStatus.FAILED);
            }
        }

        // Lưu trạng thái lỗi (nếu không phải lỗi Quota)
        workshopRepository.save(workshop);
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text
                .replaceAll("(?m)^\\s*[0-9]+\\s*$", "") // Xóa số trang đứng một mình
                .replaceAll("(?i)trang\\s+[0-9]+/[0-9]+", "") // Xóa chữ "Trang X/Y"
                .replaceAll("[\\r\\n]+", " ") // Chuyển các dấu xuống dòng thành khoảng trắng
                .replaceAll("\\s{2,}", " ") // Xóa khoảng trắng dư thừa (chỉ giữ 1 dấu cách)
                .replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "") // Xóa các ký tự biểu tượng lạ
                .trim();
    }

    // Đã thêm tham số chunkSize để dùng linh hoạt với biến threshold
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
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
                return "[PDF bị mã hóa - Không thể trích xuất văn bản]";
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
                if (parentFile.exists()) return new FileInputStream(parentFile);
            }
            return new FileInputStream(file);
        }
    }
}