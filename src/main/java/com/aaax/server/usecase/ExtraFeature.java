package com.aaax.server.usecase;

import java.util.List;

/** Optional register-time feature flags. Mesh-specific IDV/Onboarding removed for OSS. */
public interface ExtraFeature {
    List<String> ALL = List.of();
}
