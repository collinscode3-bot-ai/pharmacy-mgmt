package org.pharmacy.mgmt.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pharmacy.mgmt.dto.PaymentBreakdownDTO;
import org.pharmacy.mgmt.dto.SaleCreateRequest;
import org.pharmacy.mgmt.dto.SaleCreateResponse;
import org.pharmacy.mgmt.dto.SaleItemCreateRequest;
import org.pharmacy.mgmt.model.Inventory;
import org.pharmacy.mgmt.model.Medicine;
import org.pharmacy.mgmt.model.PaymentBreakdown;
import org.pharmacy.mgmt.model.PaymentType;
import org.pharmacy.mgmt.model.Sale;
import org.pharmacy.mgmt.model.SaleItem;
import org.pharmacy.mgmt.model.Tax;
import org.pharmacy.mgmt.model.User;
import org.pharmacy.mgmt.repository.InventoryRepository;
import org.pharmacy.mgmt.repository.MedicineRepository;
import org.pharmacy.mgmt.repository.PaymentBreakdownRepository;
import org.pharmacy.mgmt.repository.SaleItemRepository;
import org.pharmacy.mgmt.repository.SaleRepository;
import org.pharmacy.mgmt.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private SaleItemRepository saleItemRepository;
    @Mock
    private PaymentBreakdownRepository paymentBreakdownRepository;
    @Mock
    private MedicineRepository medicineRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InvoicePdfService invoicePdfService;

    @InjectMocks
    private SaleService saleService;

    @BeforeEach
    void setUpSecurity() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("pharmacist@demo.com", null));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSale_Success() {
        User user = User.builder().user_id(7).username("pharmacist@demo.com").build();
        Tax tax = Tax.builder().taxId(1).taxPercentage(new BigDecimal("5.00")).build();
        Medicine medicine = Medicine.builder().medicineId(1).name("Paracetamol").tax(tax).build();
        Inventory inventory = Inventory.builder()
                .inventoryId(10)
                .medicine(medicine)
                .batchNumber("B001")
                .expirationDate(LocalDate.of(2027, 1, 1))
                .quantityOnHand(10)
                .sellingPrice(new BigDecimal("100.00"))
                .location("A1")
                .build();

        SaleCreateRequest request = SaleCreateRequest.builder()
                .customerName("John Doe")
                .paymentMethod("Card")
                .subtotalAmount(new BigDecimal("200.00"))
                .discountAmount(new BigDecimal("0.00"))
                .gstAmount(new BigDecimal("10.00"))
                .grandTotalAmount(new BigDecimal("210.00"))
                .items(List.of(SaleItemCreateRequest.builder()
                        .medicineId(1)
                        .inventoryId(10)
                        .quantity(2)
                        .unitPrice(new BigDecimal("100.00"))
                        .gstRate(new BigDecimal("5.00"))
                        .priceIncludesGst(false)
                        .lineAmount(new BigDecimal("210.00"))
                        .build()))
                .build();

        when(saleRepository.existsById(anyLong())).thenReturn(false);
        when(userRepository.findByUsername("pharmacist@demo.com")).thenReturn(Optional.of(user));
        when(medicineRepository.findById(1)).thenReturn(Optional.of(medicine));
        when(inventoryRepository.findById(10)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(saleItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentBreakdownRepository.saveAll(any())).thenAnswer(invocation -> {
            List<PaymentBreakdown> rows = invocation.getArgument(0);
            if (!rows.isEmpty()) {
                rows.get(0).setPaymentId(1);
            }
            return rows;
        });
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale s = invocation.getArgument(0);
            if (s.getCreatedAt() == null) {
                s.setCreatedAt(LocalDateTime.of(2026, 4, 10, 10, 0));
            }
            return s;
        });
        when(invoicePdfService.generateAndArchiveInvoice(any(Sale.class), any(), any()))
            .thenReturn(new InvoicePdfService.InvoicePdfResult(
                "John_Doe_9999999999_20260410100000.pdf",
                "application/pdf",
                "pdf".getBytes(StandardCharsets.UTF_8),
                "logs/invoices/2026-04-10/John_Doe_9999999999_20260410100000.pdf"
            ));

        SaleCreateResponse response = saleService.createSale(request);

        assertNotNull(response);
        assertNotNull(response.getBillNo());
        assertEquals(new BigDecimal("200.00"), response.getSubtotalAmount());
        assertEquals(new BigDecimal("10.00"), response.getGstAmount());
        assertEquals(new BigDecimal("0.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("210.00"), response.getGrandTotalAmount());
        assertEquals(1, response.getItems().size());
        assertEquals(8, inventory.getQuantityOnHand());
        assertNotNull(response.getPaymentBreakdown());
        assertEquals(new BigDecimal("210.00"), response.getPaymentBreakdown().getCard());
        assertNotNull(response.getInvoiceDocument());
        assertEquals("application/pdf", response.getInvoiceDocument().getContentType());
        assertEquals(10, response.getItems().get(0).getInventoryId());
        verify(saleItemRepository).saveAll(any());
        verify(paymentBreakdownRepository).saveAll(any());
    }

    @Test
    void createSale_InsufficientInventory_SavesAsIs() {
        User user = User.builder().user_id(7).username("pharmacist@demo.com").build();
        Medicine medicine = Medicine.builder().medicineId(1).name("Paracetamol").build();
        Inventory inventory = Inventory.builder().inventoryId(10).medicine(medicine).quantityOnHand(1).build();

        SaleCreateRequest request = SaleCreateRequest.builder()
            .customerName("John Doe")
            .paymentMethod("Card")
            .items(List.of(SaleItemCreateRequest.builder()
                .medicineId(1)
                .inventoryId(10)
                .quantity(2)
                .unitPrice(new BigDecimal("100.00"))
                .gstRate(new BigDecimal("5.00"))
                .priceIncludesGst(false)
                .build()))
                .build();

        when(userRepository.findByUsername("pharmacist@demo.com")).thenReturn(Optional.of(user));
        when(medicineRepository.findById(1)).thenReturn(Optional.of(medicine));
        when(inventoryRepository.findById(10)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(saleRepository.existsById(anyLong())).thenReturn(false);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(saleItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentBreakdownRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoicePdfService.generateAndArchiveInvoice(any(Sale.class), any(), any()))
            .thenReturn(new InvoicePdfService.InvoicePdfResult(
                "John_Doe_9999999999_20260410100000.pdf",
                "application/pdf",
                "pdf".getBytes(StandardCharsets.UTF_8),
                "logs/invoices/2026-04-10/John_Doe_9999999999_20260410100000.pdf"
            ));

        SaleCreateResponse response = saleService.createSale(request);
        assertNotNull(response);
        assertEquals(-1, inventory.getQuantityOnHand());
    }

    @Test
    void getSaleByBillNo_Success() {
        User user = User.builder().user_id(7).username("pharmacist@demo.com").build();
        Tax tax = Tax.builder().taxId(1).taxPercentage(new BigDecimal("5.00")).build();
        Medicine medicine = Medicine.builder().medicineId(1).name("Paracetamol").tax(tax).build();
        Inventory inventory = Inventory.builder()
            .inventoryId(10)
            .medicine(medicine)
            .batchNumber("B001")
            .expirationDate(LocalDate.of(2027, 1, 1))
            .quantityOnHand(15)
            .location("A1")
            .build();
        Sale sale = Sale.builder()
            .billNo(200L)
            .createdAt(LocalDateTime.of(2026, 4, 10, 14, 0))
            .subtotalAmount(new BigDecimal("200.00"))
            .discountAmount(new BigDecimal("0.00"))
            .gstAmount(new BigDecimal("10.00"))
            .grandTotalAmount(new BigDecimal("210.00"))
                .customerName("John Doe")
                .paymentMethod("Card")
                .user(user)
                .build();
        SaleItem saleItem = SaleItem.builder()
            .billItemId(1)
                .sale(sale)
                .medicine(medicine)
            .inventory(inventory)
            .quantitySold(2)
                .unitPrice(new BigDecimal("100.00"))
            .lineAmount(new BigDecimal("210.00"))
                .build();
        PaymentBreakdown payment = PaymentBreakdown.builder()
            .paymentId(1)
            .sale(sale)
            .paymentType(PaymentType.CARD)
            .amount(new BigDecimal("210.00"))
                .build();

        when(saleRepository.findById(200L)).thenReturn(Optional.of(sale));
        when(saleItemRepository.findBySaleBillNoOrderByBillItemIdAsc(200L)).thenReturn(List.of(saleItem));
        when(paymentBreakdownRepository.findBySaleBillNoOrderByPaymentIdAsc(200L)).thenReturn(List.of(payment));

        Optional<SaleCreateResponse> responseOpt = saleService.getSaleByBillNo(200L);

        assertTrue(responseOpt.isPresent());
        SaleCreateResponse response = responseOpt.get();
        assertEquals(200L, response.getBillNo());
        assertEquals(Integer.valueOf(7), response.getCreatedBy());
        assertEquals(1, response.getItems().size());
        assertEquals(10, response.getItems().get(0).getInventoryId());
        PaymentBreakdownDTO breakdown = response.getPaymentBreakdown();
        assertNotNull(breakdown);
        assertEquals(new BigDecimal("210.00"), breakdown.getCard());
    }

    @Test
    void getSaleByBillNo_NotFound() {
        when(saleRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<SaleCreateResponse> responseOpt = saleService.getSaleByBillNo(999L);

        assertTrue(responseOpt.isEmpty());
    }
}
