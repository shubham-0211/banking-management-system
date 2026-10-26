
package com.banking.backend.repository;

import com.banking.backend.entity.BankTransaction;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    Page<BankTransaction> findByAccountId(
        Long accountId,
        Pageable pageable
    );

    List<BankTransaction> findByAccountIdOrderByCreatedAtDesc(
        Long accountId
    );
}
