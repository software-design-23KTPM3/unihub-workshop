package com.example.unihubworkshop.features.workshop.presentation.viewmodel;

import androidx.lifecycle.LiveData;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.unihubworkshop.features.workshop.domain.entity.WorkShop;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopDetailUseCase;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopsUseCase;
import com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository;
import java.util.List;

public class WorkshopViewModel extends ViewModel {
    private final GetWorkshopsUseCase getWorkshopsUseCase;
    private final GetWorkshopDetailUseCase getWorkshopDetailUseCase;

    private final MutableLiveData<List<WorkShop>> _workshops = new MutableLiveData<>();
    public LiveData<List<WorkShop>> workshops = _workshops;

    private final androidx.lifecycle.MediatorLiveData<WorkShop> _selectedWorkshop = new androidx.lifecycle.MediatorLiveData<>();
    public LiveData<WorkShop> selectedWorkshop = _selectedWorkshop;

    private final MutableLiveData<String> _selectedWorkshopId = new MutableLiveData<>();
    private final LiveData<WorkShop> _repoWorkshop;

    private final WorkshopRepository repository;

    public WorkshopViewModel(WorkshopRepository repository, GetWorkshopsUseCase getWorkshopsUseCase, GetWorkshopDetailUseCase getWorkshopDetailUseCase) {
        this.repository = repository;
        this.getWorkshopsUseCase = getWorkshopsUseCase;
        this.getWorkshopDetailUseCase = getWorkshopDetailUseCase;
        
        _repoWorkshop = androidx.lifecycle.Transformations.switchMap(
            _selectedWorkshopId, id -> repository.getWorkshopLiveData(id)
        );
        _selectedWorkshop.addSource(_repoWorkshop, workShop -> {
            if (workShop != null) {
                _selectedWorkshop.setValue(workShop);
            }
        });
        
        loadWorkshops();
    }

    public void loadWorkshops() {
        new Thread(() -> {
            List<WorkShop> result = getWorkshopsUseCase.execute();
            _workshops.postValue(result);
        }).start();
    }

    public void selectWorkshop(String id) {
        _selectedWorkshopId.setValue(id);
        repository.syncWorkshopAndRegistrations(id);
    }

    public LiveData<Integer> getLocalAttendanceCount(String workshopId) {
        return repository.getLocalAttendanceCount(workshopId);
    }

    public boolean verifyOfflineCheckin(String qrCode, String workshopId) {
        // Runs on caller's thread, recommend running in background if needed
        return repository.verifyOfflineCheckin(qrCode, workshopId);
    }

    public void registerForWorkshop(String workshopId, java.util.function.Consumer<Boolean> callback) {
        repository.registerForWorkshop(workshopId, callback);
    }

    public void updateRegistrationStatus(String workshopId, boolean isRegistered, String registrationId) {
        WorkShop current = _selectedWorkshop.getValue();
        if (current != null && current.getId().equals(workshopId)) {
            // Prevent double-counting if we're already in the target state
            if (current.isRegistered() == isRegistered && registrationId != null && registrationId.equals(current.getRegistrationId())) {
                return;
            }

            int newCount = isRegistered ? current.getAttendanceCount() + 1 : Math.max(0, current.getAttendanceCount() - 1);
            repository.updateRegistrationStatus(workshopId, isRegistered, registrationId, newCount);
            
            // Update local object and post to trigger UI
            current.setRegistered(isRegistered);
            current.setRegistrationId(registrationId);
            current.setAttendanceCount(newCount);
            _selectedWorkshop.setValue(current);
        }
    }

    public void finalizeRegistration(String workshopId, String registrationId) {
        WorkShop current = _selectedWorkshop.getValue();
        if (current != null && current.getId().equals(workshopId)) {
            current.setRegistrationId(registrationId);
            repository.updateRegistrationStatus(workshopId, true, registrationId, current.getAttendanceCount());
            _selectedWorkshop.setValue(current);
        }
    }
}
