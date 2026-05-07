package com.batchhawk.unit;

import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.entity.product.Product;
import com.batchhawk.data.repository.ProductObservationRepository;
import com.batchhawk.data.repository.ProductRepository;
import com.batchhawk.exception.EntityNotFoundException;
import com.batchhawk.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ProductObservationRepository productObservationRepository;
    @InjectMocks ProductService productService;

    @Test
    void getById_returnsMappedResponse_whenFound() {
        final var product = product("Ethiopia Guji");
        final var uuid = UUID.randomUUID();
        ReflectionTestUtils.setField(product, "uuid", uuid);
        when(productRepository.findByUuid(uuid)).thenReturn(Optional.of(product));

        final var response = productService.getById(uuid);

        assertThat(response.name()).isEqualTo("Ethiopia Guji");
        assertThat(response.id()).isEqualTo(uuid);
    }

    @Test
    void getById_throwsEntityNotFoundException_whenNotFound() {
        final var uuid = UUID.randomUUID();
        when(productRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(uuid))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(uuid.toString());
    }

    @Test
    void list_withNoFilters_passesNullSpec() {
        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        productService.list(null, null, false, null, null, null, null, false, Pageable.unpaged());

        verify(productRepository).findAll(ArgumentMatchers.<Specification<Product>>isNull(), any(Pageable.class));
    }

    @Test
    void list_withRoasterId_passesNonNullSpec() {
        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>notNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        productService.list(UUID.randomUUID(), null, false, null, null, null, null, false, Pageable.unpaged());

        verify(productRepository).findAll(ArgumentMatchers.<Specification<Product>>notNull(), any(Pageable.class));
    }

    @Test
    void list_returnsMappedPage() {
        final var product = product("Colombia Huila");
        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(product)));

        final var result = productService.list(null, null, false, null, null, null, null, false, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("Colombia Huila");
    }

    private Product product(final String name) {
        final var roaster = new Roaster();
        roaster.setName("Onyx Coffee Lab");

        final var p = new Product();
        p.setName(name);
        p.setRoaster(roaster);
        p.setActive(true);
        return p;
    }
}
