package com.batchhawk.controller;

import com.batchhawk.data.response.RoasterResponse;
import com.batchhawk.service.RoasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/roasters", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RoasterController {

    private final RoasterService roasterService;

    @GetMapping
    public Page<RoasterResponse> list(
        @RequestParam(required = false) final String name,
        @RequestParam(required = false, defaultValue = "true") final boolean activeOnly,
        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) final Pageable pageable
    ) {
        return roasterService.list(name, activeOnly, pageable);
    }

    @GetMapping("/{id}")
    public RoasterResponse get(@PathVariable final UUID id) {
        return roasterService.getById(id);
    }
}
