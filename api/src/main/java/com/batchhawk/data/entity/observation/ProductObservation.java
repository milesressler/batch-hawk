package com.batchhawk.data.entity.observation;

import com.batchhawk.data.entity.BaseEntity;
import com.batchhawk.data.entity.product.Product;
import com.batchhawk.data.enums.ValueTier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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

    @Column(name = "bag_size_oz", precision = 6, scale = 2)
    private BigDecimal bagSizeOz;

    @Column(name = "price_usd", precision = 8, scale = 2)
    private BigDecimal priceUsd;

    @Column(name = "price_per_oz", precision = 8, scale = 4)
    private BigDecimal pricePerOz;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_tier", length = 20)
    private ValueTier valueTier;

    @Column(name = "in_stock")
    private Boolean inStock;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "agent_run_id")
    private UUID agentRunId;
}
