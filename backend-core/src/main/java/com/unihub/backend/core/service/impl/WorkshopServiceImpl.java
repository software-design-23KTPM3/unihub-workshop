package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.config.RabbitConfig;
import com.unihub.backend.core.exception.InvalidWorkshopException;
import com.unihub.backend.core.exception.WorkshopAccessDeniedException;
import com.unihub.backend.core.exception.WorkshopNotFoundException;
import com.unihub.backend.core.model.dto.WorkshopRequest;
import com.unihub.backend.core.model.dto.WorkshopResponse;
import com.unihub.backend.core.model.entity.Workshop;
import com.unihub.backend.core.model.enums.SummaryStatus;
import com.unihub.backend.core.model.enums.WorkshopStatus;
import com.unihub.backend.core.repository.WorkshopRepository;
import com.unihub.backend.core.repository.RegistrationRepository;
import com.unihub.backend.core.model.entity.Registration;
import com.unihub.backend.core.service.FileStorageService;
import com.unihub.backend.core.service.WorkshopService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkshopServiceImpl implements WorkshopService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String WORKSHOP_LIST_CACHE = "workshop_list";
    private static final String WORKSHOP_DETAILS_CACHE_PREFIX = "workshop_details:";
    private static final String WORKSHOP_SLOTS_PREFIX = "workshop_slots:";

    private final WorkshopRepository workshopRepository;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final FileStorageService fileStorageService;
    private final RegistrationRepository registrationRepository;

    public WorkshopServiceImpl(WorkshopRepository workshopRepository,
            StringRedisTemplate redisTemplate,
            RabbitTemplate rabbitTemplate,
            FileStorageService fileStorageService,
            RegistrationRepository registrationRepository) {
        this.workshopRepository = workshopRepository;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.fileStorageService = fileStorageService;
        this.registrationRepository = registrationRepository;
    }

    @Override
    public List<WorkshopResponse> getAllWorkshops(Authentication authentication, Map<String, String> filters, String studentId) {
        List<Workshop> workshops = isOrganizer(authentication) && !isAdmin(authentication)
                ? workshopRepository.findByOrganizerId(currentUserId(authentication))
                : workshopRepository.findAll();

        Map<UUID, String> registrationMap = Collections.emptyMap();
        if (studentId != null && !studentId.isEmpty()) {
            List<Registration> registrations = registrationRepository.findByStudentMssv(studentId);
            registrationMap = registrations.stream()
                    .collect(Collectors.toMap(
                            reg -> reg.getWorkshop().getId(),
                            reg -> reg.getId().toString()
                    ));
        }

        final Map<UUID, String> finalRegMap = registrationMap;
        return workshops.stream()
                .map(w -> mapToResponseWithReg(w, finalRegMap.containsKey(w.getId()), finalRegMap.get(w.getId())))
                .filter(workshop -> matchesFilters(workshop, filters))
                .collect(Collectors.toList());
    }

    @Override
    public WorkshopResponse getWorkshopById(UUID id, Authentication authentication, String studentId) {
        Workshop workshop = findWorkshop(id);
        boolean isReg = false;
        String regId = null;
        if (studentId != null && !studentId.isEmpty()) {
            var regOpt = registrationRepository.findByStudentMssvAndWorkshopId(studentId, id);
            if (regOpt.isPresent()) {
                isReg = true;
                regId = regOpt.get().getId().toString();
            }
        }
        return mapToResponseWithReg(workshop, isReg, regId);
    }

    @Override
    @Transactional
    public WorkshopResponse createWorkshop(WorkshopRequest request, MultipartFile file, Authentication authentication) {
        validateRequest(request);

        Workshop workshop = Workshop.builder()
                .name(request.getTitle().trim())
                .description(request.getDescription())
                .speaker(request.getSpeaker())
                .speakerTitle(request.getSpeakerTitle())
                .topic(request.getTopic())
                .room(request.getRoom())
                .roomMapText(request.getRoomMapText())
                .tags(serializeTags(request.getTags()))
                .organizerId(currentUserId(authentication))
                .maxSeats(request.getCapacity())
                .availableSlots(request.getCapacity())
                .startTime(toZonedDateTime(request))
                .endTime(toEndZonedDateTime(request))
                .isPaid(Boolean.TRUE.equals(request.getIsPaid()))
                .price(request.getPrice())
                .status(WorkshopStatus.ACTIVE)
                .summaryStatus(SummaryStatus.PENDING)
                .build();
        workshop.setSummaryText(request.getAiSummary());

        workshop = workshopRepository.save(workshop);
        
        if (file != null && !file.isEmpty()) {
            handlePdfUpload(workshop, file);
        }

        redisTemplate.opsForValue().set(WORKSHOP_SLOTS_PREFIX + workshop.getId(),
                String.valueOf(workshop.getAvailableSlots()));
        rabbitTemplate.convertAndSend("workshop.exchange", "workshop.created", workshop.getId().toString());
        invalidateCache(workshop.getId());

        return mapToResponse(workshop);
    }

    @Override
    @Transactional
    public WorkshopResponse updateWorkshop(UUID id, WorkshopRequest request, MultipartFile file, Authentication authentication) {
        validateRequest(request);

        Workshop workshop = findWorkshop(id);
        ensureCanManage(workshop, authentication);
        ensureMutable(workshop);

        int registeredCount = getRegisteredCount(workshop);
        if (request.getCapacity() < registeredCount) {
            throw new InvalidWorkshopException("Capacity cannot be less than registered students");
        }

        workshop.setName(request.getTitle().trim());
        workshop.setDescription(request.getDescription());
        workshop.setSpeaker(request.getSpeaker());
        workshop.setSpeakerTitle(request.getSpeakerTitle());
        workshop.setTopic(request.getTopic());
        workshop.setRoom(request.getRoom());
        workshop.setRoomMapText(request.getRoomMapText());
        workshop.setTags(serializeTags(request.getTags()));
        workshop.setMaxSeats(request.getCapacity());
        workshop.setAvailableSlots(request.getCapacity() - registeredCount);
        workshop.setStartTime(toZonedDateTime(request));
        workshop.setEndTime(toEndZonedDateTime(request));
        workshop.setIsPaid(Boolean.TRUE.equals(request.getIsPaid()));
        workshop.setPrice(request.getPrice());
        workshop.setSummaryText(request.getAiSummary());

        if (file != null && !file.isEmpty()) {
            handlePdfUpload(workshop, file);
        }

        workshop = workshopRepository.save(workshop);
        redisTemplate.opsForValue().set(WORKSHOP_SLOTS_PREFIX + workshop.getId(),
                String.valueOf(workshop.getAvailableSlots()));
        invalidateCache(workshop.getId());

        return mapToResponse(workshop);
    }

    @Override
    @Transactional
    public WorkshopResponse cancelWorkshop(UUID id, Authentication authentication) {
        Workshop workshop = findWorkshop(id);
        ensureCanManage(workshop, authentication);
        ensureMutable(workshop);

        workshop.setStatus(WorkshopStatus.CANCELLED);
        workshop.setAvailableSlots(0);
        workshop = workshopRepository.save(workshop);

        redisTemplate.delete(WORKSHOP_SLOTS_PREFIX + id);
        invalidateCache(id);

        return mapToResponse(workshop);
    }

    @Override
    @Transactional
    public void uploadPdf(UUID id, MultipartFile file) {
        Workshop workshop = findWorkshop(id);
        handlePdfUpload(workshop, file);
        workshopRepository.save(workshop);
    }

    private void handlePdfUpload(Workshop workshop, MultipartFile file) {
        String path = fileStorageService.save(file);
        workshop.setPdfUrl(path);
        workshop.setSummaryStatus(SummaryStatus.PENDING);

        UUID id = workshop.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(RabbitConfig.AI_SUMMARY_EXCHANGE, RabbitConfig.AI_SUMMARY_ROUTING_KEY,
                        id.toString());
            }
        });
    }

    private Workshop findWorkshop(UUID id) {
        return workshopRepository.findById(id)
                .orElseThrow(WorkshopNotFoundException::new);
    }

    private void validateRequest(WorkshopRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new InvalidWorkshopException("Title must not be blank");
        }
        if (request.getCapacity() == null || request.getCapacity() <= 0) {
            throw new InvalidWorkshopException("Capacity must be greater than 0");
        }
        if (request.getStartTime() == null || request.getEndTime() == null
                || !request.getStartTime().isBefore(request.getEndTime())) {
            throw new InvalidWorkshopException("Start time must be before end time");
        }
        if (request.getDate() == null) {
            throw new InvalidWorkshopException("Date is required");
        }
    }

    private void ensureMutable(Workshop workshop) {
        if (workshop.getStatus() == WorkshopStatus.CANCELLED) {
            throw new InvalidWorkshopException("Cancelled workshops cannot be updated or cancelled again");
        }
    }

    private void ensureCanManage(Workshop workshop, Authentication authentication) {
        if (isAdmin(authentication)) {
            return;
        }
        if (!isOrganizer(authentication) || !Objects.equals(workshop.getOrganizerId(), currentUserId(authentication))) {
            throw new WorkshopAccessDeniedException();
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN");
    }

    private boolean isOrganizer(Authentication authentication) {
        return hasRole(authentication, "ROLE_ORGANIZER");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private String currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new WorkshopAccessDeniedException();
        }
        return authentication.getName();
    }

    private ZonedDateTime toZonedDateTime(WorkshopRequest request) {
        return ZonedDateTime.of(LocalDateTime.of(request.getDate(), request.getStartTime()), APP_ZONE);
    }

    private ZonedDateTime toEndZonedDateTime(WorkshopRequest request) {
        return ZonedDateTime.of(LocalDateTime.of(request.getDate(), request.getEndTime()), APP_ZONE);
    }

    private void invalidateCache(UUID id) {
        redisTemplate.delete(WORKSHOP_LIST_CACHE);
        redisTemplate.delete(WORKSHOP_DETAILS_CACHE_PREFIX + id);
    }

    private int getRegisteredCount(Workshop workshop) {
        return (int) registrationRepository.countByWorkshopId(workshop.getId());
    }

    private String toContractStatus(Workshop workshop) {
        if (workshop.getStatus() == WorkshopStatus.CANCELLED) {
            return "CANCELLED";
        }
        return getRegisteredCount(workshop) >= workshop.getMaxSeats() ? "FULL" : "OPEN";
    }

    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.joining(","));
    }

    private List<String> deserializeTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean matchesFilters(WorkshopResponse workshop, Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        String keyword = normalize(firstNonBlank(filters.get("keyword"), filters.get("search")));
        if (!keyword.isEmpty()) {
            String haystack = normalize(String.join(" ",
                    nullToEmpty(workshop.getTitle()),
                    nullToEmpty(workshop.getSpeakerName()),
                    nullToEmpty(workshop.getTopic()),
                    nullToEmpty(workshop.getDescription()),
                    String.join(" ", workshop.getTags())));
            if (!haystack.contains(keyword)) {
                return false;
            }
        }
        if (!matchesNormalized(workshop.getTopic(), filters.get("topic"))) {
            return false;
        }
        if (!normalize(workshop.getRoom()).contains(normalize(filters.get("room")))) {
            return false;
        }
        if (!matchesNormalized(workshop.getStatus(), filters.get("status"))) {
            return false;
        }
        if (filters.get("date") != null && !filters.get("date").isBlank()
                && !filters.get("date").equals(workshop.getDate())) {
            return false;
        }
        if (filters.get("isPaid") != null && !filters.get("isPaid").isBlank()) {
            boolean expected = Boolean.parseBoolean(filters.get("isPaid"));
            return Objects.equals(workshop.getIsPaid(), expected);
        }
        return true;
    }

    private boolean matchesNormalized(String actual, String expected) {
        String normalizedExpected = normalize(expected);
        return normalizedExpected.isEmpty() || normalize(actual).equals(normalizedExpected);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private WorkshopResponse mapToResponse(Workshop workshop) {
        return mapToResponseWithReg(workshop, false, null);
    }

    private WorkshopResponse mapToResponseWithReg(Workshop workshop, boolean isRegistered, String registrationId) {
        ZonedDateTime startTime = workshop.getStartTime().withZoneSameInstant(APP_ZONE);
        ZonedDateTime endTime = workshop.getEndTime().withZoneSameInstant(APP_ZONE);

        return WorkshopResponse.builder()
                .id(workshop.getId())
                .title(workshop.getName())
                .description(workshop.getDescription())
                .speaker(workshop.getSpeaker())
                .speakerName(workshop.getSpeaker())
                .speakerTitle(workshop.getSpeakerTitle())
                .topic(workshop.getTopic())
                .room(workshop.getRoom())
                .roomMapText(workshop.getRoomMapText())
                .date(startTime.toLocalDate().toString())
                .startTime(startTime.toLocalTime().format(TIME_FORMATTER))
                .endTime(endTime.toLocalTime().format(TIME_FORMATTER))
                .capacity(workshop.getMaxSeats())
                .registeredCount(getRegisteredCount(workshop))
                .price(workshop.getPrice())
                .isPaid(workshop.getIsPaid())
                .status(toContractStatus(workshop))
                .tags(deserializeTags(workshop.getTags()))
                .aiSummary(workshop.getSummaryText())
                .organizerId(workshop.getOrganizerId())
                .isRegistered(isRegistered)
                .registrationId(registrationId)
                .build();
    }
}
