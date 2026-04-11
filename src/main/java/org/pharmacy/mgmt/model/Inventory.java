package org.pharmacy.mgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Inventory", indexes = {
    @Index(name = "idx_expiration_date", columnList = "expiration_date"),
    @Index(name = "idx_quantity_on_hand", columnList = "quantity_on_hand")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer inventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Builder.Default
    @Column(name = "quantity_on_hand")
    private Integer quantityOnHand = 0;

    @Column(name = "purchase_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "selling_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @Column(length = 100)
    private String location;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_updated", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime lastUpdated;
}
