package org.pharmacy.mgmt.repository;

import org.pharmacy.mgmt.dto.ExpiringItemDTO;
import org.pharmacy.mgmt.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

        @Query("SELECT new org.pharmacy.mgmt.dto.ExpiringItemDTO(i.medicine.name, i.batchNumber, i.expirationDate, i.quantityOnHand, i.purchasePrice, i.sellingPrice, i.location) " +
            "FROM Inventory i " +
            "WHERE i.expirationDate BETWEEN CURRENT_DATE AND :endDate")
        List<ExpiringItemDTO> findExpiringItems(LocalDate endDate);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.expirationDate BETWEEN CURRENT_DATE AND :endDate")
    Long countExpiringItems(LocalDate endDate);

    long countByMedicineMedicineId(Integer medicineId);

    @Query("SELECT COALESCE(SUM(i.quantityOnHand),0) FROM Inventory i WHERE i.medicine.medicineId = :medicineId")
    Integer totalQuantityByMedicineId(Integer medicineId);

    @Query("SELECT COALESCE(MIN(i.sellingPrice), 0) FROM Inventory i WHERE i.medicine.medicineId = :medicineId")
    java.math.BigDecimal minSellingPriceByMedicineId(Integer medicineId);

    @Query("SELECT m.medicineId FROM Medicine m LEFT JOIN m.inventories i GROUP BY m.medicineId HAVING COALESCE(SUM(i.quantityOnHand),0) > m.reorderLevel")
    java.util.List<Integer> findMedicineIdsInStock();

    @Query("SELECT m.medicineId FROM Medicine m LEFT JOIN m.inventories i GROUP BY m.medicineId HAVING COALESCE(SUM(i.quantityOnHand),0) > 0 AND COALESCE(SUM(i.quantityOnHand),0) <= m.reorderLevel")
    java.util.List<Integer> findMedicineIdsLowStock();

    @Query("SELECT m.medicineId FROM Medicine m LEFT JOIN m.inventories i GROUP BY m.medicineId HAVING COALESCE(SUM(i.quantityOnHand),0) = 0")
    java.util.List<Integer> findMedicineIdsOutOfStock();

    @Query("SELECT COALESCE(SUM(i.quantityOnHand * i.sellingPrice), 0) FROM Inventory i")
    java.math.BigDecimal calculateCatalogValue();

    java.util.List<Inventory> findByMedicineMedicineIdOrderByExpirationDateAsc(Integer medicineId);

    java.util.Optional<Inventory> findFirstByMedicineMedicineIdAndBatchNumber(Integer medicineId, String batchNumber);
}
