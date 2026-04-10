package org.pharmacy.mgmt.repository;

import org.pharmacy.mgmt.model.PaymentBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentBreakdownRepository extends JpaRepository<PaymentBreakdown, Integer> {
    java.util.List<PaymentBreakdown> findBySaleBillNoOrderByPaymentIdAsc(Long billNo);
}
