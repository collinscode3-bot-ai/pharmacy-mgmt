package org.pharmacy.mgmt.repository;

import org.pharmacy.mgmt.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Integer> {

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.saleDate >= CURRENT_DATE")
    BigDecimal calculateTodaySales();
}
