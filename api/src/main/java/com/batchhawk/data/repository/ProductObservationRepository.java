package com.batchhawk.data.repository;

import com.batchhawk.data.entity.observation.ProductObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductObservationRepository extends JpaRepository<ProductObservation, Long> {

    // All variants (bag sizes) from the most recent scrape run for one product
    @Query(value = """
        SELECT o.* FROM product_observations o
        JOIN (
            SELECT product_id, MAX(observed_at) AS max_at
            FROM product_observations
            WHERE product_id = :productId
            GROUP BY product_id
        ) latest ON o.product_id = latest.product_id AND o.observed_at = latest.max_at
        ORDER BY o.bag_size_oz ASC NULLS LAST
        """, nativeQuery = true)
    List<ProductObservation> findLatestVariantsByProductId(@Param("productId") Long productId);

    // All variants from the most recent scrape run, for a batch of products
    @Query(value = """
        SELECT o.* FROM product_observations o
        JOIN (
            SELECT product_id, MAX(observed_at) AS max_at
            FROM product_observations
            WHERE product_id IN (:productIds)
            GROUP BY product_id
        ) latest ON o.product_id = latest.product_id AND o.observed_at = latest.max_at
        ORDER BY o.product_id, o.bag_size_oz ASC NULLS LAST
        """, nativeQuery = true)
    List<ProductObservation> findAllLatestVariantsByProductIds(@Param("productIds") List<Long> productIds);
}
