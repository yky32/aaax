/**
 * AAAX foundation layer (public OSS stand-in for private app-core).
 *
 * <pre>
 * entity/      AuditableEntity — createdAt / updatedAt mapped superclass
 * exception/   BizException — domain/HTTP business errors
 * id/          Ids — UUID helpers
 * web/         Global exception → JSON error body
 * </pre>
 *
 * Domain packages (account, auth, …) depend on core — not the reverse.
 * No Quinsic / private app-core types.
 */
package com.aaax.core;
