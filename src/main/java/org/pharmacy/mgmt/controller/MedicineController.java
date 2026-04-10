package org.pharmacy.mgmt.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.MedicineCatalogResponse;
import org.pharmacy.mgmt.dto.MedicineDTO;
import org.pharmacy.mgmt.dto.MedicineResponseDTO;
import org.pharmacy.mgmt.service.MedicineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping
    public ResponseEntity<MedicineCatalogResponse> list(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "25") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "asc") String dir,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice
    ) {
        return ResponseEntity.ok(medicineService.searchMedicines(page, size, sort, dir, q, category, status, minPrice, maxPrice));
    }

    @GetMapping("/alpha")
    public ResponseEntity<java.util.List<MedicineResponseDTO>> alphabetical(
            @RequestParam(required = false, name = "startsWith") String startsWith
    ) {
        return ResponseEntity.ok(medicineService.fetchAlphabeticalOrByChar(startsWith));
    }

    @GetMapping("/cards")
    public ResponseEntity<org.pharmacy.mgmt.dto.MedicineCatalogCardsDTO> getCards() {
        return ResponseEntity.ok(medicineService.getCatalogCards());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<MedicineResponseDTO> get(@PathVariable Integer id) {
        return medicineService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MedicineResponseDTO> create(@Valid @RequestBody MedicineDTO dto) {
        MedicineResponseDTO created = medicineService.create(dto);
        return ResponseEntity.created(URI.create("/api/medicines/" + created.getMedicineId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicineResponseDTO> update(@PathVariable Integer id, @RequestBody MedicineDTO dto) {
        return medicineService.update(id, dto).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        medicineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
