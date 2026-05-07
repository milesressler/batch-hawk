package com.batchhawk.data.entity.product;

import com.batchhawk.data.entity.BaseEntity;
import com.batchhawk.data.entity.roaster.Roaster;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roaster_id", nullable = false)
    private Roaster roaster;

    @Column(nullable = false)
    private String name;

    @Column(name = "roast_level", length = 100)
    private String roastLevel;

    @Column(name = "product_type", length = 100)
    private String productType;

    @Column(name = "origin_country", length = 100)
    private String originCountry;

    @Column(name = "origin_region", length = 100)
    private String originRegion;

    @Column(length = 100)
    private String process;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "brew_methods", columnDefinition = "text[]")
    private List<String> brewMethods;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "flavor_profile", columnDefinition = "text[]")
    private List<String> flavorProfile;

    @Column(name = "is_decaf", nullable = false)
    private boolean decaf = false;

    @Column(name = "availability_type", length = 100)
    private String availabilityType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "product_url", length = 1000)
    private String productUrl;

    @Column(name = "external_product_id", length = 255)
    private String externalProductId;

    @Column(name = "offers_grinding", nullable = false)
    private boolean offersGrinding = false;
}
