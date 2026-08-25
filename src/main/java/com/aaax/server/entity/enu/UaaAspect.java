package com.aaax.server.entity.enu;

import java.util.List;

public interface UaaAspect {
    String PROFILE = "PROFILE";
    String PREFERENCE = "PREFERENCE";
    String DEVICE = "DEVICE";
    String VERIFICATION = "VERIFICATION";
    String USER = "USER";


    List<String> USER_PROFILES = List.of(VERIFICATION);
    List<String> USERS = List.of(PROFILE, PREFERENCE, DEVICE, VERIFICATION, USER);
}
