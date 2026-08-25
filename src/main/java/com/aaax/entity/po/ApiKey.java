package com.aaax.entity.po;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.core.utils.RandomHashGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "unique_api_key", columnNames = {"key"})
})
@Builder
public class ApiKey extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "api_key_id_generator", strategy = "com.aaax.core.utils.generator.id.SnowflakeIdGenerator")
    @GeneratedValue(generator = "api_key_id_generator")
    private Long id;

    @Column
    private String key;

    @Column
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant expireAt;

    @PrePersist // _ Generate [key] before persisting the entity
    private void prePersist() {
        if (key == null) {
            key = "sk_".concat(RandomHashGenerator.generateRandomHash(24));
        }
    }
}