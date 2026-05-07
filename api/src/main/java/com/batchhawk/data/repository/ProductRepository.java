package com.batchhawk.data.repository;

import com.batchhawk.data.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByUuid(UUID uuid);

    Optional<Product> findByRoasterIdAndNameIgnoreCase(Long roasterId, String name);

    Optional<Product> findByRoasterIdAndExternalProductId(Long roasterId, String externalProductId);

    @Modifying
    @Query("UPDATE Product p SET p.active = false WHERE p.roaster.id = :roasterId")
    int deactivateAllByRoasterId(@Param("roasterId") Long roasterId);
}
