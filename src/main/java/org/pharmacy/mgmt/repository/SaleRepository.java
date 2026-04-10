package org.pharmacy.mgmt.repository;

import org.pharmacy.mgmt.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query(value = "SELECT COALESCE(SUM(grand_total_amount), 0) FROM Sales " +
            "WHERE DATE(created_at) = CURDATE() AND COALESCE(is_active, 1) = 1", nativeQuery = true)
    BigDecimal calculateTodaySales();

    @Query(value = "SELECT DATE(created_at) as d, COALESCE(SUM(grand_total_amount),0) as total FROM Sales " +
            "WHERE DATE(created_at) BETWEEN DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND CURDATE() AND is_active = 1 " +
            "GROUP BY DATE(created_at) ORDER BY DATE(created_at)", nativeQuery = true)
    java.util.List<Object[]> sumLast7DaysGroupByDate();

    java.util.List<org.pharmacy.mgmt.model.Sale> findTop5ByIsActiveTrueOrderByCreatedAtDesc();
}
