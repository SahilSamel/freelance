package com.freelance.rest.jaxrs.filter;

import com.freelance.rest.app.transaction.Transaction;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TransactionFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String threadName = Thread.currentThread().getName();
        System.out.println("\n[TX-FILTER] >>> REQUEST ENTERING [Thread: " + threadName + "]");
        System.out.println("[TX-FILTER] Path: " + requestContext.getUriInfo().getPath());

        Transaction.start();
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String threadName = Thread.currentThread().getName();
        System.out.println("[TX-FILTER] <<< RESPONSE EXITING [Thread: " + threadName + "]");
        System.out.println("[TX-FILTER] Status: " + responseContext.getStatus() + "\n");

        Transaction.finish();
    }
}
