package com.plcoding.cardiganapp.pitest;

import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.plugin.Feature;

/**
 * Registers {@link ComposeUiFilter} with PIT through {@code META-INF/services}. The feature is on by
 * default, so putting this jar on PIT's classpath is enough to activate it.
 */
public final class ComposeUiFilterFactory implements MutationInterceptorFactory {

    public static final String FEATURE_NAME = "FCOMPOSEUI";

    @Override
    public String description() {
        return "Compose UI filter";
    }

    @Override
    public Feature provides() {
        return Feature.named(FEATURE_NAME)
                .withOnByDefault(true)
                .withDescription(
                        "Filters mutations in @Composable functions, Modifier-returning functions, "
                                + "the lambdas declared in them and the classes they enclose");
    }

    @Override
    public MutationInterceptor createInterceptor(InterceptorParameters params) {
        return new ComposeUiFilter(params.source());
    }
}
