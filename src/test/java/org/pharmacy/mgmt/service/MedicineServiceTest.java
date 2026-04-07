package org.pharmacy.mgmt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;
    @Mock
    private TaxRepository taxRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private SaleItemRepository saleItemRepository;

    @InjectMocks
    private MedicineService medicineService;

    private Medicine medicine;
    private MedicineDTO medicineDTO;
    private Tax tax;

    @BeforeEach
    void setUp() {
        tax = Tax.builder()
                .taxId(1)
                .taxName("GST 5%")
                .taxPercentage(new BigDecimal("5.00"))
                .build();

        medicine = Medicine.builder()
                .medicineId(1)
                .name("Paracetamol")
                .strength("500mg")
                .reorderLevel(10)
                .tax(tax)
                .build();

        medicineDTO = MedicineDTO.builder()
                .name("Paracetamol")
                .strength("500mg")
                .taxId(1)
                .build();
    }

    @Test
    void createMedicine_Success() {
        when(taxRepository.findById(1)).thenReturn(Optional.of(tax));
        when(medicineRepository.existsByNameAndStrength("Paracetamol", "500mg")).thenReturn(false);
        when(medicineRepository.save(any(Medicine.class))).thenReturn(medicine);

        MedicineResponseDTO result = medicineService.create(medicineDTO);

        assertNotNull(result);
        assertEquals("Paracetamol", result.getName());
        assertEquals("GST 5%", result.getTaxName());
        verify(medicineRepository).save(any(Medicine.class));
    }

    @Test
    void createMedicine_AlreadyExists() {
        when(taxRepository.findById(1)).thenReturn(Optional.of(tax));
        when(medicineRepository.existsByNameAndStrength("Paracetamol", "500mg")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> medicineService.create(medicineDTO));
    }

    @Test
    void deleteMedicine_InUse_ThrowsException() {
        when(inventoryRepository.countByMedicineMedicineId(1)).thenReturn(1L);

        ResourceInUseException ex = assertThrows(ResourceInUseException.class, () -> medicineService.delete(1));
        assertEquals("Cannot delete medicine with existing transaction history. Suggest deactivating instead.", ex.getMessage());
    }

    @Test
    void deleteMedicine_NotInUse_Success() {
        when(inventoryRepository.countByMedicineMedicineId(1)).thenReturn(0L);
        when(saleItemRepository.countByMedicineMedicineId(1)).thenReturn(0L);

        medicineService.delete(1);

        verify(medicineRepository).deleteById(1);
    }
}
