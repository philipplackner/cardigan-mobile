package com.plcoding.cardiganapp.pitest

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.pitest.bytecode.analysis.ClassTree
import org.pitest.classinfo.ClassByteArraySource
import org.pitest.classinfo.ClassName
import org.pitest.mutationtest.build.MutationInterceptor
import org.pitest.mutationtest.engine.Location
import org.pitest.mutationtest.engine.MutationDetails
import org.pitest.mutationtest.engine.MutationIdentifier
import java.util.Optional

/** Classes the filters can load, from synthetic nodes or from the test classpath. */
internal class TestClasses {
    private val registered = mutableMapOf<String, ByteArray>()

    val source =
        ClassByteArraySource { name ->
            Optional.ofNullable(registered[name] ?: compiledBytes(name))
        }

    fun register(node: ClassNode) {
        val writer = ClassWriter(0)
        node.accept(writer)
        registered[node.name.replace('/', '.')] = writer.toByteArray()
    }

    fun compiled(binaryName: String): ClassNode {
        val bytes = requireNotNull(compiledBytes(binaryName)) { "No class file for $binaryName" }
        return ClassNode().also { ClassReader(bytes).accept(it, 0) }
    }

    private fun compiledBytes(binaryName: String): ByteArray? =
        javaClass.classLoader.getResourceAsStream(binaryName.replace('.', '/') + ".class")?.use { it.readBytes() }
}

/** Runs [filter] over one mutant per method of [node] and returns the names of the methods whose mutants are left. */
internal fun survivors(
    filter: MutationInterceptor,
    node: ClassNode,
    methods: List<MethodNode> = node.methods,
): List<String> {
    val mutations = methods.mapIndexed { index, method -> mutation(node.name, method.name, method.desc, index) }
    filter.begin(ClassTree(node))
    val result = filter.intercept(mutations, null).map { it.method }
    filter.end()
    return result
}

internal fun mutation(
    owner: String,
    name: String,
    desc: String,
    index: Int = 0,
): MutationDetails {
    val location = Location.location(ClassName.fromString(owner), name, desc)
    return MutationDetails(MutationIdentifier(location, index, "mutator"), "File.kt", "description", 1, 0)
}

internal fun classNode(
    name: String,
    vararg methods: MethodNode,
) = ClassNode().apply {
    version = Opcodes.V17
    access = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL
    this.name = name
    superName = "java/lang/Object"
    this.methods.addAll(methods)
}

internal fun ClassNode.enclosedBy(
    owner: String,
    method: String,
    desc: String,
) = apply {
    outerClass = owner
    outerMethod = method
    outerMethodDesc = desc
}

internal fun ClassNode.withKotlinMetadata(
    kind: Int,
    version: IntArray = intArrayOf(2, 2, 0),
    data: List<String> = emptyList(),
) = apply {
    val annotation = AnnotationNode("Lkotlin/Metadata;")
    annotation.values = listOf("k", kind, "mv", version.toList(), "d1", data, "d2", emptyList<String>())
    visibleAnnotations = listOf(annotation)
}

internal fun staticMethod(
    name: String,
    desc: String,
) = MethodNode(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_ABSTRACT, name, desc, null, null)

internal fun instanceMethod(
    name: String,
    desc: String,
) = MethodNode(Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT, name, desc, null, null)

internal const val INVOKE_SUSPEND_DESC = "(Ljava/lang/Object;)Ljava/lang/Object;"
