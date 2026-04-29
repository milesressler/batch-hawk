package com.batchhawk.data.repository;

import com.batchhawk.data.entity.observation.ProductObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductObservationRepository extends JpaRepository<ProductObservation, UUID> {

    // Most recent observation per product (used for current pricing display)
    @Query(value = """
        SELECT DISTINCT ON (product_id) *
        FROM product_observations
        WHERE product_id = :productId
        ORDER BY product_id, observed_at DESC
        """, nativeQuery = true)
    Optional<ProductObservation> findLatestByProductId(@Param("productId") UUID productId);

    @Query(value = """
        SELECT DISTINCT ON (product_id) *
        FROM product_observations
        WHERE product_id IN (:productIds)
        ORDER BY product_id, observed_at DESC
        """, nativeQuery = true)
    List<ProductObservation> findLatestByProductIds(@Param("productIds") List<UUID> productIds);
}
