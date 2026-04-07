package org.pharmacy.mgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "Medicines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer medicineId;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Column(name = "reorder_level")
    private Integer reorderLevel = 10;

    @Builder.Default
    @Column(name = "is_prescription_required")
    private Boolean isPrescriptionRequired = false;

    @OneToMany(mappedBy = "medicine", cascade = CascadeType.ALL)
    private List<Inventory> inventories;
}
