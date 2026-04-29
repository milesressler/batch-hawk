package com.batchhawk.data.specification;

import com.batchhawk.data.entity.product.Product;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class ProductSpec {

    public static Specification<Product> forRoaster(final UUID roasterId) {
        return (root, query, cb) ->
            cb.equal(root.get("roaster").get("id"), roasterId);
    }

    public static Specification<Product> nameContains(final String name) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> isActive(final boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
