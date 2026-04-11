package org.pharmacy.mgmt.repository;

import org.pharmacy.mgmt.model.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, Integer> {

    List<ReturnItem> findBySaleReturnReturnIdOrderByReturnItemIdAsc(Long returnId);

    List<ReturnItem> findBySaleItemBillItemIdOrderByReturnItemIdAsc(Integer billItemId);
}
