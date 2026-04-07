package org.pharmacy.mgmt.repository;

import org.pharmacy.mgmt.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Integer> {
    long countByMedicineMedicineId(Integer medicineId);
}
