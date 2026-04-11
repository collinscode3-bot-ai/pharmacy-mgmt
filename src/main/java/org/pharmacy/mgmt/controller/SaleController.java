package org.pharmacy.mgmt.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.pharmacy.mgmt.dto.SaleSearchDTO;
import java.time.LocalDate;
import java.util.Optional;

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
    public ResponseEntity<SaleCreateResponse> create(@RequestBody SaleCreateRequest request) {
        SaleCreateResponse created = saleService.createSale(request);
        return ResponseEntity.created(URI.create("/api/sales/" + created.getBillNo())).body(created);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SaleSearchDTO>> search(
            @RequestParam(required = false) Long billNo,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String billDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortObj = Sort.by(Sort.Order.desc("createdAt"));
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length == 2) {
                sortObj = Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
            } else {
                sortObj = Sort.by(sort);
            }
        }
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);

        Optional<Long> optBillNo = Optional.ofNullable(billNo);
        Optional<String> optPatient = Optional.ofNullable(patientName);
        Optional<LocalDate> optDate = Optional.empty();
        if (billDate != null && !billDate.isBlank()) {
            optDate = Optional.of(LocalDate.parse(billDate));
        }

        Page<SaleSearchDTO> result = saleService.searchSales(optBillNo, optPatient, optDate, pageable);
        return ResponseEntity.ok(result);
    }
}
