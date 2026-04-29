package com.example.unihubworkshop.features.workshop.domain.usecase;

import com.example.unihubworkshop.features.workshop.domain.entity.WorkShop;
import com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository;

public class GetWorkshopDetailUseCase {
    private final WorkshopRepository repository;

    public GetWorkshopDetailUseCase(WorkshopRepository repository) {
        this.repository = repository;
    }

    public WorkShop execute(String id) {
        return repository.getWorkshopById(id);
    }
}
