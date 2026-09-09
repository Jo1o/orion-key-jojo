package com.orionkey.repository;

import com.orionkey.entity.ProductMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductMappingRepository extends JpaRepository<ProductMapping, UUID> {

    Optional<ProductMapping> findByProductIdAndEnabledTrue(UUID productId);

    Optional<ProductMapping> findByTghaoCode(String tghaoCode);
}
