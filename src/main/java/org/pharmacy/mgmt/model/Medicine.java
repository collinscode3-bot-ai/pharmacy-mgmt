package org.pharmacy.mgmt.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import org.pharmacy.mgmt.model.Tax;

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

    @Column(name = "generic_name", length = 255)
    private String genericName;

    @Column(length = 255)
    private String manufacturer;

    @Column(length = 50)
    private String strength;

    @Column(name = "dosage_form", length = 50)
    private String dosageForm;

    @Builder.Default
    @Column(name = "reorder_level")
    private Integer reorderLevel = 10;

    @Builder.Default
    @Column(name = "is_prescription_required")
    private Boolean isPrescriptionRequired = false;

    @OneToMany(mappedBy = "medicine", cascade = CascadeType.ALL)
    private List<Inventory> inventories;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "tax_id")
    private Tax tax;
}
