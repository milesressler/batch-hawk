package com.batchhawk.controller;

import com.batchhawk.data.response.ProductResponse;
import com.batchhawk.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/products", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Page<ProductResponse> list(
        @RequestParam(required = false) final UUID roasterId,
        @RequestParam(required = false) final String keyword,
        @RequestParam(required = false, defaultValue = "true") final boolean activeOnly,
        @RequestParam(required = false) final List<String> roastLevel,
        @RequestParam(required = false) final List<String> process,
        @RequestParam(required = false) final List<String> productType,
        @RequestParam(required = false) final List<String> availabilityType,
        @RequestParam(required = false, defaultValue = "false") final boolean decafOnly,
        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) final Pageable pageable
    ) {
        return productService.list(roasterId, keyword, activeOnly, roastLevel, process, productType, availabilityType, decafOnly, pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable final UUID id) {
        return productService.getById(id);
    }
}
