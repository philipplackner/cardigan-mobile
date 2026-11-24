package com.plcoding.cardiganapp.pitest;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.mutationtest.build.InterceptorType;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.Mutater;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Drops mutants in Compose UI code, which Android host tests can't run:
 * <ul>
 *   <li>methods annotated with {@code @Composable} (binary retention, so read from the bytecode),</li>
 *   <li>methods that return {@code Modifier} (custom modifiers and draw helpers),</li>
 *   <li>lambda methods declared in either of them ({@code <name>$lambda...}, Kotlin's indy lambda naming),</li>
 *   <li>classes whose enclosing method is one of the above, directly or through outer classes,
 *       such as suspend lambdas started by {@code LaunchedEffect}.</li>
 * </ul>
 */
final class ComposeUiFilter implements MutationInterceptor {

    static final String COMPOSABLE = "Landroidx/compose/runtime/Composable;";
    static final String MODIFIER_RETURN = ")Landroidx/compose/ui/Modifier;";

    private static final String LAMBDA_MARKER = "$lambda";
    private static final int MAX_NESTING = 32;

    private final ClassByteArraySource source;
    private final Map<String, Optional<ClassNode>> loaded = new HashMap<>();

    private boolean skipClass;
    private Set<String> uiMethods = Collections.emptySet();

    ComposeUiFilter(ClassByteArraySource source) {
        this.source = source;
    }

    @Override
    public InterceptorType type() {
        return InterceptorType.FILTER;
    }

    @Override
    public void begin(ClassTree clazz) {
        ClassNode node = clazz.rawNode();
        skipClass = isEnclosedByUi(node, 0);
        uiMethods = skipClass ? Collections.emptySet() : uiMethodKeys(node);
    }

    @Override
    public Collection<MutationDetails> intercept(Collection<MutationDetails> mutations, Mutater mutater) {
        if (skipClass) {
            return Collections.emptyList();
        }
        if (uiMethods.isEmpty()) {
            return mutations;
        }
        return mutations.stream()
                .filter(mutation -> !uiMethods.contains(key(mutation.getId().getLocation())))
                .collect(Collectors.toList());
    }

    @Override
    public void end() {
        skipClass = false;
        uiMethods = Collections.emptySet();
    }

    /** Name plus descriptor of every UI method in {@code node} and of every lambda declared in one. */
    static Set<String> uiMethodKeys(ClassNode node) {
        List<String> lambdaPrefixes = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (MethodNode method : node.methods) {
            if (isUiRoot(method)) {
                keys.add(key(method.name, method.desc));
                // Kotlin replaces '-' from mangled names (value class parameters) with '_' in lambda names.
                lambdaPrefixes.add(method.name.replace('-', '_') + LAMBDA_MARKER);
            }
        }
        if (lambdaPrefixes.isEmpty()) {
            return keys;
        }
        for (MethodNode method : node.methods) {
            for (String prefix : lambdaPrefixes) {
                if (method.name.startsWith(prefix)) {
                    keys.add(key(method.name, method.desc));
                    break;
                }
            }
        }
        return keys;
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

    private boolean isEnclosedByUi(ClassNode node, int depth) {
        if (node.outerClass == null || depth > MAX_NESTING) {
            return false;
        }
        Optional<ClassNode> outer = load(node.outerClass);
        if (!outer.isPresent()) {
            return false;
        }
        if (node.outerMethod != null
                && uiMethodKeys(outer.get()).contains(key(node.outerMethod, node.outerMethodDesc))) {
            return true;
        }
        return isEnclosedByUi(outer.get(), depth + 1);
    }

    private Optional<ClassNode> load(String internalName) {
        return loaded.computeIfAbsent(internalName, name ->
                source.getBytes(name.replace('/', '.')).map(bytes -> {
                    ClassNode node = new ClassNode();
                    new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
                    return node;
                }));
    }

    private static String key(Location location) {
        return key(location.getMethodName(), location.getMethodDesc());
    }

    private static String key(String name, String desc) {
        return name + desc;
    }
}
