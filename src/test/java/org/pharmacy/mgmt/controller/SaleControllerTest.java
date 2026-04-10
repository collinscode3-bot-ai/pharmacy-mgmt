package org.pharmacy.mgmt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.pharmacy.mgmt.dto.SaleCreateRequest;
import org.pharmacy.mgmt.dto.SaleCreateResponse;
import org.pharmacy.mgmt.dto.PaymentBreakdownDTO;
import org.pharmacy.mgmt.dto.SaleItemCreateRequest;
import org.pharmacy.mgmt.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SaleController.class)
public class SaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SaleService saleService;

    @Test
    @WithMockUser
    void createSale_ReturnsCreatedResponse() throws Exception {
        SaleCreateRequest request = SaleCreateRequest.builder()
                .customerName("Jane Doe")
                .paymentMethod("Cash")
                .subtotalAmount(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("0.00"))
                .gstAmount(new BigDecimal("5.00"))
                .grandTotalAmount(new BigDecimal("105.00"))
                .items(List.of(SaleItemCreateRequest.builder().medicineId(1).inventoryId(101).quantity(1).build()))
                .build();

        SaleCreateResponse response = SaleCreateResponse.builder()
                .saleId(55L)
                .billNo(55L)
                .customerName("Jane Doe")
                .paymentMethod("Cash")
                .items(List.of(SaleItemCreateRequest.builder().medicineId(1).inventoryId(101).quantity(1).build()))
                .subtotalAmount(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("0.00"))
                .gstAmount(new BigDecimal("5.00"))
                .grandTotalAmount(new BigDecimal("105.00"))
                .paymentBreakdown(PaymentBreakdownDTO.builder().cash(new BigDecimal("105.00")).build())
                .createdAt(LocalDateTime.of(2026, 4, 10, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 10, 10, 0))
                .createdBy(7)
                .isActive(true)
                .build();

        when(saleService.createSale(any(SaleCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/sales")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/sales/55"))
                .andExpect(jsonPath("$.billNo").value(55))
                .andExpect(jsonPath("$.customerName").value("Jane Doe"));
    }

        @Test
        @WithMockUser
        void getSaleByBillNo_ReturnsOkResponse() throws Exception {
                SaleCreateResponse response = SaleCreateResponse.builder()
                                .saleId(88L)
                                .billNo(88L)
                                .customerName("John Doe")
                                .paymentMethod("Card")
                                .items(List.of(SaleItemCreateRequest.builder().medicineId(1).inventoryId(101).quantity(2).build()))
                                .subtotalAmount(new BigDecimal("200.00"))
                                .discountAmount(new BigDecimal("0.00"))
                                .gstAmount(new BigDecimal("10.00"))
                                .grandTotalAmount(new BigDecimal("210.00"))
                                .paymentBreakdown(PaymentBreakdownDTO.builder().card(new BigDecimal("210.00")).build())
                                .createdAt(LocalDateTime.of(2026, 4, 10, 12, 0))
                                .updatedAt(LocalDateTime.of(2026, 4, 10, 12, 0))
                                .createdBy(7)
                                .isActive(true)
                                .build();

                when(saleService.getSaleByBillNo(88L)).thenReturn(Optional.of(response));

                mockMvc.perform(get("/api/sales/88"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.billNo").value(88))
                                .andExpect(jsonPath("$.customerName").value("John Doe"));
        }
}
