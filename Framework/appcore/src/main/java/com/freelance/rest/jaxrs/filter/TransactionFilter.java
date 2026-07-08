package com.freelance.rest.jaxrs.filter;

import com.freelance.rest.app.transaction.Transaction;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS filter that binds the transaction lifecycle to each HTTP request:
 * starts a transaction when a request arrives, and finishes it (commit or
 * rollback) once the response is ready to be sent.
 */
@Provider
public class TransactionFilter implements ContainerRequestFilter, ContainerResponseFilter {

    /**
     * Starts a new transaction for the incoming request.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        Transaction.start();
    }

    /**
     * Finalizes the transaction once the response has been produced.
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Transaction.finish();
    }
}