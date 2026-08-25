package com.aaax.core.kafka.event;

import com.aaax.core.kafka.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Request util-service to delete a CDN file (DB row + S3 object) asynchronously.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CdnFileDeleteEvent extends BaseEvent {

    /** Public id from upload, e.g. {@code cdn_7460487483503935488}. */
    private String cdnId;

    /** Originating service, e.g. program-management-service. */
    private String sourceService;
}
