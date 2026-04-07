package org.pharmacy.mgmt.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.pharmacy.mgmt.model.User;

@Entity
@Table(name = "Sales", indexes = {
    @Index(name = "idx_sale_date", columnList = "sale_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_no")
    private Integer billNo;

    @CreationTimestamp
    @Column(name = "sale_date", updatable = false)
    private LocalDateTime saleDate;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_tax_amount", precision = 12, scale = 2)
    private BigDecimal totalTaxAmount;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;
}
