package com.batchhawk.data.entity.user;

import com.batchhawk.data.entity.BaseEntity;
import com.batchhawk.data.entity.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_saves", uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_saves_user_product", columnNames = {"user_id", "product_id"})
})
public class UserSave extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;
}
