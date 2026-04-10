package org.pharmacy.mgmt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDTO {
    private BigDecimal todaySales;
    private Long lowStockCount;
    private Long expiringSoonCount;

    @JsonProperty("dailySales")
    public BigDecimal getDailySales() {
        return todaySales;
    }

    @JsonProperty("dailySales")
    public void setDailySales(BigDecimal dailySales) {
        this.todaySales = dailySales;
    }
}
