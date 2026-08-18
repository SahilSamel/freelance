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
 * JAX-RS filter that binds the transaction lifecycle to each HTTP request:
 * starts a transaction when a request arrives, and finishes it (commit or
 * rollback) once the response is ready to be sent.
 */
@Provider
@Priority(2)
public class TransactionFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String CLASS_NAME = TransactionFilter.class.getSimpleName();

    private static final Logger logger = LoggerFactory.getLogger(TransactionFilter.class);

    /**
     * Starts a new transaction for the incoming request.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        Transaction.start();
        logger.debug("Transaction started for {}", requestContext.getUriInfo());
    }

    /**
     * Finalizes the transaction once the response has been produced.
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Transaction.finish();
        logger.debug("Transaction closed for {}", requestContext.getUriInfo());
    }
}