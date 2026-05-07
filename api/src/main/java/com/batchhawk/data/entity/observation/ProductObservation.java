package com.batchhawk.data.entity.observation;

import com.batchhawk.data.entity.BaseEntity;
import com.batchhawk.data.entity.agent.AgentRun;
import com.batchhawk.data.entity.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_observations", indexes = {
    @Index(name = "idx_product_observations_product_observed", columnList = "product_id, observed_at DESC")
})
public class ProductObservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "bag_size")
    private Integer bagSize;

    @Column(name = "bag_size_unit", length = 10)
    private String bagSizeUnit;

    @Column(name = "bag_size_oz", precision = 6, scale = 2)
    private BigDecimal bagSizeOz;

    @Column(name = "price_usd", precision = 8, scale = 2)
    private BigDecimal priceUsd;

    @Column(name = "price_per_oz", precision = 8, scale = 4)
    private BigDecimal pricePerOz;

    @Column(name = "in_stock")
    private Boolean inStock;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_run_id")
    private AgentRun agentRun;
}