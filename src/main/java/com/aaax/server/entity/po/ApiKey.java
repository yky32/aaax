package com.aaax.server.entity.po;

import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.core.utils.RandomHashGenerator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "api_keys")
@Builder
public class ApiKey extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "api_key_id_generator", strategy = "com.aaax.core.utils.generator.id.SnowflakeIdGenerator")
    @GeneratedValue(generator = "api_key_id_generator")
    private Long id;

    @Column(unique = true)
    private String secretKey;

    @Column
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant expireAt;

    @PrePersist
    private void generateSecretKey() {
        if (secretKey == null) {
            secretKey = "sk_".concat(Objects.requireNonNull(RandomHashGenerator.generateRandomHash(24)));
        }
    }
}
