package org.pharmacy.mgmt.controller;

import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.MedicineDTO;
import org.pharmacy.mgmt.service.MedicineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping
    public ResponseEntity<List<MedicineDTO>> list() {
        return ResponseEntity.ok(medicineService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicineDTO> get(@PathVariable Integer id) {
        return medicineService.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MedicineDTO> create(@RequestBody MedicineDTO dto) {
        MedicineDTO created = medicineService.create(dto);
        return ResponseEntity.created(URI.create("/api/medicines/" + created.getMedicineId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicineDTO> update(@PathVariable Integer id, @RequestBody MedicineDTO dto) {
        return medicineService.update(id, dto).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        medicineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
