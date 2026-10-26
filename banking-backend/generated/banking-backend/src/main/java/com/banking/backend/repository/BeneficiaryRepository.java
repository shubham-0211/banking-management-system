
package com.banking.backend.repository;

import com.banking.backend.entity.Beneficiary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository
        extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByCustomerIdOrderByIdAsc(Long customerId);
}
