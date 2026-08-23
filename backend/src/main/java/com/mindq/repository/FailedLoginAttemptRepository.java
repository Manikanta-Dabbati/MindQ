package com.mindq.repository;

import com.mindq.model.FailedLoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface FailedLoginAttemptRepository extends JpaRepository<FailedLoginAttempt, Long> {

    long countByEmailAndAttemptedAtAfter(String email, LocalDateTime since);

    @Modifying
    @Query("DELETE FROM FailedLoginAttempt fla WHERE fla.attemptedAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    void deleteByEmail(String email);
}
