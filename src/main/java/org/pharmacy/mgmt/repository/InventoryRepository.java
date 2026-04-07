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
}
