package com.plcoding.cardiganapp.pitest;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.pitest.classinfo.ClassByteArraySource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Drops mutants in Compose UI code, which Android host tests can't run:
 * <ul>
 *   <li>methods annotated with {@code @Composable} (binary retention, so read from the bytecode),</li>
 *   <li>methods that return {@code Modifier} (custom modifiers and draw helpers),</li>
 *   <li>lambdas and local functions declared in either of them ({@code <name>$lambda...} and
 *       {@code <name>$<localName>}, Kotlin's naming for both),</li>
 *   <li>classes whose enclosing method is one of the above, directly or through outer classes,
 *       such as suspend lambdas started by {@code LaunchedEffect}.</li>
 * </ul>
 */
final class ComposeUiFilter extends EnclosedCodeFilter {

    static final String COMPOSABLE = "Landroidx/compose/runtime/Composable;";
    static final String MODIFIER_RETURN = ")Landroidx/compose/ui/Modifier;";

    ComposeUiFilter(ClassByteArraySource source) {
        super(source);
    }

    @Override
    Set<String> rootKeys(ClassNode node) {
        Set<String> keys = new HashSet<>();
        for (MethodNode method : node.methods) {
            if (isUiRoot(method)) {
                keys.add(key(method.name, method.desc));
            }
        }
        return keys;
    }

    @Override
    List<String> childPrefixes(String rootName) {
        return withMangledForm(rootName, "$");
    }

    static boolean isUiRoot(MethodNode method) {
        return hasComposable(method.invisibleAnnotations)
                || hasComposable(method.visibleAnnotations)
                || method.desc.endsWith(MODIFIER_RETURN);
    }

    private static boolean hasComposable(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode annotation : annotations) {
            if (COMPOSABLE.equals(annotation.desc)) {
                return true;
            }
        }
        return false;
    }
}
