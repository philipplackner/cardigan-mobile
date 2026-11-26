package com.plcoding.cardiganapp.pitest;

import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.plugin.Feature;

/** Registers {@link NavReceiverFilter} with PIT through {@code META-INF/services}. Off unless enabled with {@code +FNAVRECEIVER}. */
public final class NavReceiverFilterFactory implements MutationInterceptorFactory {

    public static final String FEATURE_NAME = "FNAVRECEIVER";

    @Override
    public String description() {
        return "Navigation receiver filter";
    }

    @Override
    public Feature provides() {
        return Feature.named(FEATURE_NAME)
                .withOnByDefault(false)
                .withDescription("Filters mutations in top-level functions whose first parameter is a NavController, NavHostController or NavDestination, their lambdas and the classes they enclose");
    }

    @Override
    public MutationInterceptor createInterceptor(InterceptorParameters params) {
        return new NavReceiverFilter(params.source());
    }
}
