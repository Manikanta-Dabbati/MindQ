package com.mindq.repository;

import com.mindq.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    Optional<UserSubscription> findByUserIdAndStatus(Long userId, String status);

    @Query("SELECT us FROM UserSubscription us JOIN FETCH us.plan WHERE us.user.id = :userId AND us.status = :status")
    Optional<UserSubscription> findByUserIdAndStatusWithPlan(@Param("userId") Long userId, @Param("status") String status);

    List<UserSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT us FROM UserSubscription us WHERE us.user.id = :userId")
    List<UserSubscription> findAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserSubscription us WHERE us.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(us) FROM UserSubscription us")
    long countAll();

    @Query("SELECT us.plan.code, COUNT(us) FROM UserSubscription us GROUP BY us.plan.code")
    List<Object[]> countByPlanCodeGrouped();
}
