package com.aaax.server.usecase;

import com.aaax.server.repository.UserPermissionRepository;
import com.aaax.server.service.UaaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserPermissionUseCase {

    private final UserPermissionRepository userPermissionRepository;
    private final UaaService uaaService;
    private final ResourceLoader resourceLoader;
}
