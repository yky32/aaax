/**
 * Persistence and API shapes — qs/uaa layout:
 * <pre>
 * po/              JPA entities (extend AuditEntity*)
 * dto/request/     *RequestDto
 * dto/response/    Get*|…*ResponseDto
 * dto/event/       bus/payload DTOs
 * </pre>
 * No bag classes. Records OK. Naming ends with RequestDto / ResponseDto.
 */
package com.aaax.entity;
