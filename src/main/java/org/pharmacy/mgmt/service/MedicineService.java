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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.pharmacy.mgmt.dto.MedicineListItemDTO;
import org.pharmacy.mgmt.dto.MedicineCatalogResponse;
import java.math.BigDecimal;

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

    public MedicineCatalogResponse searchMedicines(Integer page, Integer size, String sort, String dir,
                                                   String q, String category, String status,
                                                   BigDecimal minPrice, BigDecimal maxPrice) {
        int validatedSize = (size == null || size <= 0) ? 25 : Math.min(size, 100);
        int validatedPage = (page == null || page < 0) ? 0 : page;
        String requestedSort = (sort == null || sort.isEmpty()) ? "name" : sort;
        String sortBy;
        if ("name".equals(requestedSort) || "genericName".equals(requestedSort) || "manufacturer".equals(requestedSort)
                || "strength".equals(requestedSort) || "dosageForm".equals(requestedSort) || "reorderLevel".equals(requestedSort)
                || "productType".equals(requestedSort)) {
            sortBy = requestedSort;
        } else {
            // "price" and any unknown sort keys fallback to a stable field.
            sortBy = "name";
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(direction, sortBy));

        java.util.Set<Integer> statusFilteredIds = null;
        if (status != null && !status.isBlank()) {
            switch (status) {
                case "in_stock":
                    statusFilteredIds = new java.util.HashSet<>(inventoryRepository.findMedicineIdsInStock());
                    break;
                case "low_stock":
                    statusFilteredIds = new java.util.HashSet<>(inventoryRepository.findMedicineIdsLowStock());
                    break;
                case "out_of_stock":
                    statusFilteredIds = new java.util.HashSet<>(inventoryRepository.findMedicineIdsOutOfStock());
                    break;
                default:
                    statusFilteredIds = null;
            }

            if (statusFilteredIds != null && statusFilteredIds.isEmpty()) {
                return new MedicineCatalogResponse(java.util.Collections.emptyList(), validatedPage, validatedSize, 0, 0);
            }
        }
        final java.util.Set<Integer> finalStatusFilteredIds = statusFilteredIds;

        Specification<Medicine> spec = (root, query, cb) -> {
            query.distinct(true);
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (q != null && !q.isEmpty()) {
                String pattern = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("genericName")), pattern)
                ));
            }
            if (minPrice != null || maxPrice != null) {
                jakarta.persistence.criteria.Join<?,?> inv = root.join("inventories", jakarta.persistence.criteria.JoinType.LEFT);
                if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(inv.get("sellingPrice"), minPrice));
                if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(inv.get("sellingPrice"), maxPrice));
            }
            if (category != null && !category.isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("productType")), category.trim().toLowerCase()));
            }
            if (finalStatusFilteredIds != null) {
                predicates.add(root.get("medicineId").in(finalStatusFilteredIds));
            }
            if (!predicates.isEmpty()) {
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }
            return cb.conjunction();
        };

        Page<Medicine> resultPage = medicineRepository.findAll(spec, pageable);

        java.util.List<MedicineListItemDTO> items = resultPage.getContent().stream().map(m -> {
            Integer totalQty = inventoryRepository.totalQuantityByMedicineId(m.getMedicineId());
            java.math.BigDecimal price = inventoryRepository.minSellingPriceByMedicineId(m.getMedicineId());
            String st = "in_stock";
            int qty = (totalQty == null) ? 0 : totalQty;
            if (qty == 0) st = "out_of_stock";
            else if (qty <= (m.getReorderLevel() == null ? 0 : m.getReorderLevel())) st = "low_stock";

            MedicineListItemDTO dto = MedicineListItemDTO.builder()
                    .medicineId(m.getMedicineId())
                    .productType(m.getProductType())
                    .name(m.getName())
                    .genericName(m.getGenericName())
                    .manufacturer(m.getManufacturer())
                    .strength(m.getStrength())
                    .dosageForm(m.getDosageForm())
                    .reorderLevel(m.getReorderLevel())
                    .isPrescriptionRequired(m.getIsPrescriptionRequired())
                    .totalQuantity(qty)
                    .price(price)
                    .status(st)
                    .build();
                    return dto;
        }).collect(Collectors.toList());

        MedicineCatalogResponse resp = new MedicineCatalogResponse(items,
                resultPage.getNumber(), resultPage.getSize(), resultPage.getTotalElements(), resultPage.getTotalPages());
        return resp;
    }

    public org.pharmacy.mgmt.dto.MedicineCatalogCardsDTO getCatalogCards() {
        long totalSku = medicineRepository.count();
        Long lowStock = medicineRepository.countLowStockMedicines();
        Long outOfStock = medicineRepository.countOutOfStockMedicines();
        java.math.BigDecimal catalogValue = inventoryRepository.calculateCatalogValue();

        return new org.pharmacy.mgmt.dto.MedicineCatalogCardsDTO(
                totalSku,
                lowStock == null ? 0L : lowStock,
                outOfStock == null ? 0L : outOfStock,
                catalogValue == null ? java.math.BigDecimal.ZERO : catalogValue
        );
    }

    @Transactional
    public MedicineResponseDTO create(MedicineDTO dto) {
        Tax tax = taxRepository.findById(dto.getTaxId())
                .orElseThrow(() -> new IllegalArgumentException("Tax not found with ID: " + dto.getTaxId()));

        String normalizedProductType = normalizeProductType(dto.getProductType());
        boolean surgical = isSurgical(normalizedProductType);

        if (medicineRepository.existsByNameAndStrength(dto.getName(), dto.getStrength())) {
            throw new ResourceAlreadyExistsException("Medicine with name " + dto.getName() + " and strength " + dto.getStrength() + " already exists.");
        }

        Medicine m = Medicine.builder()
                .name(dto.getName())
            .productType(normalizedProductType)
            .genericName(surgical ? null : normalizeNullable(dto.getGenericName()))
                .manufacturer(dto.getManufacturer())
            .strength(surgical ? null : normalizeNullable(dto.getStrength()))
            .dosageForm(surgical ? null : normalizeNullable(dto.getDosageForm()))
                .reorderLevel(dto.getReorderLevel() == null ? 10 : dto.getReorderLevel())
                .isPrescriptionRequired(dto.getIsPrescriptionRequired() != null && dto.getIsPrescriptionRequired())
                .description(dto.getDescription())
                .tax(tax)
                .build();

        Medicine saved = medicineRepository.save(m);

        // If inventories provided, create Inventory records linked to the saved Medicine
        if (dto.getInventories() != null && !dto.getInventories().isEmpty()) {
            java.util.List<org.pharmacy.mgmt.model.Inventory> toSave = dto.getInventories().stream().map(invDto ->
                    org.pharmacy.mgmt.model.Inventory.builder()
                            .medicine(saved)
                            .batchNumber(invDto.getBatchNumber())
                            .expirationDate(invDto.getExpirationDate() == null ? java.time.LocalDate.now().plusYears(1) : invDto.getExpirationDate())
                            .quantityOnHand(invDto.getQuantityOnHand() == null ? 0 : invDto.getQuantityOnHand())
                            .purchasePrice(invDto.getPurchasePrice())
                            .sellingPrice(invDto.getSellingPrice())
                            .location(invDto.getLocation())
                            .build()
            ).collect(Collectors.toList());
            inventoryRepository.saveAll(toSave);
        }

        return toResponseDto(saved);
    }

    @Transactional
    public Optional<MedicineResponseDTO> update(Integer id, MedicineDTO dto) {
        return medicineRepository.findById(id).map(existing -> {
            if (dto.getName() != null) existing.setName(dto.getName());
            if (dto.getProductType() != null) existing.setProductType(normalizeProductType(dto.getProductType()));
            if (dto.getGenericName() != null) existing.setGenericName(normalizeNullable(dto.getGenericName()));
            if (dto.getManufacturer() != null) existing.setManufacturer(dto.getManufacturer());
            if (dto.getStrength() != null) existing.setStrength(normalizeNullable(dto.getStrength()));
            if (dto.getDosageForm() != null) existing.setDosageForm(normalizeNullable(dto.getDosageForm()));
            if (dto.getReorderLevel() != null) existing.setReorderLevel(dto.getReorderLevel());
            if (dto.getIsPrescriptionRequired() != null) existing.setIsPrescriptionRequired(dto.getIsPrescriptionRequired());
            if (dto.getDescription() != null) existing.setDescription(dto.getDescription());

            if (isSurgical(existing.getProductType())) {
                existing.setGenericName(null);
                existing.setStrength(null);
                existing.setDosageForm(null);
            }

            if (dto.getTaxId() != null) {
                Tax tax = taxRepository.findById(dto.getTaxId())
                        .orElseThrow(() -> new IllegalArgumentException("Tax not found with ID: " + dto.getTaxId()));
                existing.setTax(tax);
            }

            Medicine saved = medicineRepository.save(existing);

            // If inventories provided on update, update matching batch records or create new ones
            if (dto.getInventories() != null && !dto.getInventories().isEmpty()) {
                for (org.pharmacy.mgmt.dto.InventoryDTO invDto : dto.getInventories()) {
                    String batch = invDto.getBatchNumber();
                    if (batch != null && !batch.isBlank()) {
                        java.util.Optional<org.pharmacy.mgmt.model.Inventory> existingInv = inventoryRepository.findFirstByMedicineMedicineIdAndBatchNumber(saved.getMedicineId(), batch);
                        if (existingInv.isPresent()) {
                            org.pharmacy.mgmt.model.Inventory inv = existingInv.get();
                            if (invDto.getExpirationDate() != null) inv.setExpirationDate(invDto.getExpirationDate());
                            if (invDto.getQuantityOnHand() != null) inv.setQuantityOnHand(invDto.getQuantityOnHand());
                            if (invDto.getPurchasePrice() != null) inv.setPurchasePrice(invDto.getPurchasePrice());
                            if (invDto.getSellingPrice() != null) inv.setSellingPrice(invDto.getSellingPrice());
                            if (invDto.getLocation() != null) inv.setLocation(invDto.getLocation());
                            inventoryRepository.save(inv);
                            continue;
                        }
                    }

                    // create new inventory/batch if not matched
                    org.pharmacy.mgmt.model.Inventory invNew = org.pharmacy.mgmt.model.Inventory.builder()
                            .medicine(saved)
                            .batchNumber(invDto.getBatchNumber())
                            .expirationDate(invDto.getExpirationDate() == null ? java.time.LocalDate.now().plusYears(1) : invDto.getExpirationDate())
                            .quantityOnHand(invDto.getQuantityOnHand() == null ? 0 : invDto.getQuantityOnHand())
                            .purchasePrice(invDto.getPurchasePrice())
                            .sellingPrice(invDto.getSellingPrice())
                            .location(invDto.getLocation())
                            .build();
                    inventoryRepository.save(invNew);
                }
            }

            return toResponseDto(saved);
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
                .productType(m.getProductType())
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

        // Load inventories (batches) for this medicine and map to DTOs
        java.util.List<org.pharmacy.mgmt.model.Inventory> invs = inventoryRepository.findByMedicineMedicineIdOrderByExpirationDateAsc(m.getMedicineId());
        if (invs != null && !invs.isEmpty()) {
            java.util.List<org.pharmacy.mgmt.dto.InventoryDTO> invDtos = invs.stream().map(i -> org.pharmacy.mgmt.dto.InventoryDTO.builder()
                    .batchNumber(i.getBatchNumber())
                    .expirationDate(i.getExpirationDate())
                    .quantityOnHand(i.getQuantityOnHand())
                    .purchasePrice(i.getPurchasePrice())
                    .sellingPrice(i.getSellingPrice())
                    .location(i.getLocation())
                    .build()
            ).collect(Collectors.toList());
            dto.setInventories(invDtos);
        }
        return dto;
    }

    private String normalizeProductType(String productType) {
        if (productType == null || productType.isBlank()) {
            return "Medicine";
        }

        String normalized = productType.trim();
        if ("medicine".equalsIgnoreCase(normalized)) {
            return "Medicine";
        }
        if ("surgical".equalsIgnoreCase(normalized)) {
            return "Surgical";
        }
        throw new IllegalArgumentException("Unsupported productType: " + productType + ". Allowed values: Medicine, Surgical");
    }

    private boolean isSurgical(String productType) {
        return productType != null && "surgical".equalsIgnoreCase(productType);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
