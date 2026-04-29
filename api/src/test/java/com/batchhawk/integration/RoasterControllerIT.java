package com.batchhawk.integration;

import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.enums.ModerationStatus;
import com.batchhawk.data.repository.RoasterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RoasterControllerIT extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired RoasterRepository roasterRepository;

    @Test
    void listRoasters_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/roasters"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listRoasters_returnsEmptyPage_whenNoneExist() throws Exception {
        mockMvc.perform(get("/api/roasters").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listRoasters_returnsActiveRoasters() throws Exception {
        saveRoaster("Onyx Coffee Lab", true);
        saveRoaster("Abandoned Warehouse", false);

        mockMvc.perform(get("/api/roasters").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Onyx Coffee Lab"));
    }

    @Test
    void listRoasters_includesInactive_whenActiveOnlyFalse() throws Exception {
        saveRoaster("Onyx Coffee Lab", true);
        saveRoaster("Abandoned Warehouse", false);

        mockMvc.perform(get("/api/roasters?activeOnly=false").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listRoasters_filtersByName() throws Exception {
        saveRoaster("Onyx Coffee Lab", true);
        saveRoaster("Counter Culture", true);

        mockMvc.perform(get("/api/roasters?name=onyx").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Onyx Coffee Lab"));
    }

    @Test
    void listRoasters_filtersBy_nameAndActiveOnly() throws Exception {
        saveRoaster("Onyx Coffee Lab", true);
        saveRoaster("Onyx Archive", false);
        saveRoaster("Counter Culture", true);

        mockMvc.perform(get("/api/roasters?name=onyx&activeOnly=false").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Onyx Coffee Lab", "Onyx Archive")));
    }

    @Test
    void getRoaster_returnsRoaster_whenFound() throws Exception {
        final var roaster = saveRoaster("Onyx Coffee Lab", true);

        mockMvc.perform(get("/api/roasters/{id}", roaster.getId()).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(roaster.getId().toString()))
            .andExpect(jsonPath("$.name").value("Onyx Coffee Lab"));
    }

    @Test
    void getRoaster_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/roasters/{id}", "00000000-0000-0000-0000-000000000000").with(jwt()))
            .andExpect(status().isNotFound());
    }

    private Roaster saveRoaster(final String name, final boolean active) {
        final var roaster = new Roaster();
        roaster.setName(name);
        roaster.setActive(active);
        roaster.setModerationStatus(ModerationStatus.APPROVED);
        return roasterRepository.save(roaster);
    }
}
