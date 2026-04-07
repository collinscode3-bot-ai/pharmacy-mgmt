package org.pharmacy.mgmt.service;

import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.MedicineDTO;
import org.pharmacy.mgmt.model.Medicine;
import org.pharmacy.mgmt.model.Tax;
import org.pharmacy.mgmt.repository.MedicineRepository;
import org.pharmacy.mgmt.repository.TaxRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final TaxRepository taxRepository;

    public List<MedicineDTO> findAll() {
        return medicineRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Optional<MedicineDTO> findById(Integer id) {
        return medicineRepository.findById(id).map(this::toDto);
    }

    public MedicineDTO create(MedicineDTO dto) {
        Medicine m = toEntity(dto);
        Medicine saved = medicineRepository.save(m);
        return toDto(saved);
    }

    public Optional<MedicineDTO> update(Integer id, MedicineDTO dto) {
        return medicineRepository.findById(id).map(existing -> {
            existing.setName(dto.getName());
            existing.setGenericName(dto.getGenericName());
            existing.setManufacturer(dto.getManufacturer());
            existing.setStrength(dto.getStrength());
            existing.setDosageForm(dto.getDosageForm());
            existing.setReorderLevel(dto.getReorderLevel());
            existing.setIsPrescriptionRequired(dto.getIsPrescriptionRequired());
            existing.setDescription(dto.getDescription());
            if (dto.getTaxId() != null) {
                Tax tax = taxRepository.findById(dto.getTaxId()).orElse(null);
                existing.setTax(tax);
            } else {
                existing.setTax(null);
            }
            return toDto(medicineRepository.save(existing));
        });
    }

    public void delete(Integer id) {
        medicineRepository.deleteById(id);
    }

    private MedicineDTO toDto(Medicine m) {
        MedicineDTO dto = MedicineDTO.builder()
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
        }
        return dto;
    }

    private Medicine toEntity(MedicineDTO dto) {
        Medicine m = Medicine.builder()
                .name(dto.getName())
                .genericName(dto.getGenericName())
                .manufacturer(dto.getManufacturer())
                .strength(dto.getStrength())
                .dosageForm(dto.getDosageForm())
                .reorderLevel(dto.getReorderLevel() == null ? 10 : dto.getReorderLevel())
                .isPrescriptionRequired(dto.getIsPrescriptionRequired() == null ? false : dto.getIsPrescriptionRequired())
                .description(dto.getDescription())
                .build();
        if (dto.getTaxId() != null) {
            Tax tax = taxRepository.findById(dto.getTaxId()).orElse(null);
            m.setTax(tax);
        }
        return m;
    }
}
