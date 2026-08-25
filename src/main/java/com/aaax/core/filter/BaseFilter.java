package com.aaax.core.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * this is the filter to manage [microservice] related issue. example,
 * 1. x-request-id - for global traceid
 * 2. x-app-id ??
 */
@Slf4j
public class BaseFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // if (newStr4.matches("(Mon|Tues|Wed|Thurs|Fri).*"))
        return request.getServletPath().matches("(/actuator|/swagger-ui|/v3/api-docs).*");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        log.info("-------------- statistic log: {} start --------------", this.getClass().getSimpleName());
        chain.doFilter(request, response);
        log.info("-------------- statistic log: {} start --------------", this.getClass().getSimpleName());
    }
}