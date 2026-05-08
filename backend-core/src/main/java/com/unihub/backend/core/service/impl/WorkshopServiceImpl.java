package com.unihub.backend.core.service.impl;

import com.unihub.backend.core.config.RabbitConfig;
import com.unihub.backend.core.exception.InvalidWorkshopException;
import com.unihub.backend.core.exception.WorkshopAccessDeniedException;
import com.unihub.backend.core.exception.WorkshopNotFoundException;
import com.unihub.backend.core.model.dto.WorkshopRequest;
import com.unihub.backend.core.model.dto.WorkshopResponse;
import com.unihub.backend.core.model.entity.Workshop;
import com.unihub.backend.core.model.enums.RegistrationStatus;
import com.unihub.backend.core.model.enums.SummaryStatus;
import com.unihub.backend.core.model.enums.WorkshopStatus;
import com.unihub.backend.core.repository.RegistrationRepository;
import com.unihub.backend.core.repository.WorkshopRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkshopServiceImpl implements WorkshopService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WorkshopServiceImpl.class);

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<RegistrationStatus> OCCUPIED_REGISTRATION_STATUSES = List.of(
            RegistrationStatus.PENDING,
            RegistrationStatus.SUCCESS,
            RegistrationStatus.CHECKED_IN);
    private static final String WORKSHOP_LIST_CACHE = "workshop_list";
    private static final String WORKSHOP_DETAILS_CACHE_PREFIX = "workshop_details:";
    private static final String WORKSHOP_SLOTS_PREFIX = "workshop_slots:";
    private static final String WORKSHOP_META_PREFIX = "workshop_meta:";

    private final WorkshopRepository workshopRepository;
    private final RegistrationRepository registrationRepository;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final FileStorageService fileStorageService;

    public WorkshopServiceImpl(WorkshopRepository workshopRepository,
            RegistrationRepository registrationRepository,
            StringRedisTemplate redisTemplate,
            RabbitTemplate rabbitTemplate,
            FileStorageService fileStorageService) {
        this.workshopRepository = workshopRepository;
        this.registrationRepository = registrationRepository;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Tự động đồng bộ số lượng slot từ DB sang Redis khi ứng dụng khởi động.
     * Hữu ích khi dữ liệu được seed trực tiếp bằng SQL.
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void initWorkshopSlotsToRedis() {
        log.info("Starting Workshop Slots synchronization to Redis...");
        workshopRepository.findAll().forEach(workshop -> {
            String key = WORKSHOP_SLOTS_PREFIX + workshop.getId();
            // Chỉ set nếu chưa có trong Redis để tránh ghi đè dữ liệu đang chạy
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.opsForValue().set(key, String.valueOf(workshop.getAvailableSlots()));
                log.info("Initialized Workshop {} slots: {}", workshop.getId(), workshop.getAvailableSlots());
            }
            syncWorkshopMetaToRedis(workshop);
        });
        log.info("Workshop Slots synchronization COMPLETED.");
    }

    @Override
    public List<WorkshopResponse> getAllWorkshops(Authentication authentication, Map<String, String> filters) {
        List<Workshop> workshops = workshopRepository.findAll();

        return workshops.stream()
                .map(this::mapToResponse)
                .filter(workshop -> matchesFilters(workshop, filters))
                .collect(Collectors.toList());
    }

    @Override
    public WorkshopResponse getWorkshopById(UUID id, Authentication authentication) {
        return mapToResponse(findWorkshop(id));
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
                .tags(normalizeTags(request.getTags()))
                .organizerId(currentUserId(authentication))
                .maxSeats(request.getCapacity())
                .availableSlots(request.getCapacity())
                .registrationStartTime(toRegistrationStartZonedDateTime(request))
                .registrationEndTime(toRegistrationEndZonedDateTime(request))
                .startTime(toZonedDateTime(request))
                .endTime(toEndZonedDateTime(request))
                .isPaid(Boolean.TRUE.equals(request.getIsPaid()))
                .price(normalizePrice(request))
                .status(WorkshopStatus.ACTIVE)
                .summaryStatus(SummaryStatus.PENDING)
                .build();
        workshop.setSummaryText(request.getAiSummary());

        workshop = workshopRepository.save(workshop);

        if (file != null && !file.isEmpty()) {
            handlePdfUpload(workshop, file);
        }

        syncWorkshopAdmissionToRedisAfterCommit(workshop);
        publishWorkshopCreatedAfterCommit(workshop.getId());
        invalidateCache(workshop.getId());

        return mapToResponse(workshop);
    }

    @Override
    @Transactional
    public WorkshopResponse updateWorkshop(UUID id, WorkshopRequest request, MultipartFile file,
            Authentication authentication) {
        validateRequest(request);

        Workshop workshop = findWorkshop(id);
        ensureCanManage(workshop, authentication);
        ensureMutable(workshop);

        int registeredCount = getOccupiedRegistrationCount(workshop.getId());
        if (request.getCapacity() < registeredCount) {
            throw new InvalidWorkshopException("Capacity cannot be less than registered students");
        }
        int capacityDelta = request.getCapacity() - workshop.getMaxSeats();

        workshop.setName(request.getTitle().trim());
        workshop.setDescription(request.getDescription());
        workshop.setSpeaker(request.getSpeaker());
        workshop.setSpeakerTitle(request.getSpeakerTitle());
        workshop.setTopic(request.getTopic());
        workshop.setRoom(request.getRoom());
        workshop.setRoomMapText(request.getRoomMapText());
        workshop.setTags(normalizeTags(request.getTags()));
        workshop.setMaxSeats(request.getCapacity());
        workshop.setAvailableSlots(request.getCapacity() - registeredCount);
        workshop.setRegistrationStartTime(toRegistrationStartZonedDateTime(request));
        workshop.setRegistrationEndTime(toRegistrationEndZonedDateTime(request));
        workshop.setStartTime(toZonedDateTime(request));
        workshop.setEndTime(toEndZonedDateTime(request));
        workshop.setIsPaid(Boolean.TRUE.equals(request.getIsPaid()));
        workshop.setPrice(normalizePrice(request));
        workshop.setSummaryText(request.getAiSummary());

        if (file != null && !file.isEmpty()) {
            handlePdfUpload(workshop, file);
        }

        workshop = workshopRepository.save(workshop);
        updateRedisSlotsForCapacityChange(workshop, capacityDelta);
        syncWorkshopMetaToRedisAfterCommit(workshop);
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
        redisTemplate.delete(WORKSHOP_META_PREFIX + id);
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
        validatePrice(request);
        if (request.getStartTime() == null || request.getEndTime() == null
                || !request.getStartTime().isBefore(request.getEndTime())) {
            throw new InvalidWorkshopException("Start time must be before end time");
        }
        if (request.getDate() == null) {
            throw new InvalidWorkshopException("Date is required");
        }
        if (request.getRegistrationStartTime() == null || request.getRegistrationEndTime() == null
                || !request.getRegistrationStartTime().isBefore(request.getRegistrationEndTime())) {
            throw new InvalidWorkshopException("Registration start time must be before registration end time");
        }
        if (toRegistrationEndZonedDateTime(request).isAfter(toZonedDateTime(request))) {
            throw new InvalidWorkshopException("Registration end time must be before or equal to workshop start time");
        }
    }

    private void ensureMutable(Workshop workshop) {
        if (workshop.getStatus() == WorkshopStatus.CANCELLED) {
            throw new InvalidWorkshopException("Cancelled workshops cannot be updated or cancelled again");
        }
    }

    private void ensureCanManage(Workshop workshop, Authentication authentication) {
        if (isAdmin(authentication) || isOrganizer(authentication)) {
            return;
        }
        throw new WorkshopAccessDeniedException();
    }

    private void validatePrice(WorkshopRequest request) {
        BigDecimal price = request.getPrice() == null ? BigDecimal.ZERO : request.getPrice();
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidWorkshopException("Price must not be negative");
        }
        if (Boolean.TRUE.equals(request.getIsPaid()) && price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidWorkshopException("Paid workshops must have a price greater than 0");
        }
    }

    private BigDecimal normalizePrice(WorkshopRequest request) {
        if (!Boolean.TRUE.equals(request.getIsPaid())) {
            return BigDecimal.ZERO;
        }
        return request.getPrice();
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

    private ZonedDateTime toRegistrationStartZonedDateTime(WorkshopRequest request) {
        return ZonedDateTime.of(request.getRegistrationStartTime(), APP_ZONE);
    }

    private ZonedDateTime toRegistrationEndZonedDateTime(WorkshopRequest request) {
        return ZonedDateTime.of(request.getRegistrationEndTime(), APP_ZONE);
    }

    private void invalidateCache(UUID id) {
        redisTemplate.delete(WORKSHOP_LIST_CACHE);
        redisTemplate.delete(WORKSHOP_DETAILS_CACHE_PREFIX + id);
    }

    private void publishWorkshopCreatedAfterCommit(UUID id) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend("workshop.exchange", "workshop.created", id.toString());
            }
        });
    }

    private void syncWorkshopAdmissionToRedisAfterCommit(Workshop workshop) {
        UUID id = workshop.getId();
        Integer availableSlots = workshop.getAvailableSlots();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisTemplate.opsForValue().set(WORKSHOP_SLOTS_PREFIX + id, String.valueOf(availableSlots));
                syncWorkshopMetaToRedis(workshop);
            }
        });
    }

    private void syncWorkshopMetaToRedisAfterCommit(Workshop workshop) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                syncWorkshopMetaToRedis(workshop);
            }
        });
    }

    private void syncWorkshopMetaToRedis(Workshop workshop) {
        String metaKey = WORKSHOP_META_PREFIX + workshop.getId();
        redisTemplate.opsForHash().put(metaKey, "status", workshop.getStatus().name());
        redisTemplate.opsForHash().put(metaKey, "registration_start_epoch",
                String.valueOf(workshop.getRegistrationStartTime().toInstant().getEpochSecond()));
        redisTemplate.opsForHash().put(metaKey, "registration_end_epoch",
                String.valueOf(workshop.getRegistrationEndTime().toInstant().getEpochSecond()));
    }

    private void updateRedisSlotsForCapacityChange(Workshop workshop, int capacityDelta) {
        String slotKey = WORKSHOP_SLOTS_PREFIX + workshop.getId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(slotKey))) {
            redisTemplate.opsForValue().increment(slotKey, capacityDelta);
            return;
        }
        redisTemplate.opsForValue().set(slotKey, String.valueOf(workshop.getAvailableSlots()));
    }

    private int getRegisteredCount(Workshop workshop) {
        if (workshop.getId() == null) {
            return 0;
        }
        return getOccupiedRegistrationCount(workshop.getId());
    }

    private int getOccupiedRegistrationCount(UUID workshopId) {
        return Math.toIntExact(registrationRepository.countByWorkshopIdAndStatusIn(
                workshopId,
                OCCUPIED_REGISTRATION_STATUSES));
    }

    private String toContractStatus(Workshop workshop) {
        if (workshop.getStatus() == WorkshopStatus.CANCELLED) {
            return "CANCELLED";
        }
        return getRegisteredCount(workshop) >= workshop.getMaxSeats() ? "FULL" : "OPEN";
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream()
                .filter(Objects::nonNull)
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
        ZonedDateTime startTime = workshop.getStartTime().withZoneSameInstant(APP_ZONE);
        ZonedDateTime endTime = workshop.getEndTime().withZoneSameInstant(APP_ZONE);
        ZonedDateTime registrationStartTime = workshop.getRegistrationStartTime().withZoneSameInstant(APP_ZONE);
        ZonedDateTime registrationEndTime = workshop.getRegistrationEndTime().withZoneSameInstant(APP_ZONE);

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
                .registrationStartTime(registrationStartTime.toLocalDateTime().toString())
                .registrationEndTime(registrationEndTime.toLocalDateTime().toString())
                .capacity(workshop.getMaxSeats())
                .registeredCount(getRegisteredCount(workshop))
                .price(workshop.getPrice())
                .isPaid(workshop.getIsPaid())
                .status(toContractStatus(workshop))
                .tags(workshop.getTags() == null ? Collections.emptyList() : workshop.getTags())
                .aiSummary(workshop.getSummaryText())
                .organizerId(workshop.getOrganizerId())
                .build();
    }
}
