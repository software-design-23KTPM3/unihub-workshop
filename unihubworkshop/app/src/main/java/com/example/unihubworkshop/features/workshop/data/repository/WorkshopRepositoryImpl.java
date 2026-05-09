package com.example.unihubworkshop.features.workshop.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.unihubworkshop.core.network.RetrofitClient;
import com.example.unihubworkshop.features.workshop.data.datasource.WorkshopApi;
import com.example.unihubworkshop.features.workshop.data.datasource.WorkshopResponseDto;
import com.example.unihubworkshop.features.workshop.domain.entity.WorkShop;
import com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class WorkshopRepositoryImpl implements WorkshopRepository {
    private static final String TAG = "WorkshopRepository";

    private final Context context;
    private final WorkshopApi workshopApi;

    public WorkshopRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        this.workshopApi = RetrofitClient.getClient(context).create(WorkshopApi.class);
    }

    @Override
    public List<WorkShop> getWorkshops() {
        try {
            List<WorkshopResponseDto> response = workshopApi.getWorkshops().execute().body();
            if (response == null) {
                return getWorkshopsLocal();
            }

            List<WorkShop> workshops = new ArrayList<>();
            com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase db = 
                com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase.getInstance(context);
            List<com.example.unihubworkshop.features.workshop.data.local.entity.WorkshopEntity> entities = new ArrayList<>();
            
            for (WorkshopResponseDto dto : response) {
                workshops.add(mapWorkshop(dto, null));
                
                com.example.unihubworkshop.features.workshop.data.local.entity.WorkshopEntity entity = new com.example.unihubworkshop.features.workshop.data.local.entity.WorkshopEntity();
                entity.id = dto.getId();
                entity.title = valueOrDefault(dto.getTitle(), "Workshop");
                entity.speaker = firstNonBlank(dto.getSpeakerName(), dto.getSpeaker(), "Chưa cập nhật");
                entity.price = dto.getPrice() == null ? 0 : dto.getPrice().intValue();
                entity.date = valueOrDefault(dto.getDate(), "");
                entity.time = joinTime(dto.getStartTime(), dto.getEndTime());
                entity.room = valueOrDefault(dto.getRoom(), "");
                entity.description = firstNonBlank(dto.getAiSummary(), dto.getDescription(), "");
                entity.roomMapText = valueOrDefault(dto.getRoomMapText(), "Chưa cập nhật sơ đồ phòng");
                entity.isOpen = "OPEN".equalsIgnoreCase(dto.getStatus());
                entity.registeredCount = dto.getRegisteredCount() == null ? 0 : dto.getRegisteredCount();
                entity.capacity = dto.getCapacity() == null ? 0 : dto.getCapacity();
                entities.add(entity);
            }
            db.workshopDao().insertAll(entities);
            return workshops;
        } catch (Exception e) {
            Log.e(TAG, "Cannot load workshops", e);
            return getWorkshopsLocal();
        }
    }

    private List<WorkShop> getWorkshopsLocal() {
        com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase db = 
            com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase.getInstance(context);
        List<com.example.unihubworkshop.features.workshop.data.local.entity.WorkshopEntity> entities = db.workshopDao().getAllWorkshops();
        List<WorkShop> workshops = new ArrayList<>();
        for (com.example.unihubworkshop.features.workshop.data.local.entity.WorkshopEntity entity : entities) {
            workshops.add(new WorkShop(
                entity.id,
                entity.title,
                entity.speaker,
                entity.price,
                entity.date,
                entity.time,
                entity.room,
                entity.description,
                entity.roomMapText,
                entity.isOpen,
                new ArrayList<>(),
                entity.registeredCount,
                entity.capacity,
                entity.roomMapText,
                findLocalRegistrationId(entity.id) != null,
                findLocalRegistrationId(entity.id)
            ));
        }
        return workshops;
    }

    @Override
    public WorkShop getWorkshopById(String id) {
        try {
            WorkshopResponseDto dto = workshopApi.getWorkshopById(id).execute().body();
            if (dto == null) {
                return getWorkshopByIdLocal(id);
            }
            return mapWorkshop(dto, findLocalRegistrationId(id));
        } catch (Exception e) {
            Log.e(TAG, "Cannot load workshop " + id, e);
            return getWorkshopByIdLocal(id);
        }
    }

    @Override
    public WorkShop getWorkshopByIdLocal(String id) {
        com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase db = 
            com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase.getInstance(context);
        com.example.unihubworkshop.features.workshop.data.local.entity.WorkshopEntity entity = db.workshopDao().getWorkshopById(id);
        if (entity != null) {
            return new WorkShop(
                entity.id,
                entity.title,
                entity.speaker,
                entity.price,
                entity.date,
                entity.time,
                entity.room,
                entity.description,
                entity.roomMapText,
                entity.isOpen,
                new ArrayList<>(),
                entity.registeredCount,
                entity.capacity,
                entity.roomMapText,
                findLocalRegistrationId(id) != null,
                findLocalRegistrationId(id)
            );
        }
        return null;
    }

    @Override
    public androidx.lifecycle.LiveData<WorkShop> getWorkshopLiveData(String id) {
        return androidx.lifecycle.Transformations.map(
            com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase.getInstance(context).workshopDao().getWorkshopLiveDataById(id),
            entity -> {
                if (entity == null) return null;
                return new WorkShop(
                    entity.id,
                    entity.title,
                    entity.speaker,
                    entity.price,
                    entity.date,
                    entity.time,
                    entity.room,
                    entity.description,
                    entity.roomMapText,
                    entity.isOpen,
                    new ArrayList<>(),
                    entity.registeredCount,
                    entity.capacity,
                    entity.roomMapText,
                    findLocalRegistrationId(entity.id) != null,
                    findLocalRegistrationId(entity.id)
                );
            }
        );
    }

    @Override
    public void syncWorkshopAndRegistrations(String id) {
        new Thread(() -> {
            try {
                WorkshopResponseDto dto = workshopApi.getWorkshopById(id).execute().body();
                if (dto != null) {
                    com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase db = 
                        com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase.getInstance(context);
                    
                    com.example.unihubworkshop.features.workshop.data.local.entity.WorkshopEntity entity = new com.example.unihubworkshop.features.workshop.data.local.entity.WorkshopEntity();
                    entity.id = dto.getId();
                    entity.title = valueOrDefault(dto.getTitle(), "Workshop");
                    entity.speaker = firstNonBlank(dto.getSpeakerName(), dto.getSpeaker(), "Chưa cập nhật");
                    entity.price = dto.getPrice() == null ? 0 : dto.getPrice().intValue();
                    entity.date = valueOrDefault(dto.getDate(), "");
                    entity.time = joinTime(dto.getStartTime(), dto.getEndTime());
                    entity.room = valueOrDefault(dto.getRoom(), "");
                    entity.description = firstNonBlank(dto.getAiSummary(), dto.getDescription(), "");
                    entity.roomMapText = valueOrDefault(dto.getRoomMapText(), "Chưa cập nhật sơ đồ phòng");
                    entity.isOpen = "OPEN".equalsIgnoreCase(dto.getStatus());
                    entity.registeredCount = dto.getRegisteredCount() == null ? 0 : dto.getRegisteredCount();
                    entity.capacity = dto.getCapacity() == null ? 0 : dto.getCapacity();
                    
                    db.workshopDao().insert(entity);
                }
                
                fetchAndCacheRegistrations(id);
            } catch (Exception e) {
                Log.e(TAG, "Cannot sync workshop " + id, e);
            }
        }).start();
    }

    @Override
    public void updateRegistrationStatus(String workshopId, boolean isRegistered, String registrationId, int attendanceCount) {
        SharedPreferences.Editor editor = context
                .getSharedPreferences("RegistrationPrefs", Context.MODE_PRIVATE)
                .edit();
        if (isRegistered && registrationId != null) {
            editor.putString(workshopId, registrationId);
        } else {
            editor.remove(workshopId);
        }
        editor.apply();
    }

    private String findLocalRegistrationId(String workshopId) {
        return context
                .getSharedPreferences("RegistrationPrefs", Context.MODE_PRIVATE)
                .getString(workshopId, null);
    }

    private WorkShop mapWorkshop(WorkshopResponseDto dto, String registrationId) {
        int price = 0;
        BigDecimal dtoPrice = dto.getPrice();
        if (dtoPrice != null) {
            price = dtoPrice.intValue();
        }

        int capacity = dto.getCapacity() == null ? 0 : dto.getCapacity();
        int registered = dto.getRegisteredCount() == null ? 0 : dto.getRegisteredCount();
        String speaker = firstNonBlank(dto.getSpeakerName(), dto.getSpeaker(), "Chưa cập nhật");
        String date = valueOrDefault(dto.getDate(), "");
        String time = joinTime(dto.getStartTime(), dto.getEndTime());
        String room = valueOrDefault(dto.getRoom(), "");
        String description = firstNonBlank(dto.getAiSummary(), dto.getDescription(), "");

        return new WorkShop(
                dto.getId(),
                valueOrDefault(dto.getTitle(), "Workshop"),
                speaker,
                price,
                date,
                time,
                room,
                description,
                room,
                "OPEN".equalsIgnoreCase(dto.getStatus()),
                dto.getTags() == null ? new ArrayList<>() : dto.getTags(),
                registered,
                capacity,
                valueOrDefault(dto.getRoomMapText(), "Chưa cập nhật sơ đồ phòng"),
                registrationId != null,
                registrationId
        );
    }

    private String joinTime(String startTime, String endTime) {
        if (isBlank(startTime) && isBlank(endTime)) {
            return "";
        }
        if (isBlank(endTime)) {
            return startTime;
        }
        if (isBlank(startTime)) {
            return endTime;
        }
        return startTime + " - " + endTime;
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (!isBlank(first)) {
            return first;
        }
        if (!isBlank(second)) {
            return second;
        }
        return fallback;
    }

    private String valueOrDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public void registerForWorkshop(String workshopId, java.util.function.Consumer<Boolean> callback) {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String studentId = prefs.getString("userMssv", "");
        
        com.example.unihubworkshop.features.workshop.data.datasource.RegistrationRequestDto request = 
                new com.example.unihubworkshop.features.workshop.data.datasource.RegistrationRequestDto(workshopId, studentId);
        String idempotencyKey = java.util.UUID.randomUUID().toString();
        
        com.example.unihubworkshop.features.workshop.data.datasource.RegistrationApi registrationApi = 
                RetrofitClient.getClient(context).create(com.example.unihubworkshop.features.workshop.data.datasource.RegistrationApi.class);
                
        registrationApi.createRegistration(idempotencyKey, request).enqueue(new retrofit2.Callback<com.example.unihubworkshop.features.workshop.data.datasource.RegistrationResponseDto>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.unihubworkshop.features.workshop.data.datasource.RegistrationResponseDto> call, retrofit2.Response<com.example.unihubworkshop.features.workshop.data.datasource.RegistrationResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Update local status immediately
                    updateRegistrationStatus(workshopId, true, response.body().getRegistrationId(), getLocalAttendanceCount(workshopId).getValue() != null ? getLocalAttendanceCount(workshopId).getValue() : 0);
                    callback.accept(true);
                } else {
                    callback.accept(false);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.unihubworkshop.features.workshop.data.datasource.RegistrationResponseDto> call, Throwable t) {
                Log.e(TAG, "Registration failed", t);
                callback.accept(false);
            }
        });
    }

    @Override
    public void fetchAndCacheRegistrations(String workshopId) {
        try {
            List<com.example.unihubworkshop.features.workshop.data.datasource.RegistrationDetailResponseDto> response = 
                RetrofitClient.getClient(context).create(com.example.unihubworkshop.features.workshop.data.datasource.RegistrationApi.class)
                .getRegistrationsByWorkshop(workshopId, "SUCCESS").execute().body();
                
            if (response != null && !response.isEmpty()) {
                List<com.example.unihubworkshop.features.workshop.data.local.entity.RegistrationEntity> entities = new ArrayList<>();
                for (com.example.unihubworkshop.features.workshop.data.datasource.RegistrationDetailResponseDto dto : response) {
                    entities.add(new com.example.unihubworkshop.features.workshop.data.local.entity.RegistrationEntity(
                            dto.getId(),
                            workshopId,
                            dto.getStudentId(),
                            dto.getStudentName(),
                            dto.getQrCode(),
                            dto.getStatus()
                    ));
                }
                com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase.getInstance(context)
                        .registrationDao().insertAll(entities);
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot fetch and cache registrations for workshop " + workshopId, e);
        }
    }

    @Override
    public androidx.lifecycle.LiveData<Integer> getLocalAttendanceCount(String workshopId) {
        return com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase.getInstance(context)
                .registrationDao().getCheckedInCount(workshopId);
    }

    @Override
    public boolean verifyOfflineCheckin(String qrCode, String workshopId) {
        com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase db = 
            com.example.unihubworkshop.features.workshop.data.local.db.AppDatabase.getInstance(context);
            
        com.example.unihubworkshop.features.workshop.data.local.entity.RegistrationEntity registration = 
            db.registrationDao().findByQrCode(qrCode, workshopId);
            
        if (registration != null && !"CHECKED_IN".equals(registration.status)) {
            // Mark as checked in locally
            db.registrationDao().markAsCheckedIn(registration.id);
            
            // Create pending checkin event
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("UTC"));
            String timestamp = now.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String staffId = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("userId", "staff");
            String deviceId = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            
            com.example.unihubworkshop.features.workshop.data.local.entity.CheckinEventEntity event = 
                new com.example.unihubworkshop.features.workshop.data.local.entity.CheckinEventEntity(
                    java.util.UUID.randomUUID().toString(),
                    registration.studentId,
                    workshopId,
                    registration.id,
                    registration.qrCode,
                    staffId,
                    deviceId,
                    timestamp,
                    System.currentTimeMillis()
                );
            db.checkinEventDao().insert(event);
            return true;
        }
        return false;
    }
}
