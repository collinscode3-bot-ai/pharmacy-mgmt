package org.pharmacy.mgmt.service;

import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.DashboardSummaryDTO;
import org.pharmacy.mgmt.dto.ExpiringItemDTO;
import org.pharmacy.mgmt.dto.LowStockDTO;
import org.pharmacy.mgmt.repository.InventoryRepository;
import org.pharmacy.mgmt.repository.MedicineRepository;
import org.pharmacy.mgmt.repository.SaleRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;

    public DashboardSummaryDTO getDashboardSummary() {
        LocalDate ninetyDaysFromNow = LocalDate.now().plusDays(90);

        return DashboardSummaryDTO.builder()
                .todaySales(saleRepository.calculateTodaySales())
                .lowStockCount(medicineRepository.countLowStockMedicines())
                .expiringSoonCount(inventoryRepository.countExpiringItems(ninetyDaysFromNow))
                .build();
    }

    public List<LowStockDTO> getLowStockMedicines() {
        return medicineRepository.findLowStockMedicines();
    }

    public List<ExpiringItemDTO> getExpiringItems() {
        LocalDate ninetyDaysFromNow = LocalDate.now().plusDays(90);
        return inventoryRepository.findExpiringItems(ninetyDaysFromNow);
    }
}
