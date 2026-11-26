package com.plcoding.cardiganapp.pitest;

import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.plugin.Feature;

/** Registers {@link InlineFunctionFilter} with PIT through {@code META-INF/services}. Off unless enabled with {@code +FKOTLININLINE}. */
public final class InlineFunctionFilterFactory implements MutationInterceptorFactory {

    public static final String FEATURE_NAME = "FKOTLININLINE";

    @Override
    public String description() {
        return "Kotlin inline function filter";
    }

    @Override
    public Feature provides() {
        return Feature.named(FEATURE_NAME)
                .withOnByDefault(false)
                .withDescription("Filters mutations in Kotlin inline functions, the lambdas declared in them and the classes they enclose");
    }

    @Override
    public MutationInterceptor createInterceptor(InterceptorParameters params) {
        return new InlineFunctionFilter(params.source());
    }
}
