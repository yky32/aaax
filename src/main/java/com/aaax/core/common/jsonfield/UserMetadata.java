package com.aaax.core.common.jsonfield;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserMetadata {
    // private String avatar; // migrated to userProfile.metadata[avatar]
    // private String email; // migrated to userProfile.metadata[email]
    // private String language; // migrated to userProfile.metadata[language]
    private Map<String, Object> extReferenceMap = new HashMap<>(); // MMID, Google ID;
}
