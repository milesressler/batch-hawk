package com.batchhawk.data.entity.roaster;

import com.batchhawk.data.entity.BaseEntity;
import com.batchhawk.data.entity.user.AppUser;
import com.batchhawk.data.enums.IntegrationType;
import com.batchhawk.data.enums.ModerationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roasters")
public class Roaster extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    @Column(name = "email_list_url", length = 512)
    private String emailListUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "url_hints", columnDefinition = "jsonb")
    private String urlHints;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private AppUser submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false, length = 20)
    private IntegrationType integrationType = IntegrationType.UNKNOWN;
}
