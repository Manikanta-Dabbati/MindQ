package com.mindq.repository;

import com.mindq.enums.AIProviderType;
import com.mindq.model.AIModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIModelRepository extends JpaRepository<AIModel, Long> {

    Optional<AIModel> findByModelCode(String modelCode);

    List<AIModel> findByIsActiveTrue();

    List<AIModel> findByProviderAndIsActiveTrue(AIProviderType provider);

    Optional<AIModel> findByIsDefaultTrue();
}
