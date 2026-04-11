package org.pharmacy.mgmt.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleReturn {

    @Id
    @Column(name = "return_id")
    private Long returnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_bill_no", nullable = false)
    private Sale originalSale;

    @Column(name = "total_refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRefundAmount;

    @Column(name = "return_reason", length = 255)
    private String returnReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Integer createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> returnItems = new ArrayList<>();
}
