package com.plcoding.cardiganapp.pitest;

import kotlin.Metadata;
import kotlin.metadata.Attributes;
import kotlin.metadata.KmFunction;
import kotlin.metadata.jvm.JvmExtensionsKt;
import kotlin.metadata.jvm.JvmMetadataUtil;
import kotlin.metadata.jvm.JvmMethodSignature;
import kotlin.metadata.jvm.KotlinClassMetadata;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Reads the {@code kotlin.Metadata} annotation of a class from its bytecode. */
final class KotlinMetadataReader {

    static final int KIND_CLASS = 1;
    static final int KIND_FILE_FACADE = 2;
    static final int KIND_MULTI_FILE_CLASS_PART = 5;

    private static final String METADATA = "Lkotlin/Metadata;";

    private KotlinMetadataReader() {
    }

    /** The metadata kind ({@code k}), or empty for a class without Kotlin metadata. */
    static Optional<Integer> kind(ClassNode node) {
        return annotation(node).map(annotation -> {
            Object kind = value(annotation, "k");
            return kind == null ? KIND_CLASS : (Integer) kind;
        });
    }

    /**
     * Name plus descriptor of every {@code inline} function declared in {@code node}. Empty for a
     * class without Kotlin metadata.
     *
     * @throws IllegalStateException if the metadata can't be read, so a run never silently treats
     *     a class as having no inline functions
     */
    static Set<String> inlineFunctionKeys(ClassNode node) {
        Optional<AnnotationNode> annotation = annotation(node);
        if (!annotation.isPresent()) {
            return Collections.emptySet();
        }
        Metadata metadata = toMetadata(annotation.get());
        KotlinClassMetadata parsed;
        try {
            parsed = KotlinClassMetadata.readLenient(metadata);
        } catch (RuntimeException e) {
            throw unreadable(node, metadata, e);
        }
        List<KmFunction> functions;
        if (parsed instanceof KotlinClassMetadata.Class kotlinClass) {
            functions = kotlinClass.getKmClass().getFunctions();
        } else if (parsed instanceof KotlinClassMetadata.FileFacade facade) {
            functions = facade.getKmPackage().getFunctions();
        } else if (parsed instanceof KotlinClassMetadata.MultiFileClassPart part) {
            functions = part.getKmPackage().getFunctions();
        } else if (parsed instanceof KotlinClassMetadata.SyntheticClass
                || parsed instanceof KotlinClassMetadata.MultiFileClassFacade) {
            return Collections.emptySet();
        } else {
            throw unreadable(node, metadata, null);
        }
        Set<String> keys = new HashSet<>();
        for (KmFunction function : functions) {
            if (!Attributes.isInline(function)) {
                continue;
            }
            JvmMethodSignature signature = JvmExtensionsKt.getSignature(function);
            if (signature != null) {
                keys.add(EnclosedCodeFilter.key(signature.getName(), signature.getDescriptor()));
            }
        }
        return keys;
    }

    private static IllegalStateException unreadable(ClassNode node, Metadata metadata, RuntimeException cause) {
        return new IllegalStateException(
                "Can't read the Kotlin metadata of " + node.name.replace('/', '.')
                        + " (metadata version " + Arrays.toString(metadata.mv()) + ")",
                cause);
    }

    private static Optional<AnnotationNode> annotation(ClassNode node) {
        for (List<AnnotationNode> annotations : Arrays.asList(node.visibleAnnotations, node.invisibleAnnotations)) {
            if (annotations == null) {
                continue;
            }
            for (AnnotationNode annotation : annotations) {
                if (METADATA.equals(annotation.desc)) {
                    return Optional.of(annotation);
                }
            }
        }
        return Optional.empty();
    }

    private static Metadata toMetadata(AnnotationNode annotation) {
        return JvmMetadataUtil.Metadata(
                (Integer) value(annotation, "k"),
                toIntArray(value(annotation, "mv")),
                toStringArray(value(annotation, "d1")),
                toStringArray(value(annotation, "d2")),
                (String) value(annotation, "xs"),
                (String) value(annotation, "pn"),
                (Integer) value(annotation, "xi"));
    }

    private static Object value(AnnotationNode annotation, String name) {
        if (annotation.values == null) {
            return null;
        }
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (name.equals(annotation.values.get(i))) {
                return annotation.values.get(i + 1);
            }
        }
        return null;
    }

    private static int[] toIntArray(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof int[] array) {
            return array;
        }
        List<?> list = (List<?>) value;
        int[] result = new int[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = (Integer) list.get(i);
        }
        return result;
    }

    private static String[] toStringArray(Object value) {
        if (value == null) {
            return null;
        }
        return ((List<?>) value).toArray(new String[0]);
    }
}
