package com.aaax.exception.response;


import com.aaax.core.response.Response;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface TenantRoleWithRouteErrorResponse {
    Response TRR0001 = new Response("TRR0001", "Tenant Role Route Record not found.");
}
