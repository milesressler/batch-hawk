package com.batchhawk.unit;

import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.enums.ModerationStatus;
import com.batchhawk.data.repository.RoasterRepository;
import com.batchhawk.exception.EntityNotFoundException;
import com.batchhawk.service.RoasterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoasterServiceTest {

    @Mock RoasterRepository roasterRepository;
    @InjectMocks RoasterService roasterService;

    @Test
    void getById_returnsMappedResponse_whenFound() {
        final var roaster = roaster("Onyx Coffee Lab");
        when(roasterRepository.findById(roaster.getId())).thenReturn(Optional.of(roaster));

        final var response = roasterService.getById(roaster.getId());

        assertThat(response.name()).isEqualTo("Onyx Coffee Lab");
        assertThat(response.id()).isEqualTo(roaster.getId());
    }

    @Test
    void getById_throwsEntityNotFoundException_whenNotFound() {
        final var id = UUID.randomUUID();
        when(roasterRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roasterService.getById(id))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(id.toString());
    }

    @Test
    void list_withNoFilters_passesNullSpec() {
        when(roasterRepository.findAll(ArgumentMatchers.<Specification<Roaster>>isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        roasterService.list(null, false, Pageable.unpaged());

        verify(roasterRepository).findAll(ArgumentMatchers.<Specification<Roaster>>isNull(), any(Pageable.class));
    }

    @Test
    void list_withActiveOnly_passesNonNullSpec() {
        when(roasterRepository.findAll(ArgumentMatchers.<Specification<Roaster>>notNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        roasterService.list(null, true, Pageable.unpaged());

        verify(roasterRepository).findAll(ArgumentMatchers.<Specification<Roaster>>notNull(), any(Pageable.class));
    }

    @Test
    void list_returnsMappedPage() {
        final var roaster = roaster("Counter Culture");
        when(roasterRepository.findAll(ArgumentMatchers.<Specification<Roaster>>isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(roaster)));

        final var result = roasterService.list(null, false, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Counter Culture");
    }

    private Roaster roaster(final String name) {
        final var r = new Roaster();
        r.setName(name);
        r.setActive(true);
        r.setModerationStatus(ModerationStatus.APPROVED);
        return r;
    }
}
