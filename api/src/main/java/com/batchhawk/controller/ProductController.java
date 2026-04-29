package com.batchhawk.controller;

import com.batchhawk.data.response.ProductResponse;
import com.batchhawk.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Page<ProductResponse> list(
        @RequestParam(required = false) final UUID roasterId,
        @RequestParam(required = false) final String name,
        @RequestParam(required = false, defaultValue = "true") final boolean activeOnly,
        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) final Pageable pageable
    ) {
        return productService.list(roasterId, name, activeOnly, pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable final UUID id) {
        return productService.getById(id);
    }
}
