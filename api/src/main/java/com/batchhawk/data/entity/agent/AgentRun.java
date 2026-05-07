package com.batchhawk.data.entity.agent;

import com.batchhawk.common.ScrapedField;
import com.batchhawk.data.entity.BaseEntity;
import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.enums.AgentRunStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "agent_runs")
public class AgentRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roaster_id", nullable = false)
    private Roaster roaster;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AgentRunStatus status;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "fields_found", columnDefinition = "text[]")
    private List<ScrapedField> fieldsFound;

    @Column(name = "feedback_notes", columnDefinition = "text")
    private String feedbackNotes;

    @Column(name = "checkout_notes", columnDefinition = "text")
    private String checkoutNotes;

    @Column(name = "input_tokens")
    private Long inputTokens;

    @Column(name = "output_tokens")
    private Long outputTokens;
}
