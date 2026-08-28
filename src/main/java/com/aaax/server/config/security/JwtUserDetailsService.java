package com.aaax.server.config.security;

import com.aaax.server.entity.po.user.UserPrincipal;
import com.aaax.server.service.AaaxService;
import com.aaax.server.validation.AaaxValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    @Autowired
    private AaaxService aaaxService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String canonical = AaaxValidation.toCanonicalIdentifierIfPresent(username);
        return UserPrincipal.create(aaaxService.getByUsername(canonical != null ? canonical : username));
    }
}