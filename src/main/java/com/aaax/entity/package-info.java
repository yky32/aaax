/**
 * Persistence and API shapes — <b>layer-first</b>, qs/uaa neatness.
 *
 * <pre>
 * po/              JPA only (@Entity extends AuditEntity*)
 * model/           non-persistent domain types (e.g. QR session)
 * dto/request/     *RequestDto
 * dto/response/    Get*|…*ResponseDto  (+ core BaseResponseDto for audit)
 * dto/event/       bus payloads
 * </pre>
 *
 * One type per file. No bag classes. Records OK.
 */
package com.aaax.entity;
