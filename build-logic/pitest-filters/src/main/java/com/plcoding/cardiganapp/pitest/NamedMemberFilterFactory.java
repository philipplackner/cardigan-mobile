package com.plcoding.cardiganapp.pitest;

import org.pitest.mutationtest.build.InterceptorParameters;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationInterceptorFactory;
import org.pitest.plugin.Feature;
import org.pitest.plugin.FeatureParameter;

/**
 * Registers {@link NamedMemberFilter} with PIT through {@code META-INF/services}. Off unless enabled
 * with {@code +FNAMEDEXCLUDE(member[ClassGlob#methodGlob] ...)}.
 */
public final class NamedMemberFilterFactory implements MutationInterceptorFactory {

    public static final String FEATURE_NAME = "FNAMEDEXCLUDE";

    static final FeatureParameter MEMBER = FeatureParameter.named("member")
            .withDescription("A ClassGlob#methodGlob entry whose mutants are dropped");

    @Override
    public String description() {
        return "Named member filter";
    }

    @Override
    public Feature provides() {
        return Feature.named(FEATURE_NAME)
                .withOnByDefault(false)
                .withDescription("Filters mutations in members named by ClassGlob#methodGlob entries")
                .withParameter(MEMBER);
    }

    @Override
    public MutationInterceptor createInterceptor(InterceptorParameters params) {
        return new NamedMemberFilter(params.getList(MEMBER));
    }
}
