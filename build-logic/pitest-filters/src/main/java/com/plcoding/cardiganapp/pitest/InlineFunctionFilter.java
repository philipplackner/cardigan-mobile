package com.plcoding.cardiganapp.pitest;

import org.objectweb.asm.tree.ClassNode;
import org.pitest.classinfo.ClassByteArraySource;

import java.util.List;
import java.util.Set;

/**
 * Drops mutants in Kotlin {@code inline} functions, in lambdas declared in them and in classes they
 * enclose. Callers run their own inlined copy, so host tests never run the original body and
 * can't kill a mutant there. Inline functions are found through Kotlin metadata and matched by JVM
 * signature, so a non-inline overload with the same name stays in.
 */
final class InlineFunctionFilter extends EnclosedCodeFilter {

    InlineFunctionFilter(ClassByteArraySource source) {
        super(source);
    }

    @Override
    Set<String> rootKeys(ClassNode node) {
        return KotlinMetadataReader.inlineFunctionKeys(node);
    }

    @Override
    List<String> childPrefixes(String rootName) {
        return withMangledForm(rootName, "$lambda");
    }
}
