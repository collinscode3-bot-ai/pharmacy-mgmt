package org.pharmacy.mgmt.service;

import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.MedicineDTO;
import org.pharmacy.mgmt.dto.MedicineResponseDTO;
import org.pharmacy.mgmt.exception.ResourceAlreadyExistsException;
import org.pharmacy.mgmt.exception.ResourceInUseException;
import org.pharmacy.mgmt.model.Medicine;
import org.pharmacy.mgmt.model.Tax;
import org.pharmacy.mgmt.repository.InventoryRepository;
import org.pharmacy.mgmt.repository.MedicineRepository;
import org.pharmacy.mgmt.repository.SaleItemRepository;
import org.pharmacy.mgmt.repository.TaxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final TaxRepository taxRepository;
    private final InventoryRepository inventoryRepository;
    private final SaleItemRepository saleItemRepository;

    public List<MedicineResponseDTO> findAll(String query) {
        List<Medicine> medicines;
        if (query != null && !query.isEmpty()) {
            medicines = medicineRepository.findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(query, query);
        } else {
            medicines = medicineRepository.findAll();
        }
        return medicines.stream().map(this::toResponseDto).collect(Collectors.toList());
    }

    public Optional<MedicineResponseDTO> findById(Integer id) {
        return medicineRepository.findById(id).map(this::toResponseDto);
    }

    @Transactional
    public MedicineResponseDTO create(MedicineDTO dto) {
        Tax tax = taxRepository.findById(dto.getTaxId())
                .orElseThrow(() -> new IllegalArgumentException("Tax not found with ID: " + dto.getTaxId()));

        if (medicineRepository.existsByNameAndStrength(dto.getName(), dto.getStrength())) {
            throw new ResourceAlreadyExistsException("Medicine with name " + dto.getName() + " and strength " + dto.getStrength() + " already exists.");
        }

        Medicine m = Medicine.builder()
                .name(dto.getName())
                .genericName(dto.getGenericName())
                .manufacturer(dto.getManufacturer())
                .strength(dto.getStrength())
                .dosageForm(dto.getDosageForm())
                .reorderLevel(dto.getReorderLevel() == null ? 10 : dto.getReorderLevel())
                .isPrescriptionRequired(dto.getIsPrescriptionRequired() != null && dto.getIsPrescriptionRequired())
                .description(dto.getDescription())
                .tax(tax)
                .build();

        Medicine saved = medicineRepository.save(m);
        return toResponseDto(saved);
    }

    @Transactional
    public Optional<MedicineResponseDTO> update(Integer id, MedicineDTO dto) {
        return medicineRepository.findById(id).map(existing -> {
            if (dto.getName() != null) existing.setName(dto.getName());
            if (dto.getGenericName() != null) existing.setGenericName(dto.getGenericName());
            if (dto.getManufacturer() != null) existing.setManufacturer(dto.getManufacturer());
            if (dto.getStrength() != null) existing.setStrength(dto.getStrength());
            if (dto.getDosageForm() != null) existing.setDosageForm(dto.getDosageForm());
            if (dto.getReorderLevel() != null) existing.setReorderLevel(dto.getReorderLevel());
            if (dto.getIsPrescriptionRequired() != null) existing.setIsPrescriptionRequired(dto.getIsPrescriptionRequired());
            if (dto.getDescription() != null) existing.setDescription(dto.getDescription());

            if (dto.getTaxId() != null) {
                Tax tax = taxRepository.findById(dto.getTaxId())
                        .orElseThrow(() -> new IllegalArgumentException("Tax not found with ID: " + dto.getTaxId()));
                existing.setTax(tax);
            }

            return toResponseDto(medicineRepository.save(existing));
        });
    }

    @Transactional
    public void delete(Integer id) {
        long inventoryCount = inventoryRepository.countByMedicineMedicineId(id);
        long saleItemCount = saleItemRepository.countByMedicineMedicineId(id);

        if (inventoryCount > 0 || saleItemCount > 0) {
            throw new ResourceInUseException("Cannot delete medicine with existing transaction history. Suggest deactivating instead.");
        }

        medicineRepository.deleteById(id);
    }

    private MedicineResponseDTO toResponseDto(Medicine m) {
        MedicineResponseDTO dto = MedicineResponseDTO.builder()
                .medicineId(m.getMedicineId())
                .name(m.getName())
                .genericName(m.getGenericName())
                .manufacturer(m.getManufacturer())
                .strength(m.getStrength())
                .dosageForm(m.getDosageForm())
                .reorderLevel(m.getReorderLevel())
                .isPrescriptionRequired(m.getIsPrescriptionRequired())
                .description(m.getDescription())
                .build();
        if (m.getTax() != null) {
            dto.setTaxId(m.getTax().getTaxId());
            dto.setTaxName(m.getTax().getTaxName());
            dto.setTaxPercentage(m.getTax().getTaxPercentage());
        }
        return dto;
    }
}
