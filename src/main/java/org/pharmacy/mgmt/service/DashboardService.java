package org.pharmacy.mgmt.service;

import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.DashboardSummaryDTO;
import org.pharmacy.mgmt.dto.ExpiringItemDTO;
import org.pharmacy.mgmt.dto.LowStockDTO;
import org.pharmacy.mgmt.dto.SalesTrendDTO;
import org.pharmacy.mgmt.dto.RecentActivityDTO;
import org.pharmacy.mgmt.repository.InventoryRepository;
import org.pharmacy.mgmt.repository.MedicineRepository;
import org.pharmacy.mgmt.repository.SaleRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;

    public DashboardSummaryDTO getDashboardSummary() {
        LocalDate ninetyDaysFromNow = LocalDate.now().plusDays(90);
        BigDecimal todaySales = saleRepository.calculateTodaySales();
        if (todaySales == null) {
            todaySales = BigDecimal.ZERO;
        }

        return DashboardSummaryDTO.builder()
                .todaySales(todaySales.setScale(2, RoundingMode.HALF_UP))
                .lowStockCount(medicineRepository.countLowStockMedicines())
                .expiringSoonCount(inventoryRepository.countExpiringItems(ninetyDaysFromNow))
                .build();
    }

        public java.util.List<SalesTrendDTO> getSalesTrendsLast7Days() {
            // initialize map of dates -> 0
            java.time.LocalDate today = LocalDate.now();
            java.util.Map<LocalDate, BigDecimal> map = new java.util.HashMap<>();
            for (int i = 6; i >= 0; i--) {
                map.put(today.minusDays(i), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }

            java.util.List<Object[]> rows = saleRepository.sumLast7DaysGroupByDate();
            for (Object[] r : rows) {
                if (r == null || r.length < 2) continue;
                java.sql.Date d = (java.sql.Date) r[0];
                BigDecimal total = (BigDecimal) r[1];
                LocalDate ld = d.toLocalDate();
                map.put(ld, total.setScale(2, RoundingMode.HALF_UP));
            }

            return map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> new SalesTrendDTO(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }

        public java.util.List<RecentActivityDTO> getRecentActivity(int limit) {
            java.util.List<org.pharmacy.mgmt.model.Sale> sales = saleRepository.findTop5ByIsActiveTrueOrderByCreatedAtDesc();
            return sales.stream()
                    .limit(limit)
                .map(s -> new RecentActivityDTO(s.getCustomerName(), s.getBillNo(), s.getGrandTotalAmount()))
                    .collect(java.util.stream.Collectors.toList());
        }

    public List<LowStockDTO> getLowStockMedicines() {
        return medicineRepository.findLowStockMedicines();
    }

    public List<ExpiringItemDTO> getExpiringItems() {
        LocalDate ninetyDaysFromNow = LocalDate.now().plusDays(90);
        return inventoryRepository.findExpiringItems(ninetyDaysFromNow);
    }
}
