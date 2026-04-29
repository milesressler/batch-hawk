package com.batchhawk.data.entity.observation;

import com.batchhawk.data.entity.BaseEntity;
import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.enums.ObservationSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "field_observations", indexes = {
    @Index(name = "idx_field_observations_roaster_field_observed", columnList = "roaster_id, field_name, observed_at DESC")
})
public class FieldObservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roaster_id", nullable = false)
    private Roaster roaster;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String value;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObservationSource source;

    @Column(precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "agent_run_id")
    private UUID agentRunId;
}
