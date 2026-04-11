package org.pharmacy.mgmt.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.SaleReturnProcessRequestDTO;
import org.pharmacy.mgmt.dto.SaleReturnProcessResponseDTO;
import org.pharmacy.mgmt.service.SaleReturnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class SaleReturnController {

    private final SaleReturnService saleReturnService;

    @PostMapping
    public ResponseEntity<SaleReturnProcessResponseDTO> processReturn(@Valid @RequestBody SaleReturnProcessRequestDTO request) {
        SaleReturnProcessResponseDTO response = saleReturnService.processReturn(request);
        return ResponseEntity.created(URI.create("/api/returns/" + response.getReturnId())).body(response);
    }
}
