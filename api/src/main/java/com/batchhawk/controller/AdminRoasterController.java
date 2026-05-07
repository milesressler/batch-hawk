package com.batchhawk.controller;

import com.batchhawk.data.response.AdminRoasterResponse;
import com.batchhawk.service.AdminRoasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/admin/roasters", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AdminRoasterController {

    private final AdminRoasterService adminRoasterService;

    @GetMapping
    public List<AdminRoasterResponse> list() {
        return adminRoasterService.listAll();
    }

    @PostMapping("/{id}/trigger")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trigger(@PathVariable final UUID id) {
        adminRoasterService.triggerRefresh(id);
    }

    @PostMapping("/{id}/products/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateProducts(@PathVariable final UUID id) {
        adminRoasterService.deactivateProducts(id);
    }
}