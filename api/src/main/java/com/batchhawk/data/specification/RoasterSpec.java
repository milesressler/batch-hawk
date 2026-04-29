package com.batchhawk.data.specification;

import com.batchhawk.data.entity.roaster.Roaster;
import org.springframework.data.jpa.domain.Specification;

public class RoasterSpec {

    public static Specification<Roaster> nameContains(final String name) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Roaster> isActive(final boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
