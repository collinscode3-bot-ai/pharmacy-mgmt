package org.pharmacy.mgmt.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.SaleCreateRequest;
import org.pharmacy.mgmt.dto.SaleCreateResponse;
import org.pharmacy.mgmt.service.SaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping("/{billNo:\\d+}")
    public ResponseEntity<SaleCreateResponse> getByBillNo(@PathVariable Long billNo) {
        return saleService.getSaleByBillNo(billNo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SaleCreateResponse> create(@Valid @RequestBody SaleCreateRequest request) {
        SaleCreateResponse created = saleService.createSale(request);
        return ResponseEntity.created(URI.create("/api/sales/" + created.getBillNo())).body(created);
    }
}
