package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.model.dto.WorkshopRequest;
import com.unihub.backend.core.model.dto.WorkshopResponse;
import com.unihub.backend.core.model.entity.Workshop;
import com.unihub.backend.core.model.enums.SummaryStatus;
import com.unihub.backend.core.model.enums.WorkshopStatus;
import com.unihub.backend.core.repository.WorkshopRepository;
import com.unihub.backend.core.service.FileStorageService;
import com.unihub.backend.core.service.WorkshopService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkshopServiceImpl implements WorkshopService {

    private final WorkshopRepository workshopRepository;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final FileStorageService fileStorageService;

    public WorkshopServiceImpl(WorkshopRepository workshopRepository, 
                               StringRedisTemplate redisTemplate, 
                               RabbitTemplate rabbitTemplate, 
                               FileStorageService fileStorageService) {
        this.workshopRepository = workshopRepository;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.fileStorageService = fileStorageService;
    }

    private static final String WORKSHOP_LIST_CACHE = "workshop_list";
    private static final String WORKSHOP_DETAILS_CACHE_PREFIX = "workshop_details:";
    private static final String WORKSHOP_SLOTS_PREFIX = "workshop_slots:";

    @Override
    public List<WorkshopResponse> getAllWorkshops() {
        return workshopRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WorkshopResponse getWorkshopById(UUID id) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));
        return mapToResponse(workshop);
    }

    @Override
    @Transactional
    public WorkshopResponse createWorkshop(WorkshopRequest request) {
        Workshop workshop = Workshop.builder()
                .name(request.getName())
                .speaker(request.getSpeaker())
                .room(request.getRoom())
                .maxSeats(request.getMaxSeats())
                .availableSlots(request.getMaxSeats())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isPaid(request.getIsPaid())
                .price(request.getPrice())
                .status(WorkshopStatus.ACTIVE)
                .summaryStatus(SummaryStatus.PENDING)
                .build();

        workshop = workshopRepository.save(workshop);
        redisTemplate.opsForValue().set(WORKSHOP_SLOTS_PREFIX + workshop.getId(), String.valueOf(workshop.getMaxSeats()));
        rabbitTemplate.convertAndSend("workshop.exchange", "workshop.created", workshop.getId().toString());
        invalidateCache(workshop.getId());
        return mapToResponse(workshop);
    }

    @Override
    @Transactional
    public WorkshopResponse updateWorkshop(UUID id, WorkshopRequest request) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));

        if (request.getMaxSeats() < (workshop.getMaxSeats() - workshop.getAvailableSlots())) {
            throw new RuntimeException("New max seats cannot be less than registered students");
        }

        workshop.setName(request.getName());
        workshop.setSpeaker(request.getSpeaker());
        workshop.setRoom(request.getRoom());
        workshop.setMaxSeats(request.getMaxSeats());
        workshop.setStartTime(request.getStartTime());
        workshop.setEndTime(request.getEndTime());
        workshop.setIsPaid(request.getIsPaid());
        workshop.setPrice(request.getPrice());

        workshop = workshopRepository.save(workshop);
        redisTemplate.opsForValue().set(WORKSHOP_SLOTS_PREFIX + workshop.getId(), String.valueOf(workshop.getAvailableSlots()));
        invalidateCache(workshop.getId());
        return mapToResponse(workshop);
    }

    @Override
    @Transactional
    public void cancelWorkshop(UUID id) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));

        workshop.setStatus(WorkshopStatus.CANCELLED);
        workshop.setAvailableSlots(0);
        workshopRepository.save(workshop);

        redisTemplate.delete(WORKSHOP_SLOTS_PREFIX + id);
        invalidateCache(id);
    }

    @Override
    @Transactional
    public void uploadPdf(UUID id, MultipartFile file) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));

        String path = fileStorageService.save(file);
        workshop.setPdfUrl(path);
        workshop.setSummaryStatus(SummaryStatus.PENDING);
        workshopRepository.save(workshop);

        rabbitTemplate.convertAndSend("workshop.exchange", "workshop.pdf.uploaded", id.toString());
    }

    private void invalidateCache(UUID id) {
        redisTemplate.delete(WORKSHOP_LIST_CACHE);
        redisTemplate.delete(WORKSHOP_DETAILS_CACHE_PREFIX + id);
    }

    private WorkshopResponse mapToResponse(Workshop workshop) {
        return WorkshopResponse.builder()
                .id(workshop.getId())
                .name(workshop.getName())
                .speaker(workshop.getSpeaker())
                .room(workshop.getRoom())
                .maxSeats(workshop.getMaxSeats())
                .availableSlots(workshop.getAvailableSlots())
                .startTime(workshop.getStartTime())
                .endTime(workshop.getEndTime())
                .isPaid(workshop.getIsPaid())
                .price(workshop.getPrice())
                .status(workshop.getStatus())
                .summaryText(workshop.getSummaryText())
                .summaryStatus(workshop.getSummaryStatus())
                .pdfUrl(workshop.getPdfUrl())
                .build();
    }
}
