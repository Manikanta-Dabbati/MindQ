package com.mindq.repository;

import com.mindq.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now OR prt.used = true")
    void deleteExpiredAndUsed(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}