package com.batchhawk.data.repository;

import com.batchhawk.data.entity.roaster.Roaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface RoasterRepository extends JpaRepository<Roaster, UUID>, JpaSpecificationExecutor<Roaster> {
}
