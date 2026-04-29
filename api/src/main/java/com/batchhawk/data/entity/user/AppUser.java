package com.batchhawk.data.entity.user;

import com.batchhawk.data.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_users")
public class AppUser extends BaseEntity {

    @Column(name = "keycloak_subject", nullable = false, unique = true)
    private String keycloakSubject;

    private String email;

    @Column(name = "display_name")
    private String displayName;
}
