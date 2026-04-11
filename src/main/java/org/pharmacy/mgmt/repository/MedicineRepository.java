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
        "FROM Medicine m LEFT JOIN m.inventories i WITH COALESCE(i.isActive, true) = true " +
        "WHERE COALESCE(m.isActive, true) = true " +
           "GROUP BY m.name, m.reorderLevel " +
           "HAVING COALESCE(SUM(i.quantityOnHand), 0) < m.reorderLevel")
    List<LowStockDTO> findLowStockMedicines();

    @Query("SELECT COUNT(m) FROM Medicine m WHERE COALESCE(m.isActive, true) = true AND " +
        "COALESCE((SELECT SUM(i.quantityOnHand) FROM Inventory i WHERE i.medicine = m AND COALESCE(i.isActive, true) = true), 0) < m.reorderLevel")
    Long countLowStockMedicines();

    @Query("SELECT COUNT(m) FROM Medicine m WHERE COALESCE(m.isActive, true) = true AND " +
        "COALESCE((SELECT SUM(i.quantityOnHand) FROM Inventory i WHERE i.medicine = m AND COALESCE(i.isActive, true) = true), 0) = 0")
    Long countOutOfStockMedicines();

    @Query("SELECT m FROM Medicine m WHERE COALESCE(m.isActive, true) = true AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :genericName, '%')))")
    List<Medicine> findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(String name, String genericName);

    boolean existsByNameAndStrength(String name, String strength);

    @Query("SELECT m FROM Medicine m WHERE COALESCE(m.isActive, true) = true ORDER BY m.name ASC")
    List<Medicine> findTop16ByOrderByNameAsc();

    @Query("SELECT m FROM Medicine m WHERE COALESCE(m.isActive, true) = true AND LOWER(m.name) LIKE LOWER(CONCAT(:prefix, '%')) ORDER BY m.name ASC")
    List<Medicine> findByNameStartingWithIgnoreCaseOrderByNameAsc(String prefix);

    @Query("SELECT COUNT(m) FROM Medicine m WHERE COALESCE(m.isActive, true) = true")
    long countActiveMedicines();
}
