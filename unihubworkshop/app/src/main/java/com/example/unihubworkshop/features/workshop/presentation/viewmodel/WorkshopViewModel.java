package com.example.unihubworkshop.features.workshop.presentation.viewmodel;

import androidx.lifecycle.LiveData;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.unihubworkshop.features.workshop.domain.entity.WorkShop;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopDetailUseCase;
import com.example.unihubworkshop.features.workshop.domain.usecase.GetWorkshopsUseCase;
import java.util.List;

public class WorkshopViewModel extends ViewModel {
    private final GetWorkshopsUseCase getWorkshopsUseCase;
    private final GetWorkshopDetailUseCase getWorkshopDetailUseCase;

    private final MutableLiveData<List<WorkShop>> _workshops = new MutableLiveData<>();
    public LiveData<List<WorkShop>> workshops = _workshops;

    private final MutableLiveData<WorkShop> _selectedWorkshop = new MutableLiveData<>();
    public LiveData<WorkShop> selectedWorkshop = _selectedWorkshop;

    public WorkshopViewModel(GetWorkshopsUseCase getWorkshopsUseCase, GetWorkshopDetailUseCase getWorkshopDetailUseCase) {
        this.getWorkshopsUseCase = getWorkshopsUseCase;
        this.getWorkshopDetailUseCase = getWorkshopDetailUseCase;
        loadWorkshops();
    }

    public void loadWorkshops() {
        _workshops.setValue(getWorkshopsUseCase.execute());
    }

    public void selectWorkshop(String id) {
        _selectedWorkshop.setValue(getWorkshopDetailUseCase.execute(id));
    }
}
