package com.aaax.server.config.security;

import com.aaax.server.entity.po.user.UserPrincipal;
import com.aaax.server.service.UaaService;
import com.aaax.server.validation.UaaValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    @Autowired
    private UaaService uaaService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String canonical = UaaValidation.toCanonicalIdentifierIfPresent(username);
        return UserPrincipal.create(uaaService.getByUsername(canonical != null ? canonical : username));
    }
}