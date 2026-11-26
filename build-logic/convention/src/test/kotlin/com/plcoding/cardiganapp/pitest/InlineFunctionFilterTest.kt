package com.plcoding.cardiganapp.pitest

import org.objectweb.asm.tree.ClassNode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InlineFunctionFilterTest {

    private val classes = TestClasses()
    private val facade = classes.compiled(FACADE)

    @Test
    fun `inline function is dropped and its non-inline overload is kept`() {
        val overloads = facade.methods.filter { it.name == "twice" }
        val inline = overloads.single { it.desc != "(I)I" }
        val plain = overloads.single { it.desc == "(I)I" }

        assertEquals(emptyList(), survivors(filter(), facade, listOf(inline)))
        assertEquals(listOf("twice"), survivors(filter(), facade, listOf(plain)))
    }

    @Test
    fun `suspend inline and reified inline functions are dropped, plain functions are kept`() {
        val methods = facade.methods.filter { it.name in setOf("retried", "nameOf", "wrapped", "runSuspend", "plain") }
        assertEquals(5, methods.size)

        assertEquals(setOf("runSuspend", "plain"), survivors(filter(), facade, methods).toSet())
    }

    @Test
    fun `inline function in an object is dropped and its sibling is kept`() {
        val node = classes.compiled("$PACKAGE.InlineObject")
        val methods = node.methods.filter { it.name == "double" || it.name == "triple" }

        assertEquals(listOf("triple"), survivors(filter(), node, methods))
    }

    @Test
    fun `suspend lambda class started by a suspend inline function is dropped`() {
        val enclosed = enclosedClassesOf("wrapped")
        assertTrue(enclosed.isNotEmpty(), "Expected a class enclosed by wrapped")

        enclosed.forEach { node ->
            assertEquals(emptyList(), survivors(filter(), node, node.methods.filter { it.name == "invokeSuspend" }), node.name)
        }
    }

    @Test
    fun `class enclosed by a plain function is kept`() {
        val launch = classNode("${facade.name}\$plain\$1", staticMethod("invokeSuspend", INVOKE_SUSPEND_DESC))
            .enclosedBy(facade.name, "plain", "(I)I")

        assertEquals(listOf("invokeSuspend"), survivors(filter(), launch))
    }

    @Test
    fun `class without Kotlin metadata is kept`() {
        val node = classNode("a/JavaTools", staticMethod("twice", "(Lkotlin/jvm/functions/Function0;)V"))

        assertEquals(listOf("twice"), survivors(filter(), node))
    }

    @Test
    fun `unreadable metadata fails with the class name`() {
        val node = classNode("a/BrokenKt", staticMethod("twice", "()V"))
            .withKotlinMetadata(kind = 99, version = intArrayOf(2, 2, 0))

        val error = assertFailsWith<IllegalStateException> { survivors(filter(), node) }

        assertTrue(error.message!!.contains("a.BrokenKt"), error.message)
    }

    @Test
    fun `factory provides a feature that is off by default`() {
        val feature = InlineFunctionFilterFactory().provides()

        assertEquals(InlineFunctionFilterFactory.FEATURE_NAME.lowercase(), feature.name())
        assertTrue(!feature.isOnByDefault)
    }

    private fun filter() = InlineFunctionFilter(classes.source)

    private fun enclosedClassesOf(function: String): List<ClassNode> {
        val facadeFile = File(javaClass.classLoader.getResource(FACADE.replace('.', '/') + ".class")!!.toURI())
        return facadeFile.parentFile.listFiles()!!
            .filter { it.name.startsWith("InlineFixturesKt\$$function\$") && !it.name.contains("\$\$inlined") }
            .map { classes.compiled("$PACKAGE.${it.name.removeSuffix(".class")}") }
            .filter { it.outerMethod == function }
    }

    private companion object {
        const val PACKAGE = "com.plcoding.cardiganapp.pitest.fixtures"
        const val FACADE = "$PACKAGE.InlineFixturesKt"
    }
}
