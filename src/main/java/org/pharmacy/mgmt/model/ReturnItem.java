package org.pharmacy.mgmt.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "Return_Items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_item_id")
    private Integer returnItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private SaleReturn saleReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_item_id", nullable = false)
    private SaleItem saleItem;

    @Column(name = "quantity_returned", nullable = false)
    private Integer quantityReturned;

    @Column(name = "refund_line_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundLineAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('RESTOCKED','DAMAGED','EXPIRED')")
    private ReturnItemStatus status;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
