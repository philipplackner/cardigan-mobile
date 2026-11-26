package com.plcoding.cardiganapp.pitest;

import org.objectweb.asm.ClassReader;
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
 * Drops mutants in a set of root methods, in methods whose name starts with a child prefix of a
 * root (lambdas, local functions), and in every class whose enclosing method is one of these,
 * directly or through outer classes. Subclasses decide which methods are roots and which name
 * prefixes belong to them.
 */
abstract class EnclosedCodeFilter implements MutationInterceptor {

    private static final int MAX_NESTING = 32;

    private final ClassByteArraySource source;
    private final Map<String, Optional<ClassNode>> loaded = new HashMap<>();
    private final Map<String, Set<String>> excludedByClass = new HashMap<>();

    private boolean skipClass;
    private Set<String> excluded = Collections.emptySet();

    EnclosedCodeFilter(ClassByteArraySource source) {
        this.source = source;
    }

    /** Name plus descriptor of every root method in {@code node}. */
    abstract Set<String> rootKeys(ClassNode node);

    /** Method name prefixes of code declared inside the root method {@code rootName}. */
    abstract List<String> childPrefixes(String rootName);

    @Override
    public final InterceptorType type() {
        return InterceptorType.FILTER;
    }

    @Override
    public final void begin(ClassTree clazz) {
        ClassNode node = clazz.rawNode();
        skipClass = isEnclosed(node, 0);
        excluded = skipClass ? Collections.emptySet() : excludedKeys(node);
    }

    @Override
    public final Collection<MutationDetails> intercept(Collection<MutationDetails> mutations, Mutater mutater) {
        if (skipClass) {
            return Collections.emptyList();
        }
        if (excluded.isEmpty()) {
            return mutations;
        }
        return mutations.stream()
                .filter(mutation -> !excluded.contains(key(mutation.getId().getLocation())))
                .collect(Collectors.toList());
    }

    @Override
    public final void end() {
        skipClass = false;
        excluded = Collections.emptySet();
    }

    private Set<String> excludedKeys(ClassNode node) {
        Set<String> cached = excludedByClass.get(node.name);
        if (cached != null) {
            return cached;
        }
        Set<String> keys = computeExcludedKeys(node);
        excludedByClass.put(node.name, keys);
        return keys;
    }

    private Set<String> computeExcludedKeys(ClassNode node) {
        Set<String> roots = rootKeys(node);
        if (roots.isEmpty()) {
            return roots;
        }
        List<String> prefixes = new ArrayList<>();
        for (String root : roots) {
            prefixes.addAll(childPrefixes(root.substring(0, root.indexOf('('))));
        }
        Set<String> keys = new HashSet<>(roots);
        for (MethodNode method : node.methods) {
            for (String prefix : prefixes) {
                if (method.name.startsWith(prefix)) {
                    keys.add(key(method.name, method.desc));
                    break;
                }
            }
        }
        return keys;
    }

    private boolean isEnclosed(ClassNode node, int depth) {
        if (node.outerClass == null || depth > MAX_NESTING) {
            return false;
        }
        Optional<ClassNode> outer = load(node.outerClass);
        if (!outer.isPresent()) {
            return false;
        }
        if (node.outerMethod != null
                && excludedKeys(outer.get()).contains(key(node.outerMethod, node.outerMethodDesc))) {
            return true;
        }
        return isEnclosed(outer.get(), depth + 1);
    }

    private Optional<ClassNode> load(String internalName) {
        return loaded.computeIfAbsent(internalName, name ->
                source.getBytes(name.replace('/', '.')).map(bytes -> {
                    ClassNode node = new ClassNode();
                    new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
                    return node;
                }));
    }

    /** Kotlin replaces '-' from mangled names (value class parameters) with '_' in the names of nested code. */
    static List<String> withMangledForm(String rootName, String suffix) {
        String plain = rootName + suffix;
        String mangled = rootName.replace('-', '_') + suffix;
        return plain.equals(mangled) ? List.of(plain) : List.of(plain, mangled);
    }

    static String key(Location location) {
        return key(location.getMethodName(), location.getMethodDesc());
    }

    static String key(String name, String desc) {
        return name + desc;
    }
}
