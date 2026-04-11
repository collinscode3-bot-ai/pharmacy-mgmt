package org.pharmacy.mgmt.service;

import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.AdjustedSaleItemDTO;
import org.pharmacy.mgmt.dto.ReturnProcessItemRequestDTO;
import org.pharmacy.mgmt.dto.ReturnProcessedItemDTO;
import org.pharmacy.mgmt.dto.SaleReturnProcessRequestDTO;
import org.pharmacy.mgmt.dto.SaleReturnProcessResponseDTO;
import org.pharmacy.mgmt.model.Inventory;
import org.pharmacy.mgmt.model.ReturnItem;
import org.pharmacy.mgmt.model.ReturnItemStatus;
import org.pharmacy.mgmt.model.Sale;
import org.pharmacy.mgmt.model.SaleItem;
import org.pharmacy.mgmt.model.SaleReturn;
import org.pharmacy.mgmt.model.User;
import org.pharmacy.mgmt.repository.InventoryRepository;
import org.pharmacy.mgmt.repository.ReturnItemRepository;
import org.pharmacy.mgmt.repository.SaleItemRepository;
import org.pharmacy.mgmt.repository.SaleRepository;
import org.pharmacy.mgmt.repository.SaleReturnRepository;
import org.pharmacy.mgmt.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SaleReturnService {

    private static final DateTimeFormatter RETURN_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int CURRENCY_SCALE = 2;

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public SaleReturnProcessResponseDTO processReturn(SaleReturnProcessRequestDTO request) {
        Sale sale = saleRepository.findById(request.getOriginalBillNo())
                .orElseThrow(() -> new IllegalArgumentException("Original sale not found: " + request.getOriginalBillNo()));

        List<SaleItem> saleItems = saleItemRepository.findBySaleBillNoOrderByBillItemIdAsc(sale.getBillNo());
        if (saleItems.isEmpty()) {
            throw new IllegalArgumentException("No sale items found for bill: " + request.getOriginalBillNo());
        }

        Map<Integer, SaleItem> saleItemById = new HashMap<>();
        for (SaleItem item : saleItems) {
            saleItemById.put(item.getBillItemId(), item);
        }

        Set<Integer> seenBillItemIds = new HashSet<>();
        List<PreparedReturnLine> preparedLines = new ArrayList<>();
        BigDecimal totalRefundAmount = zeroMoney();

        for (ReturnProcessItemRequestDTO reqItem : request.getItems()) {
            if (!seenBillItemIds.add(reqItem.getBillItemId())) {
                throw new IllegalArgumentException("Duplicate billItemId in request: " + reqItem.getBillItemId());
            }

            SaleItem saleItem = saleItemById.get(reqItem.getBillItemId());
            if (saleItem == null) {
                throw new IllegalArgumentException("Returned bill item not found in original sale: " + reqItem.getBillItemId());
            }

            int quantitySold = saleItem.getQuantitySold() == null ? 0 : saleItem.getQuantitySold();
            int quantityReturned = reqItem.getQuantityReturned() == null ? 0 : reqItem.getQuantityReturned();
            if (quantityReturned < 1) {
                throw new IllegalArgumentException("quantityReturned must be >= 1 for billItemId: " + reqItem.getBillItemId());
            }
            if (quantityReturned > quantitySold) {
                throw new IllegalArgumentException("quantityReturned exceeds sold quantity for billItemId: " + reqItem.getBillItemId());
            }

            BigDecimal oldLineAmount = nonNullMoney(saleItem.getLineAmount());
            BigDecimal unitRefund = quantitySold == 0
                    ? zeroMoney()
                    : oldLineAmount.divide(BigDecimal.valueOf(quantitySold), CURRENCY_SCALE, RoundingMode.HALF_UP);
            BigDecimal refundLineAmount = unitRefund.multiply(BigDecimal.valueOf(quantityReturned)).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);

            int updatedQty = quantitySold - quantityReturned;
            BigDecimal updatedLineAmount = oldLineAmount.subtract(refundLineAmount).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
            if (updatedLineAmount.compareTo(BigDecimal.ZERO) < 0) {
                updatedLineAmount = zeroMoney();
            }

            saleItem.setQuantitySold(updatedQty);
            saleItem.setLineAmount(updatedLineAmount);

            ReturnItemStatus status = reqItem.getStatus() == null ? ReturnItemStatus.RESTOCKED : reqItem.getStatus();

            preparedLines.add(new PreparedReturnLine(reqItem, saleItem, refundLineAmount, status));
            totalRefundAmount = totalRefundAmount.add(refundLineAmount).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        }

        saleItemRepository.saveAll(saleItems);

        BigDecimal oldGrandTotal = nonNullMoney(sale.getGrandTotalAmount());
        BigDecimal cappedRefund = totalRefundAmount.min(oldGrandTotal).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal newGrandTotal = oldGrandTotal.subtract(cappedRefund).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        if (newGrandTotal.compareTo(BigDecimal.ZERO) < 0) {
            newGrandTotal = zeroMoney();
        }
        sale.setGrandTotalAmount(newGrandTotal);
        saleRepository.save(sale);

        Integer createdBy = getAuthenticatedUserId();
        SaleReturn saleReturn = SaleReturn.builder()
                .returnId(generateReturnId())
                .originalSale(sale)
                .totalRefundAmount(cappedRefund)
                .returnReason(request.getReturnReason())
                .createdBy(createdBy)
                .build();
        SaleReturn savedReturn = saleReturnRepository.save(saleReturn);

        List<ReturnProcessedItemDTO> returnedItems = new ArrayList<>();
        for (PreparedReturnLine line : preparedLines) {
            Inventory reconciledInventory = reconcileInventory(line, savedReturn.getReturnId());

            ReturnItem returnItem = ReturnItem.builder()
                    .saleReturn(savedReturn)
                    .saleItem(line.saleItem)
                    .quantityReturned(line.request.getQuantityReturned())
                    .refundLineAmount(line.refundLineAmount)
                    .status(line.status)
                    .remarks(line.request.getRemarks())
                    .build();
            ReturnItem savedLine = returnItemRepository.save(returnItem);

            returnedItems.add(ReturnProcessedItemDTO.builder()
                    .returnItemId(savedLine.getReturnItemId())
                    .billItemId(line.saleItem.getBillItemId())
                    .quantityReturned(savedLine.getQuantityReturned())
                    .refundLineAmount(savedLine.getRefundLineAmount())
                    .status(savedLine.getStatus())
                    .remarks(savedLine.getRemarks())
                    .inventoryId(reconciledInventory != null ? reconciledInventory.getInventoryId() : null)
                    .batchNumber(reconciledInventory != null ? reconciledInventory.getBatchNumber() : null)
                    .build());
        }

        List<AdjustedSaleItemDTO> adjustedItems = saleItems.stream()
                .map(this::toAdjustedSaleItem)
                .toList();

        return SaleReturnProcessResponseDTO.builder()
                .returnId(savedReturn.getReturnId())
                .originalBillNo(sale.getBillNo())
                .patientName(sale.getCustomerName())
                .totalRefundAmount(savedReturn.getTotalRefundAmount())
                .updatedGrandTotalAmount(sale.getGrandTotalAmount())
                .returnReason(savedReturn.getReturnReason())
                .createdAt(savedReturn.getCreatedAt())
                .createdBy(savedReturn.getCreatedBy())
                .returnedItems(returnedItems)
                .adjustedSaleItems(adjustedItems)
                .build();
    }

    private Inventory reconcileInventory(PreparedReturnLine line, Long returnId) {
        if (line.status != ReturnItemStatus.RESTOCKED) {
            return null;
        }

        Integer medicineId = line.saleItem.getMedicine() == null ? null : line.saleItem.getMedicine().getMedicineId();
        if (medicineId == null) {
            throw new IllegalArgumentException("Sale item has no medicine mapping: " + line.saleItem.getBillItemId());
        }

        Inventory targetInventory = null;
        if (line.request.getInventoryId() != null) {
            targetInventory = inventoryRepository.findById(line.request.getInventoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Inventory not found: " + line.request.getInventoryId()));
        } else if (line.request.getBatchNumber() != null && !line.request.getBatchNumber().isBlank()) {
            targetInventory = inventoryRepository
                    .findFirstByMedicineMedicineIdAndBatchNumber(medicineId, line.request.getBatchNumber().trim())
                    .orElseGet(() -> createInventoryForRestock(line, returnId, line.request.getBatchNumber().trim()));
        } else if (line.saleItem.getInventory() != null && line.saleItem.getInventory().getInventoryId() != null) {
            targetInventory = inventoryRepository.findById(line.saleItem.getInventory().getInventoryId())
                    .orElse(line.saleItem.getInventory());
        } else {
            targetInventory = createInventoryForRestock(line, returnId, "RET-" + returnId);
        }

        if (targetInventory.getMedicine() == null
                || targetInventory.getMedicine().getMedicineId() == null
                || !targetInventory.getMedicine().getMedicineId().equals(medicineId)) {
            throw new IllegalArgumentException("Inventory medicine mismatch for billItemId: " + line.saleItem.getBillItemId());
        }

        int existingQty = targetInventory.getQuantityOnHand() == null ? 0 : targetInventory.getQuantityOnHand();
        targetInventory.setQuantityOnHand(existingQty + line.request.getQuantityReturned());
        if (targetInventory.getIsActive() == null) {
            targetInventory.setIsActive(true);
        }

        return inventoryRepository.save(targetInventory);
    }

    private Inventory createInventoryForRestock(PreparedReturnLine line, Long returnId, String batchNumber) {
        Inventory sourceInventory = line.saleItem.getInventory();
        BigDecimal unitPrice = nonNullMoney(line.saleItem.getUnitPrice());

        return Inventory.builder()
                .medicine(line.saleItem.getMedicine())
                .batchNumber(batchNumber)
                .expirationDate(sourceInventory != null && sourceInventory.getExpirationDate() != null
                        ? sourceInventory.getExpirationDate()
                        : LocalDate.now().plusYears(1))
                .quantityOnHand(0)
                .purchasePrice(sourceInventory != null && sourceInventory.getPurchasePrice() != null
                        ? sourceInventory.getPurchasePrice()
                        : unitPrice)
                .sellingPrice(sourceInventory != null && sourceInventory.getSellingPrice() != null
                        ? sourceInventory.getSellingPrice()
                        : unitPrice)
                .location(sourceInventory != null ? sourceInventory.getLocation() : "RETURNS")
                .isActive(true)
                .build();
    }

    private AdjustedSaleItemDTO toAdjustedSaleItem(SaleItem item) {
        return AdjustedSaleItemDTO.builder()
                .billItemId(item.getBillItemId())
                .medicineId(item.getMedicine() != null ? item.getMedicine().getMedicineId() : null)
                .medicineName(item.getMedicine() != null ? item.getMedicine().getName() : null)
                .quantitySold(item.getQuantitySold())
                .unitPrice(item.getUnitPrice())
                .lineAmount(item.getLineAmount())
                .build();
    }

    private Integer getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        String username = authentication.getName();
        Optional<User> user = userRepository.findByUsername(username);
        return user.map(User::getUser_id).orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private Long generateReturnId() {
        long candidate = Long.parseLong(LocalDateTime.now().format(RETURN_NO_FORMAT));
        while (saleReturnRepository.existsById(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private BigDecimal nonNullMoney(BigDecimal value) {
        if (value == null) {
            return zeroMoney();
        }
        return value.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    private static class PreparedReturnLine {
        private final ReturnProcessItemRequestDTO request;
        private final SaleItem saleItem;
        private final BigDecimal refundLineAmount;
        private final ReturnItemStatus status;

        private PreparedReturnLine(ReturnProcessItemRequestDTO request,
                                   SaleItem saleItem,
                                   BigDecimal refundLineAmount,
                                   ReturnItemStatus status) {
            this.request = request;
            this.saleItem = saleItem;
            this.refundLineAmount = refundLineAmount;
            this.status = status;
        }
    }
}
