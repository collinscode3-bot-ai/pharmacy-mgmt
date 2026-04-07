package org.pharmacy.mgmt.controller;

import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.DashboardSummaryDTO;
import org.pharmacy.mgmt.dto.ExpiringItemDTO;
import org.pharmacy.mgmt.dto.LowStockDTO;
import org.pharmacy.mgmt.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockDTO>> getLowStock() {
        return ResponseEntity.ok(dashboardService.getLowStockMedicines());
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<ExpiringItemDTO>> getExpiring() {
        return ResponseEntity.ok(dashboardService.getExpiringItems());
    }
}
