package com.batchhawk.integration;

import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.entity.product.Product;
import com.batchhawk.data.enums.ModerationStatus;
import com.batchhawk.data.repository.RoasterRepository;
import com.batchhawk.data.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductControllerIT extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;
    @Autowired RoasterRepository roasterRepository;

    private Roaster roaster;

    @BeforeEach
    void setUp() {
        roaster = saveRoaster("Onyx Coffee Lab");
    }

    @Test
    void listProducts_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listProducts_returnsActiveProducts() throws Exception {
        saveProduct("Ethiopia Guji", true);
        saveProduct("Old Lot Closeout", false);

        mockMvc.perform(get("/api/products").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Ethiopia Guji"));
    }

    @Test
    void listProducts_filtersByRoaster() throws Exception {
        final var other = saveRoaster("Counter Culture");
        saveProduct("Ethiopia Guji", true);
        saveProductFor(other, "Toscano Blend", true);

        mockMvc.perform(get("/api/products?roasterId={id}", roaster.getId()).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Ethiopia Guji"));
    }

    @Test
    void listProducts_filtersByName() throws Exception {
        saveProduct("Ethiopia Guji", true);
        saveProduct("Colombia Huila", true);

        mockMvc.perform(get("/api/products?name=ethiopia").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Ethiopia Guji"));
    }

    @Test
    void listProducts_filtersByRoasterAndName() throws Exception {
        final var other = saveRoaster("Counter Culture");
        saveProduct("Ethiopia Guji", true);
        saveProduct("Colombia Huila", true);
        saveProductFor(other, "Ethiopia Yirgacheffe", true);

        mockMvc.perform(get("/api/products?roasterId={id}&name=ethiopia", roaster.getId()).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Ethiopia Guji"));
    }

    @Test
    void listProducts_includesInactive_whenActiveOnlyFalse() throws Exception {
        saveProduct("Ethiopia Guji", true);
        saveProduct("Old Lot Closeout", false);

        mockMvc.perform(get("/api/products?activeOnly=false").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listProducts_includesRoasterName() throws Exception {
        saveProduct("Ethiopia Guji", true);

        mockMvc.perform(get("/api/products").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].roasterName").value("Onyx Coffee Lab"));
    }

    @Test
    void getProduct_returnsProduct_whenFound() throws Exception {
        final var product = saveProduct("Ethiopia Guji", true);

        mockMvc.perform(get("/api/products/{id}", product.getId()).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(product.getId().toString()))
            .andExpect(jsonPath("$.name").value("Ethiopia Guji"))
            .andExpect(jsonPath("$.roastLevel").value("light"))
            .andExpect(jsonPath("$.flavorProfile", hasItem("blueberry")))
            .andExpect(jsonPath("$.flavorProfile", hasItem("jasmine")));
    }

    @Test
    void getProduct_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/products/{id}", "00000000-0000-0000-0000-000000000000").with(jwt()))
            .andExpect(status().isNotFound());
    }

    private Roaster saveRoaster(final String name) {
        final var r = new Roaster();
        r.setName(name);
        r.setActive(true);
        r.setModerationStatus(ModerationStatus.APPROVED);
        return roasterRepository.save(r);
    }

    private Product saveProduct(final String name, final boolean active) {
        return saveProductFor(roaster, name, active);
    }

    private Product saveProductFor(final Roaster r, final String name, final boolean active) {
        final var product = new Product();
        product.setRoaster(r);
        product.setName(name);
        product.setActive(active);
        product.setRoastLevel("light");
        product.setFlavorProfile(List.of("blueberry", "jasmine"));
        return productRepository.save(product);
    }
}
