package org.pharmacy.mgmt.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Column(name = "user_id")
    private Integer userId;
}
