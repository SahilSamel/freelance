package com.freelance.rest.jaxrs.filter;

import com.freelance.rest.app.transaction.Transaction;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global JAX-RS filter coordinating request-scoped database transaction boundaries and context lifecycle.
 * <p>
 * Configured with {@code @Priority(2)} so that it executes before the system requires database access
 * or needs thread-local request context storage to provide contextual data to downstream services.
 * </p>
 */
@Provider
@Priority(2)
public class TransactionFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String CLASS_NAME = TransactionFilter.class.getSimpleName();

    private static final Logger logger = LoggerFactory.getLogger(TransactionFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Transaction.start((String) requestContext.getProperty(TenantFilter.TENANT_PROPERTY));
        logger.debug("Transaction started for on {}", requestContext.getUriInfo());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Transaction.finish();
        logger.debug("Transaction closed for {}", requestContext.getUriInfo());
    }
}