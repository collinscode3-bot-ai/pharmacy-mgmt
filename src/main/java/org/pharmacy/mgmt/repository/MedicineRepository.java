package org.pharmacy.mgmt.repository;

import org.pharmacy.mgmt.dto.LowStockDTO;
import org.pharmacy.mgmt.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer>, JpaSpecificationExecutor<Medicine> {

    @Query("SELECT new org.pharmacy.mgmt.dto.LowStockDTO(m.name, COALESCE(SUM(i.quantityOnHand), 0), m.reorderLevel) " +
           "FROM Medicine m LEFT JOIN m.inventories i " +
           "GROUP BY m.name, m.reorderLevel " +
           "HAVING COALESCE(SUM(i.quantityOnHand), 0) < m.reorderLevel")
    List<LowStockDTO> findLowStockMedicines();

    @Query("SELECT COUNT(m) FROM Medicine m WHERE COALESCE((SELECT SUM(i.quantityOnHand) FROM Inventory i WHERE i.medicine = m), 0) < m.reorderLevel")
    Long countLowStockMedicines();

    @Query("SELECT COUNT(m) FROM Medicine m WHERE COALESCE((SELECT SUM(i.quantityOnHand) FROM Inventory i WHERE i.medicine = m), 0) = 0")
    Long countOutOfStockMedicines();

    List<Medicine> findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(String name, String genericName);

    boolean existsByNameAndStrength(String name, String strength);

    List<Medicine> findTop16ByOrderByNameAsc();

    List<Medicine> findByNameStartingWithIgnoreCaseOrderByNameAsc(String prefix);
}
