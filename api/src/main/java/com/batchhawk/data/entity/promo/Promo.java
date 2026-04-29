package com.batchhawk.data.entity.promo;

import com.batchhawk.data.entity.BaseEntity;
import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.enums.DiscountType;
import com.batchhawk.data.enums.PromoSource;
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
@Table(name = "promos", uniqueConstraints = {
    @UniqueConstraint(name = "uq_promos_roaster_code", columnNames = {"roaster_id", "code"})
})
public class Promo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roaster_id", nullable = false)
    private Roaster roaster;

    @Column(nullable = false, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 8, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "applies_to")
    private String appliesTo;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromoSource source;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;
}
