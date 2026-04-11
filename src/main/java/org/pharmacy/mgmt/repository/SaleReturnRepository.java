package org.pharmacy.mgmt.repository;

import org.pharmacy.mgmt.model.SaleReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

    List<SaleReturn> findByOriginalSaleBillNoOrderByCreatedAtDesc(Long billNo);
}
