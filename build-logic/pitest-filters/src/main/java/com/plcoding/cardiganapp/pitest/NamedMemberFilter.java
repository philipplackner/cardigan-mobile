package com.plcoding.cardiganapp.pitest;

import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.util.Glob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Drops mutants in members matched by {@code ClassGlob#methodGlob} entries. The class glob matches
 * the binary class name ({@code com.example.Outer$Inner}), the method glob the JVM method name.
 * Unlike PIT's {@code excludedMethods}, a method glob only applies to the classes its entry names.
 */
final class NamedMemberFilter implements MutationInterceptor {

    private final List<Member> members;

    NamedMemberFilter(List<String> entries) {
        members = new ArrayList<>();
        for (String entry : entries) {
            int separator = entry.indexOf('#');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw new IllegalArgumentException(
                        "Named member exclusion must look like ClassGlob#methodGlob, got: " + entry);
            }
            members.add(new Member(new Glob(entry.substring(0, separator)), new Glob(entry.substring(separator + 1))));
        }
    }

    @Override
    public InterceptorType type() {
        return InterceptorType.FILTER;
    }

    @Override
    public void begin(ClassTree clazz) {
    }

    @Override
    public Collection<MutationDetails> intercept(Collection<MutationDetails> mutations, Mutater mutater) {
        if (members.isEmpty()) {
            return mutations;
        }
        return mutations.stream()
                .filter(mutation -> !matches(mutation.getId().getLocation()))
                .collect(Collectors.toList());
    }

    @Override
    public void end() {
    }

    private boolean matches(Location location) {
        String className = location.getClassName().asJavaName();
        String methodName = location.getMethodName();
        for (Member member : members) {
            if (member.className.matches(className) && member.methodName.matches(methodName)) {
                return true;
            }
        }
        return false;
    }

    private record Member(Glob className, Glob methodName) {
    }
}
