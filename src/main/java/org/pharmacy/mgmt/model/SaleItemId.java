package org.pharmacy.mgmt.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SaleItemId implements Serializable {

    @Column(name = "bill_no")
    private Integer billNo;

    @Column(name = "medicine_id")
    private Integer medicineId;
}
