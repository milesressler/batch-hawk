package com.batchhawk.data.specification;

import com.batchhawk.data.entity.product.Product;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public class ProductSpec {

    public static Specification<Product> forRoaster(final UUID roasterId) {
        return (root, query, cb) ->
            cb.equal(root.get("roaster").get("uuid"), roasterId);
    }

    public static Specification<Product> keywordSearch(final String keyword) {
        return (root, query, cb) -> {
            final String pattern = "%" + keyword.toLowerCase() + "%";
            final var roasterJoin = root.join("roaster", JoinType.INNER);
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("originCountry")), pattern),
                cb.like(cb.lower(root.get("originRegion")), pattern),
                cb.like(cb.lower(roasterJoin.get("name")), pattern)
            );
        };
    }

    public static Specification<Product> roastLevelIn(final List<String> values) {
        return (root, query, cb) -> root.get("roastLevel").in(values);
    }

    public static Specification<Product> processIn(final List<String> values) {
        return (root, query, cb) -> root.get("process").in(values);
    }

    public static Specification<Product> productTypeIn(final List<String> values) {
        return (root, query, cb) -> root.get("productType").in(values);
    }

    public static Specification<Product> availabilityTypeIn(final List<String> values) {
        return (root, query, cb) -> root.get("availabilityType").in(values);
    }

    public static Specification<Product> isDecaf() {
        return (root, query, cb) -> cb.isTrue(root.get("decaf"));
    }

    public static Specification<Product> isActive(final boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
