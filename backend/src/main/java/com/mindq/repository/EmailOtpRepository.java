package com.mindq.repository;

import com.mindq.model.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String email, String purpose);

    List<EmailOtp> findByEmailAndPurposeAndUsedFalse(String email, String purpose);

    @Modifying
    @Query("DELETE FROM EmailOtp e WHERE e.expiresAt < :now OR e.used = true")
    void deleteExpiredAndUsed(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EmailOtp e WHERE e.email = :email")
    void deleteByEmail(@Param("email") String email);

    @Query("SELECT COUNT(e) FROM EmailOtp e WHERE e.email = :email AND e.purpose = :purpose AND e.createdAt > :since")
    long countRecentByPurpose(@Param("email") String email, @Param("purpose") String purpose, @Param("since") LocalDateTime since);
}