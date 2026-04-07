package org.pharmacy.mgmt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.pharmacy.mgmt.model.Tax;

public interface TaxRepository extends JpaRepository<Tax, Integer> {
}
