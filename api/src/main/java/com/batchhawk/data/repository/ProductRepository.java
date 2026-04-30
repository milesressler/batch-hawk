package com.batchhawk.data.repository;

import com.batchhawk.data.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByUuid(UUID uuid);

    Optional<Product> findByRoasterIdAndNameIgnoreCase(Long roasterId, String name);
}
