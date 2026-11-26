package com.plcoding.cardiganapp.pitest;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.pitest.classinfo.ClassByteArraySource;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Drops mutants in top-level functions whose first JVM parameter is a {@code NavController},
 * {@code NavHostController} or {@code NavDestination}, in their lambdas and in the classes they
 * enclose. An extension receiver compiles to the first parameter, so extensions are matched too.
 * Only UI tests can run these helpers. Functions in classes and objects are not matched.
 */
final class NavReceiverFilter extends EnclosedCodeFilter {

    static final List<String> NAV_PARAMETERS = List.of(
            "(Landroidx/navigation/NavController;",
            "(Landroidx/navigation/NavHostController;",
            "(Landroidx/navigation/NavDestination;");

    NavReceiverFilter(ClassByteArraySource source) {
        super(source);
    }

    @Override
    Set<String> rootKeys(ClassNode node) {
        if (!isFileFacade(node)) {
            return Collections.emptySet();
        }
        Set<String> keys = new HashSet<>();
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) != 0 && hasNavFirstParameter(method.desc)) {
                keys.add(key(method.name, method.desc));
            }
        }
        return keys;
    }

    @Override
    List<String> childPrefixes(String rootName) {
        return withMangledForm(rootName, "$lambda");
    }

    private static boolean isFileFacade(ClassNode node) {
        return KotlinMetadataReader.kind(node)
                .map(kind -> kind == KotlinMetadataReader.KIND_FILE_FACADE
                        || kind == KotlinMetadataReader.KIND_MULTI_FILE_CLASS_PART)
                .orElse(false);
    }

    private static boolean hasNavFirstParameter(String desc) {
        for (String parameter : NAV_PARAMETERS) {
            if (desc.startsWith(parameter)) {
                return true;
            }
        }
        return false;
    }
}
