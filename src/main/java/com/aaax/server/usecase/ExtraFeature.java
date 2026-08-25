package com.aaax.server.usecase;

import java.util.List;

public interface ExtraFeature {
    String ONBOARDING = "ONBOARDING";
    String IDV = "IDV";
    List<String> ALL = List.of(ONBOARDING, IDV);
}
