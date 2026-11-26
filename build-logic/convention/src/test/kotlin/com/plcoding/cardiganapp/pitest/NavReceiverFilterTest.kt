package com.plcoding.cardiganapp.pitest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavReceiverFilterTest {

    private val classes = TestClasses()

    @Test
    fun `top-level function with each navigation type as first parameter is dropped`() {
        val node = facade(
            "a/AppShellKt",
            staticMethod("openBoardId", "(Landroidx/navigation/NavHostController;)Ljava/lang/String;"),
            staticMethod("popToStart", "(Landroidx/navigation/NavController;)V"),
            staticMethod("isAuthScreen", "(Landroidx/navigation/NavDestination;)Z"),
        )

        assertEquals(emptyList(), survivors(filter(), node))
    }

    @Test
    fun `function whose first parameter is something else is kept`() {
        val node = facade(
            "a/SessionGateKt",
            staticMethod("destinationFor", "(La/SessionState;)Ljava/lang/Object;"),
            staticMethod("label", "(Ljava/lang/String;Landroidx/navigation/NavController;)Ljava/lang/String;"),
        )

        assertEquals(listOf("destinationFor", "label"), survivors(filter(), node))
    }

    @Test
    fun `lambdas of a helper are dropped at any depth`() {
        val node = facade(
            "a/SessionGateKt",
            staticMethod("replaceBackStackWith", "(Landroidx/navigation/NavHostController;Ljava/lang/Object;)V"),
            staticMethod("replaceBackStackWith\$lambda\$0", "(Landroidx/navigation/NavHostController;Landroidx/navigation/NavOptionsBuilder;)Lkotlin/Unit;"),
            staticMethod("replaceBackStackWith\$lambda\$0\$0", "(Landroidx/navigation/PopUpToBuilder;)Lkotlin/Unit;"),
        )

        assertEquals(emptyList(), survivors(filter(), node))
    }

    @Test
    fun `lambda of a sibling whose name starts with the helper's name is kept`() {
        val node = facade(
            "a/AppShellKt",
            staticMethod("openBoard", "(Landroidx/navigation/NavController;)V"),
            staticMethod("openBoardTitle", "(Ljava/lang/String;)Ljava/lang/String;"),
            staticMethod("openBoardTitle\$lambda\$0", "()Lkotlin/Unit;"),
        )

        assertEquals(listOf("openBoardTitle", "openBoardTitle\$lambda\$0"), survivors(filter(), node))
    }

    @Test
    fun `class enclosed by a helper is dropped`() {
        val desc = "(Landroidx/navigation/NavController;)V"
        classes.register(facade("a/AppShellKt", staticMethod("observe", desc)))
        val enclosed = classNode("a/AppShellKt\$observe\$1", instanceMethod("invokeSuspend", INVOKE_SUSPEND_DESC))
            .enclosedBy("a/AppShellKt", "observe", desc)

        assertEquals(emptyList(), survivors(filter(), enclosed))
    }

    @Test
    fun `member extension in an object is kept`() {
        val node = classNode(
            "a/Navigator",
            instanceMethod("openBoard", "(Landroidx/navigation/NavController;Ljava/lang/String;)V"),
        ).withKotlinMetadata(KotlinMetadataReader.KIND_CLASS)

        assertEquals(listOf("openBoard"), survivors(filter(), node))
    }

    @Test
    fun `static method of a class that is not a file facade is kept`() {
        val node = classNode("a/Navigator", staticMethod("openBoard", "(Landroidx/navigation/NavController;)V"))
            .withKotlinMetadata(KotlinMetadataReader.KIND_CLASS)

        assertEquals(listOf("openBoard"), survivors(filter(), node))
    }

    @Test
    fun `factory provides a feature that is off by default`() {
        val feature = NavReceiverFilterFactory().provides()

        assertEquals(NavReceiverFilterFactory.FEATURE_NAME.lowercase(), feature.name())
        assertTrue(!feature.isOnByDefault)
    }

    private fun filter() = NavReceiverFilter(classes.source)

    private fun facade(name: String, vararg methods: org.objectweb.asm.tree.MethodNode) =
        classNode(name, *methods).withKotlinMetadata(KotlinMetadataReader.KIND_FILE_FACADE)
}
