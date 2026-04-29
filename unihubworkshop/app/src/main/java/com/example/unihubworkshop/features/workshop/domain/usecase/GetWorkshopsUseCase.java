package com.example.unihubworkshop.features.workshop.domain.usecase;

import com.example.unihubworkshop.features.workshop.domain.entity.WorkShop;
import com.example.unihubworkshop.features.workshop.domain.repository.WorkshopRepository;
import java.util.List;

public class GetWorkshopsUseCase {
    private final WorkshopRepository repository;

    public GetWorkshopsUseCase(WorkshopRepository repository) {
        this.repository = repository;
    }

    public List<WorkShop> execute() {
        return repository.getWorkshops();
    }
}
