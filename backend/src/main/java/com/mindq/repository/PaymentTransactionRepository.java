package com.mindq.repository;

import com.mindq.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByProviderOrderId(String providerOrderId);

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Sum of successful payment amounts — avoids loading all transactions in Java.
     */
    @Query("SELECT COALESCE(SUM(pt.amountPaise), 0) FROM PaymentTransaction pt WHERE pt.status = 'SUCCESS'")
    long sumSuccessfulAmount();

    /**
     * Find payment transactions by user ID with eagerly loaded Plan relationship.
     * This prevents LazyInitializationException when accessing plan details outside a transaction.
     */
    @Query("SELECT pt FROM PaymentTransaction pt JOIN FETCH pt.plan WHERE pt.user.id = :userId ORDER BY pt.createdAt DESC")
    java.util.List<PaymentTransaction> findByUserIdWithPlanOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM PaymentTransaction pt WHERE pt.user.id = :userId")
    void deleteByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
