package org.pharmacy.mgmt.repository;

import org.pharmacy.mgmt.dto.LowStockDTO;
import org.pharmacy.mgmt.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer> {

    @Query("SELECT new org.pharmacy.mgmt.dto.LowStockDTO(m.name, SUM(i.quantityOnHand), m.reorderLevel) " +
           "FROM Medicine m JOIN m.inventories i " +
           "GROUP BY m.name, m.reorderLevel " +
           "HAVING SUM(i.quantityOnHand) < m.reorderLevel")
    List<LowStockDTO> findLowStockMedicines();

    @Query("SELECT COUNT(m) FROM Medicine m WHERE (SELECT SUM(i.quantityOnHand) FROM Inventory i WHERE i.medicine = m) < m.reorderLevel")
    Long countLowStockMedicines();
}
