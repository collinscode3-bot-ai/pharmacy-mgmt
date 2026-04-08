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

    @Query(value = "SELECT DATE(sale_date) as d, COALESCE(SUM(total_amount),0) as total FROM Sales " +
            "WHERE DATE(sale_date) BETWEEN DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND CURDATE() " +
            "GROUP BY DATE(sale_date) ORDER BY DATE(sale_date)", nativeQuery = true)
    java.util.List<Object[]> sumLast7DaysGroupByDate();

    java.util.List<org.pharmacy.mgmt.model.Sale> findTop5ByOrderBySaleDateDesc();
}
