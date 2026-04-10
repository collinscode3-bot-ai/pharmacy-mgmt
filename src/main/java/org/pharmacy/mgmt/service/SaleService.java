package org.pharmacy.mgmt.service;

import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.PaymentBreakdownDTO;
import org.pharmacy.mgmt.dto.SaleCreateRequest;
import org.pharmacy.mgmt.dto.SaleCreateResponse;
import org.pharmacy.mgmt.dto.SaleInvoiceDocumentDTO;
import org.pharmacy.mgmt.dto.SaleItemCreateRequest;
import org.pharmacy.mgmt.dto.SaleOrderContextItemDTO;
import org.pharmacy.mgmt.exception.SalesValidationException;
import org.pharmacy.mgmt.exception.ValidationErrorDetail;
import org.pharmacy.mgmt.model.Inventory;
import org.pharmacy.mgmt.model.Medicine;
import org.pharmacy.mgmt.model.PaymentBreakdown;
import org.pharmacy.mgmt.model.PaymentType;
import org.pharmacy.mgmt.model.Sale;
import org.pharmacy.mgmt.model.SaleItem;
import org.pharmacy.mgmt.model.User;
import org.pharmacy.mgmt.repository.InventoryRepository;
import org.pharmacy.mgmt.repository.MedicineRepository;
import org.pharmacy.mgmt.repository.PaymentBreakdownRepository;
import org.pharmacy.mgmt.repository.SaleItemRepository;
import org.pharmacy.mgmt.repository.SaleRepository;
import org.pharmacy.mgmt.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SaleService {

    private static final Logger log = LoggerFactory.getLogger(SaleService.class);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final DateTimeFormatter BILL_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int CURRENCY_SCALE = 2;

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PaymentBreakdownRepository paymentBreakdownRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final InvoicePdfService invoicePdfService;

    @Value("${sales.validation.allowed-gst-slabs:0,5,12,18,28}")
    private String allowedGstSlabsRaw = "0,5,12,18,28";

    @Value("${sales.validation.max-discount-percent:5}")
    private BigDecimal maxDiscountPercent = new BigDecimal("5");

    @Value("${sales.validation.amount-tolerance:0.01}")
    private BigDecimal amountTolerance = new BigDecimal("0.01");

    @Value("${sales.validation.default-price-includes-gst:false}")
    private boolean defaultPriceIncludesGst;

    @Transactional(readOnly = true)
    public Optional<SaleCreateResponse> getSaleByBillNo(Long billNo) {
        return saleRepository.findById(billNo).map(sale -> {
            List<SaleItem> saleItems = saleItemRepository.findBySaleBillNoOrderByBillItemIdAsc(sale.getBillNo());
            List<PaymentBreakdown> paymentRows = paymentBreakdownRepository.findBySaleBillNoOrderByPaymentIdAsc(sale.getBillNo());
            return toSaleCreateResponse(sale, saleItems, paymentRows, null);
        });
    }

    @Transactional
    public SaleCreateResponse createSale(SaleCreateRequest request) {
        String username = getAuthenticatedUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        String customerName = request.getCustomerName();
        String paymentMethod = request.getPaymentMethod();
        String customerPhone = request.getCustomerPhone();

        BigDecimal subtotalAmount = nonNullMoney(request.getSubtotalAmount());
        BigDecimal discountAmount = nonNullMoney(request.getDiscountAmount());
        BigDecimal gstAmount = nonNullMoney(request.getGstAmount());
        BigDecimal grandTotalAmount = nonNullMoney(request.getGrandTotalAmount());

        Long billNo = generateBillNo();
        Sale sale = Sale.builder()
                .billNo(billNo)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .user(user)
                .subtotalAmount(subtotalAmount)
                .discountAmount(discountAmount)
                .gstAmount(gstAmount)
                .grandTotalAmount(grandTotalAmount)
                .paymentMethod(paymentMethod)
                .createdBy(user.getUser_id())
                .isActive(true)
                .build();
        Sale savedSale = saleRepository.save(sale);

        List<SaleItem> saleItems = new ArrayList<>();
        if (request.getItems() != null) {
            for (SaleItemCreateRequest item : request.getItems()) {
                Medicine medicine = medicineRepository.findById(item.getMedicineId())
                        .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + item.getMedicineId()));

                Inventory inventory = null;
                if (item.getInventoryId() != null) {
                    inventory = inventoryRepository.findById(item.getInventoryId())
                            .orElseThrow(() -> new IllegalArgumentException("Inventory not found: " + item.getInventoryId()));
                }

                int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                BigDecimal unitPrice = nonNullMoney(item.getUnitPrice());
                BigDecimal lineAmount = item.getLineAmount() != null
                        ? nonNullMoney(item.getLineAmount())
                        : nonNullMoney(unitPrice.multiply(BigDecimal.valueOf(quantity)));

                if (inventory != null) {
                    int available = inventory.getQuantityOnHand() == null ? 0 : inventory.getQuantityOnHand();
                    inventory.setQuantityOnHand(available - quantity);
                    inventoryRepository.save(inventory);
                }

                SaleItem saleItem = SaleItem.builder()
                        .sale(savedSale)
                        .medicine(medicine)
                        .inventory(inventory)
                        .quantitySold(quantity)
                        .unitPrice(unitPrice)
                        .lineAmount(lineAmount)
                        .build();
                saleItems.add(saleItem);
            }
        }
        saleItems = saleItemRepository.saveAll(saleItems);

        List<PaymentBreakdown> paymentRows = new ArrayList<>();
        PaymentBreakdownDTO breakdown = request.getPaymentBreakdown();
        if (breakdown != null) {
            if (breakdown.getCash() != null) {
                paymentRows.add(PaymentBreakdown.builder().sale(savedSale).paymentType(PaymentType.CASH).amount(nonNullMoney(breakdown.getCash())).build());
            }
            if (breakdown.getCard() != null) {
                paymentRows.add(PaymentBreakdown.builder().sale(savedSale).paymentType(PaymentType.CARD).amount(nonNullMoney(breakdown.getCard())).build());
            }
            if (breakdown.getUpi() != null) {
                paymentRows.add(PaymentBreakdown.builder().sale(savedSale).paymentType(PaymentType.UPI).amount(nonNullMoney(breakdown.getUpi())).build());
            }
            if (breakdown.getOther() != null) {
                paymentRows.add(PaymentBreakdown.builder().sale(savedSale).paymentType(PaymentType.OTHER).amount(nonNullMoney(breakdown.getOther())).build());
            }
        }

        if (paymentRows.isEmpty()) {
            paymentRows.add(PaymentBreakdown.builder()
                    .sale(savedSale)
                    .paymentType(resolvePaymentType(paymentMethod))
                    .amount(grandTotalAmount)
                    .build());
        }

        List<PaymentBreakdown> savedPaymentRows = paymentBreakdownRepository.saveAll(paymentRows);

        InvoicePdfService.InvoicePdfResult invoicePdf = invoicePdfService.generateAndArchiveInvoice(savedSale, saleItems, savedPaymentRows);
        SaleInvoiceDocumentDTO invoiceDocument = SaleInvoiceDocumentDTO.builder()
            .fileName(invoicePdf.fileName())
            .contentType(invoicePdf.contentType())
            .base64Content(Base64.getEncoder().encodeToString(invoicePdf.bytes()))
            .archivePath(invoicePdf.archivePath())
            .build();

        return toSaleCreateResponse(savedSale, saleItems, savedPaymentRows, invoiceDocument);
    }

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return authentication.getName();
    }

    private SaleCreateResponse toSaleCreateResponse(Sale sale,
                                                    List<SaleItem> saleItems,
                                                    List<PaymentBreakdown> paymentRows,
                                                    SaleInvoiceDocumentDTO invoiceDocument) {
        Integer createdBy = sale.getCreatedBy();
        if (createdBy == null && sale.getUser() != null) {
            createdBy = sale.getUser().getUser_id();
        }

        return SaleCreateResponse.builder()
                .saleId(sale.getBillNo())
                .billNo(sale.getBillNo())
                .customerName(sale.getCustomerName())
                .paymentMethod(sale.getPaymentMethod())
                .items(mapItemsContract(saleItems))
                .subtotalAmount(sale.getSubtotalAmount())
                .discountAmount(sale.getDiscountAmount())
                .gstAmount(sale.getGstAmount())
                .grandTotalAmount(sale.getGrandTotalAmount())
                .paymentBreakdown(mapPaymentBreakdownContract(paymentRows))
                .invoiceDocument(invoiceDocument)
                .orderContext(mapOrderContextContract(saleItems))
                .customerPhone(sale.getCustomerPhone())
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .createdBy(createdBy)
                .isActive(sale.getIsActive())
                .build();
    }

    private List<SaleItemCreateRequest> mapItemsContract(List<SaleItem> saleItems) {
        return saleItems.stream()
                .map(item -> SaleItemCreateRequest.builder()
                        .medicineId(item.getMedicine() != null ? item.getMedicine().getMedicineId() : null)
                        .inventoryId(item.getInventory() != null ? item.getInventory().getInventoryId() : null)
                        .quantity(item.getQuantitySold())
                        .unitPrice(item.getUnitPrice())
                        .gstRate(item.getMedicine() != null && item.getMedicine().getTax() != null
                                ? item.getMedicine().getTax().getTaxPercentage()
                                : BigDecimal.ZERO)
                        .priceIncludesGst(defaultPriceIncludesGst)
                        .lineAmount(item.getLineAmount())
                        .build())
                .toList();
    }

    private List<SaleOrderContextItemDTO> mapOrderContextContract(List<SaleItem> saleItems) {
        return saleItems.stream()
                .map(item -> {
                    Medicine medicine = item.getMedicine();
                    return SaleOrderContextItemDTO.builder()
                            .medicineId(medicine != null ? medicine.getMedicineId() : null)
                            .inventoryId(item.getInventory() != null ? item.getInventory().getInventoryId() : null)
                            .medicineName(medicine != null ? medicine.getName() : null)
                            .dose(medicine != null ? medicine.getStrength() : null)
                            .unitPrice(item.getUnitPrice())
                            .gstRate(medicine != null && medicine.getTax() != null ? medicine.getTax().getTaxPercentage() : BigDecimal.ZERO)
                            .priceIncludesGst(defaultPriceIncludesGst)
                            .quantity(item.getQuantitySold())
                            .lineAmount(item.getLineAmount())
                            .build();
                })
                .toList();
    }

    private PaymentBreakdownDTO mapPaymentBreakdownContract(List<PaymentBreakdown> paymentRows) {
        BigDecimal cash = BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal card = BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal upi = BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal other = BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);

        for (PaymentBreakdown row : paymentRows) {
            BigDecimal amount = nonNullMoney(row.getAmount());
            if (row.getPaymentType() == null) {
                other = other.add(amount);
                continue;
            }

            switch (row.getPaymentType()) {
                case CASH -> cash = cash.add(amount);
                case CARD -> card = card.add(amount);
                case UPI -> upi = upi.add(amount);
                case OTHER -> other = other.add(amount);
            }
        }

        return PaymentBreakdownDTO.builder()
                .cash(cash)
                .card(card)
                .upi(upi)
                .other(other)
                .build();
    }

    private Inventory resolveInventory(PreparedLine line, Medicine medicine, List<ValidationErrorDetail> errors) {
        if (line.inventoryId != null) {
            Inventory inventory = inventoryRepository.findById(line.inventoryId).orElse(null);
            if (inventory == null) {
                addError(errors, line.fieldPrefix + ".inventoryId", "existing inventory id", String.valueOf(line.inventoryId), "Inventory not found");
                return null;
            }

            if (inventory.getMedicine() == null
                    || inventory.getMedicine().getMedicineId() == null
                    || !inventory.getMedicine().getMedicineId().equals(medicine.getMedicineId())) {
                addError(errors, line.fieldPrefix + ".inventoryId", "inventory belonging to medicineId " + medicine.getMedicineId(),
                        String.valueOf(line.inventoryId), "inventoryId does not belong to medicineId");
                return null;
            }
            return inventory;
        }

        List<Inventory> inventories = inventoryRepository.findByMedicineMedicineIdOrderByExpirationDateAsc(medicine.getMedicineId());
        return inventories.stream()
                .filter(i -> i.getQuantityOnHand() != null && i.getQuantityOnHand() >= line.quantity)
                .findFirst()
                .orElseGet(() -> {
                    addError(errors, line.fieldPrefix + ".inventoryId", "nullable or valid inventory with available stock", null,
                            "No eligible inventory found for requested quantity");
                    return null;
                });
    }

    private List<PaymentBreakdownDraft> buildPaymentDrafts(PaymentBreakdownDTO breakdown,
                                                            String paymentMethod,
                                                            BigDecimal grandTotalAmount,
                                                            List<ValidationErrorDetail> errors) {
        List<PaymentBreakdownDraft> rows = new ArrayList<>();

        BigDecimal cash = breakdown == null ? BigDecimal.ZERO : nonNullMoney(breakdown.getCash());
        BigDecimal card = breakdown == null ? BigDecimal.ZERO : nonNullMoney(breakdown.getCard());
        BigDecimal upi = breakdown == null ? BigDecimal.ZERO : nonNullMoney(breakdown.getUpi());
        BigDecimal other = breakdown == null ? BigDecimal.ZERO : nonNullMoney(breakdown.getOther());

        if (cash.compareTo(BigDecimal.ZERO) < 0) addError(errors, "paymentBreakdown.cash", ">= 0", String.valueOf(cash), "Cash split cannot be negative");
        if (card.compareTo(BigDecimal.ZERO) < 0) addError(errors, "paymentBreakdown.card", ">= 0", String.valueOf(card), "Card split cannot be negative");
        if (upi.compareTo(BigDecimal.ZERO) < 0) addError(errors, "paymentBreakdown.upi", ">= 0", String.valueOf(upi), "UPI split cannot be negative");
        if (other.compareTo(BigDecimal.ZERO) < 0) addError(errors, "paymentBreakdown.other", ">= 0", String.valueOf(other), "Other split cannot be negative");

        if (cash.compareTo(BigDecimal.ZERO) > 0) rows.add(new PaymentBreakdownDraft(PaymentType.CASH, cash));
        if (card.compareTo(BigDecimal.ZERO) > 0) rows.add(new PaymentBreakdownDraft(PaymentType.CARD, card));
        if (upi.compareTo(BigDecimal.ZERO) > 0) rows.add(new PaymentBreakdownDraft(PaymentType.UPI, upi));
        if (other.compareTo(BigDecimal.ZERO) > 0) rows.add(new PaymentBreakdownDraft(PaymentType.OTHER, other));

        if (rows.isEmpty()) {
            if ("SPLIT".equalsIgnoreCase(paymentMethod)) {
                addError(errors, "paymentBreakdown", "non-zero split values", null, "paymentBreakdown is required when paymentMethod is SPLIT");
            } else {
                rows.add(new PaymentBreakdownDraft(resolvePaymentType(paymentMethod), grandTotalAmount));
            }
        }

        BigDecimal total = rows.stream()
                .map(d -> d.amount)
                .reduce(BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        // if (!withinTolerance(total, grandTotalAmount)) {
        //     addError(errors, "paymentBreakdown", "sum == grandTotalAmount", total + " vs " + grandTotalAmount,
        //             "Payment split total must match grand total");
        // }

        return rows;
    }

    private PaymentType resolvePaymentType(String method) {
        if (method == null) {
            return PaymentType.OTHER;
        }
        if ("CASH".equalsIgnoreCase(method)) {
            return PaymentType.CASH;
        }
        if ("CARD".equalsIgnoreCase(method)) {
            return PaymentType.CARD;
        }
        if ("UPI".equalsIgnoreCase(method)) {
            return PaymentType.UPI;
        }
        return PaymentType.OTHER;
    }

    private boolean isAllowedPaymentMethod(String method) {
        return "CASH".equalsIgnoreCase(method)
                || "CARD".equalsIgnoreCase(method)
                || "UPI".equalsIgnoreCase(method)
                || "SPLIT".equalsIgnoreCase(method)
                || "OTHER".equalsIgnoreCase(method);
    }

    private Long generateBillNo() {
        long candidate = Long.parseLong(LocalDateTime.now().format(BILL_NO_FORMAT));
        while (saleRepository.existsById(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private void validateAmountIfPresent(String fieldName, BigDecimal requested, BigDecimal computed, List<ValidationErrorDetail> errors) {
        if (requested == null) {
            return;
        }
        BigDecimal requestedScaled = requested.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        // if (!withinTolerance(requestedScaled, computed)) {
        //     addError(errors, fieldName, String.valueOf(computed), String.valueOf(requestedScaled),
        //             fieldName + " does not match server-computed value");
        // }
    }

    private boolean withinTolerance(BigDecimal left, BigDecimal right) {
        return left.subtract(right).abs().compareTo(amountTolerance) <= 0;
    }

    private BigDecimal nonNullMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Set<BigDecimal> parseAllowedGstSlabs() {
        Set<BigDecimal> slabs = new HashSet<>();
        Arrays.stream(allowedGstSlabsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(s -> slabs.add(new BigDecimal(s).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)));
        return slabs;
    }

    private List<PreparedLine> prepareAndMergeLines(SaleCreateRequest request,
                                                    Set<BigDecimal> allowedGstSlabs,
                                                    List<ValidationErrorDetail> errors) {
        Map<LineKey, PreparedLine> merged = new HashMap<>();
        Map<String, SaleOrderContextItemDTO> orderMap = buildOrderContextMap(request.getOrderContext());

        for (int i = 0; i < request.getItems().size(); i++) {
            SaleItemCreateRequest item = request.getItems().get(i);
            String prefix = "items[" + i + "]";

            Integer medicineId = item.getMedicineId();
            if (medicineId == null || medicineId <= 0) {
                addError(errors, prefix + ".medicineId", "positive integer", String.valueOf(medicineId), "medicineId must be positive");
                continue;
            }

            Integer inventoryId = item.getInventoryId();
            if (inventoryId != null && inventoryId <= 0) {
                addError(errors, prefix + ".inventoryId", "null or positive integer", String.valueOf(inventoryId), "inventoryId must be positive when provided");
                continue;
            }

            Integer quantity = item.getQuantity();
            if (quantity == null || quantity < 1) {
                addError(errors, prefix + ".quantity", "integer >= 1", String.valueOf(quantity), "quantity must be at least 1");
                continue;
            }

            SaleOrderContextItemDTO ctx = orderMap.get(keyOf(medicineId, inventoryId));

            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : (ctx != null ? ctx.getUnitPrice() : null);
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                addError(errors, prefix + ".unitPrice", "non-negative number", String.valueOf(unitPrice), "unitPrice is required and must be non-negative");
                continue;
            }
            unitPrice = nonNullMoney(unitPrice);

            BigDecimal gstRate = item.getGstRate() != null ? item.getGstRate() : (ctx != null ? ctx.getGstRate() : null);
            if (gstRate == null) {
                addError(errors, prefix + ".gstRate", "one of configured slabs", null, "gstRate is required per line");
                continue;
            }
            gstRate = nonNullMoney(gstRate);
            if (!allowedGstSlabs.contains(gstRate)) {
                addError(errors, prefix + ".gstRate", "one of " + allowedGstSlabs, String.valueOf(gstRate), "Unsupported gstRate");
                continue;
            }

            Boolean priceIncludesGst = item.getPriceIncludesGst();
            if (priceIncludesGst == null && ctx != null) {
                priceIncludesGst = ctx.getPriceIncludesGst();
            }
            if (priceIncludesGst == null) {
                priceIncludesGst = defaultPriceIncludesGst;
                log.warn("priceIncludesGst missing at {}. Falling back to configured default: {}", prefix, defaultPriceIncludesGst);
            }

            BigDecimal clientLineAmount = item.getLineAmount() != null ? item.getLineAmount() : (ctx != null ? ctx.getLineAmount() : null);

            LineKey key = new LineKey(medicineId, inventoryId, unitPrice, gstRate, priceIncludesGst);
            PreparedLine existing = merged.get(key);
            if (existing == null) {
                PreparedLine line = new PreparedLine();
                line.fieldPrefix = prefix;
                line.medicineId = medicineId;
                line.inventoryId = inventoryId;
                line.quantity = quantity;
                line.unitPrice = unitPrice;
                line.gstRate = gstRate;
                line.priceIncludesGst = priceIncludesGst;
                line.clientLineAmount = clientLineAmount;
                merged.put(key, line);
            } else {
                existing.quantity += quantity;
                if (existing.clientLineAmount == null) {
                    existing.clientLineAmount = clientLineAmount;
                }
            }
        }

        return new ArrayList<>(merged.values());
    }

    private Map<String, SaleOrderContextItemDTO> buildOrderContextMap(List<SaleOrderContextItemDTO> orderContext) {
        Map<String, SaleOrderContextItemDTO> map = new HashMap<>();
        if (orderContext == null) {
            return map;
        }
        for (SaleOrderContextItemDTO ctx : orderContext) {
            if (ctx.getMedicineId() == null) {
                continue;
            }
            map.put(keyOf(ctx.getMedicineId(), ctx.getInventoryId()), ctx);
        }
        return map;
    }

    private String keyOf(Integer medicineId, Integer inventoryId) {
        return medicineId + "|" + (inventoryId == null ? "null" : inventoryId);
    }

    private LineMath computeLineMath(BigDecimal unitPrice, Integer quantity, BigDecimal gstRate, Boolean priceIncludesGst) {
        BigDecimal lineGross = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal gstFactor = BigDecimal.ONE.add(gstRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));

        BigDecimal taxable;
        BigDecimal gst;
        BigDecimal lineAmount;

        if (Boolean.TRUE.equals(priceIncludesGst)) {
            taxable = lineGross.divide(gstFactor, CURRENCY_SCALE, RoundingMode.HALF_UP);
            gst = lineGross.subtract(taxable).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
            lineAmount = lineGross;
        } else {
            taxable = lineGross;
            gst = taxable.multiply(gstRate).divide(ONE_HUNDRED, CURRENCY_SCALE, RoundingMode.HALF_UP);
            lineAmount = taxable.add(gst).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        }

        return new LineMath(taxable, gst, lineAmount);
    }

    private void addError(List<ValidationErrorDetail> errors, String field, String expected, String received, String message) {
        errors.add(ValidationErrorDetail.builder()
                .field(field)
                .expected(expected)
                .received(received)
                .message(message)
                .build());
    }

    private void throwValidationIfAny(List<ValidationErrorDetail> errors) {
        if (!errors.isEmpty()) {
            throw new SalesValidationException("Sales validation failed", errors);
        }
    }

    private static class PreparedLine {
        private String fieldPrefix;
        private Integer medicineId;
        private Integer inventoryId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal gstRate;
        private Boolean priceIncludesGst;
        private BigDecimal clientLineAmount;
        private BigDecimal lineTaxable;
        private BigDecimal lineGst;
        private BigDecimal lineAmount;
        private Medicine medicine;
        private Inventory inventory;
    }

    private record LineMath(BigDecimal lineTaxable, BigDecimal lineGst, BigDecimal lineAmount) {}

    private record PaymentBreakdownDraft(PaymentType paymentType, BigDecimal amount) {}

    private record LineKey(Integer medicineId, Integer inventoryId, BigDecimal unitPrice, BigDecimal gstRate,
                           Boolean priceIncludesGst) {}
}
