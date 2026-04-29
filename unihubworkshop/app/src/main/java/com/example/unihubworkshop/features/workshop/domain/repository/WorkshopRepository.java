package com.example.unihubworkshop.features.workshop.domain.repository;

import com.example.unihubworkshop.features.workshop.domain.entity.WorkShop;
import java.util.List;

public interface WorkshopRepository {
    List<WorkShop> getWorkshops();
    WorkShop getWorkshopById(String id);
}
