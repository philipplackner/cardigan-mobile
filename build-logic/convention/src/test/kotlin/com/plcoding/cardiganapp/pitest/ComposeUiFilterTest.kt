package com.plcoding.cardiganapp.pitest

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.pitest.bytecode.analysis.ClassTree
import org.pitest.classinfo.ClassByteArraySource
import org.pitest.classinfo.ClassName
import org.pitest.mutationtest.engine.Location
import org.pitest.mutationtest.engine.MutationDetails
import org.pitest.mutationtest.engine.MutationIdentifier
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeUiFilterTest {

    private val classes = mutableMapOf<String, ByteArray>()
    private val source = ClassByteArraySource { name -> Optional.ofNullable(classes[name]) }

    @Test
    fun `composable function is dropped and plain function is kept`() {
        val node = classNode(
            "a/ScreenKt",
            method("Screen", COMPOSABLE_DESC, composable = true),
            method("formatTitle", "(Ljava/lang/String;)Ljava/lang/String;"),
        )

        assertEquals(listOf("formatTitle"), survivors(node, "Screen" to COMPOSABLE_DESC, "formatTitle" to "(Ljava/lang/String;)Ljava/lang/String;"))
    }

    @Test
    fun `visible Composable annotation counts too`() {
        val node = classNode("a/ScreenKt", method("Screen", COMPOSABLE_DESC, composable = true, visible = true))

        assertEquals(emptyList(), survivors(node, "Screen" to COMPOSABLE_DESC))
    }

    @Test
    fun `lambdas of a composable are dropped at any depth`() {
        val node = classNode(
            "a/ScreenKt",
            method("Screen", COMPOSABLE_DESC, composable = true),
            method("Screen\$lambda\$0", "()Lkotlin/Unit;"),
            method("Screen\$lambda\$1\$0\$2", "(Ljava/lang/String;)Lkotlin/Unit;"),
        )

        val result = survivors(node, "Screen\$lambda\$0" to "()Lkotlin/Unit;", "Screen\$lambda\$1\$0\$2" to "(Ljava/lang/String;)Lkotlin/Unit;")

        assertEquals(emptyList(), result)
    }

    @Test
    fun `lambda of a sibling whose name starts with the composable's name is kept`() {
        val node = classNode(
            "a/LabelsKt",
            method("Label", COMPOSABLE_DESC, composable = true),
            method("LabelText", "(Ljava/lang/String;)Ljava/lang/String;"),
            method("LabelText\$lambda\$0", "()Lkotlin/Unit;"),
        )

        assertEquals(listOf("LabelText\$lambda\$0"), survivors(node, "LabelText\$lambda\$0" to "()Lkotlin/Unit;"))
    }

    @Test
    fun `local function of a composable and the class it encloses are dropped`() {
        val localDesc = "(Lkotlinx/coroutines/CoroutineScope;Landroidx/compose/foundation/lazy/LazyListState;I)V"
        val root = classNode(
            "a/BoardRootKt",
            method("BoardRoot", COMPOSABLE_DESC, composable = true),
            method("BoardRoot\$scrollToItem", localDesc),
        )
        register(root)
        val launched = classNode("a/BoardRootKt\$BoardRoot\$scrollToItem\$1", method("invokeSuspend", INVOKE_SUSPEND_DESC))
            .enclosedBy("a/BoardRootKt", "BoardRoot\$scrollToItem", localDesc)

        assertEquals(emptyList(), survivors(root, "BoardRoot\$scrollToItem" to localDesc))
        assertEquals(emptyList(), survivors(launched, "invokeSuspend" to INVOKE_SUSPEND_DESC))
    }

    @Test
    fun `top-level function whose name starts with the composable's name is kept`() {
        val desc = "(Ljava/lang/String;)Ljava/lang/String;"
        val node = classNode(
            "a/BoardRootKt",
            method("BoardRoot", COMPOSABLE_DESC, composable = true),
            method("BoardRootTitle", desc),
            method("BoardRootTitle\$format", desc),
        )

        assertEquals(listOf("BoardRootTitle", "BoardRootTitle\$format"), survivors(node, "BoardRootTitle" to desc, "BoardRootTitle\$format" to desc))
    }

    @Test
    fun `lambda of a function with a mangled name is matched through the underscore form`() {
        val node = classNode(
            "a/DividerKt",
            method("drawDivider-4WTKRHQ", "(Landroidx/compose/ui/Modifier;J)Landroidx/compose/ui/Modifier;"),
            method("drawDivider_4WTKRHQ\$lambda\$0", "(JLandroidx/compose/ui/graphics/drawscope/DrawScope;)Lkotlin/Unit;"),
        )

        val result = survivors(
            node,
            "drawDivider-4WTKRHQ" to "(Landroidx/compose/ui/Modifier;J)Landroidx/compose/ui/Modifier;",
            "drawDivider_4WTKRHQ\$lambda\$0" to "(JLandroidx/compose/ui/graphics/drawscope/DrawScope;)Lkotlin/Unit;",
        )

        assertEquals(emptyList(), result)
    }

    @Test
    fun `function taking a Modifier but returning something else is kept`() {
        val desc = "(Landroidx/compose/ui/Modifier;)Ljava/lang/String;"
        val node = classNode("a/DescribeKt", method("describe", desc))

        assertEquals(listOf("describe"), survivors(node, "describe" to desc))
    }

    @Test
    fun `non-composable overload with the same name is kept`() {
        val plain = "(I)Ljava/lang/String;"
        val node = classNode(
            "a/LabelKt",
            method("Label", COMPOSABLE_DESC, composable = true),
            method("Label", plain),
        )

        assertEquals(listOf("Label"), survivors(node, "Label" to COMPOSABLE_DESC, "Label" to plain))
    }

    @Test
    fun `class enclosed by a composable is dropped`() {
        register(classNode("a/ScreenKt", method("Screen", COMPOSABLE_DESC, composable = true)))
        val effect = classNode("a/ScreenKt\$Screen\$1", method("invokeSuspend", INVOKE_SUSPEND_DESC))
            .enclosedBy("a/ScreenKt", "Screen", COMPOSABLE_DESC)

        assertEquals(emptyList(), survivors(effect, "invokeSuspend" to INVOKE_SUSPEND_DESC))
    }

    @Test
    fun `class enclosed by a lambda of a composable is dropped`() {
        register(
            classNode(
                "a/ScreenKt",
                method("Screen", COMPOSABLE_DESC, composable = true),
                method("Screen\$lambda\$0", "()Lkotlin/Unit;"),
            ),
        )
        val effect = classNode("a/ScreenKt\$Screen\$lambda\$0\$1", method("invokeSuspend", INVOKE_SUSPEND_DESC))
            .enclosedBy("a/ScreenKt", "Screen\$lambda\$0", "()Lkotlin/Unit;")

        assertEquals(emptyList(), survivors(effect, "invokeSuspend" to INVOKE_SUSPEND_DESC))
    }

    @Test
    fun `class nested two levels below a composable is dropped`() {
        register(classNode("a/ScreenKt", method("Screen", COMPOSABLE_DESC, composable = true)))
        register(
            classNode("a/ScreenKt\$Screen\$1", method("invokeSuspend", INVOKE_SUSPEND_DESC))
                .enclosedBy("a/ScreenKt", "Screen", COMPOSABLE_DESC),
        )
        val inner = classNode("a/ScreenKt\$Screen\$1\$1", method("invokeSuspend", INVOKE_SUSPEND_DESC))
            .enclosedBy("a/ScreenKt\$Screen\$1", "invokeSuspend", INVOKE_SUSPEND_DESC)

        assertEquals(emptyList(), survivors(inner, "invokeSuspend" to INVOKE_SUSPEND_DESC))
    }

    @Test
    fun `class enclosed by a plain method is kept`() {
        register(classNode("a/BoardViewModel", method("load", "()V")))
        val launch = classNode("a/BoardViewModel\$load\$1", method("invokeSuspend", INVOKE_SUSPEND_DESC))
            .enclosedBy("a/BoardViewModel", "load", "()V")

        assertEquals(listOf("invokeSuspend"), survivors(launch, "invokeSuspend" to INVOKE_SUSPEND_DESC))
    }

    @Test
    fun `class whose outer class can't be loaded is kept`() {
        val orphan = classNode("a/GoneKt\$Gone\$1", method("invokeSuspend", INVOKE_SUSPEND_DESC))
            .enclosedBy("a/GoneKt", "Gone", COMPOSABLE_DESC)

        assertEquals(listOf("invokeSuspend"), survivors(orphan, "invokeSuspend" to INVOKE_SUSPEND_DESC))
    }

    @Test
    fun `factory provides a feature that is on by default`() {
        val feature = ComposeUiFilterFactory().provides()

        assertEquals(ComposeUiFilterFactory.FEATURE_NAME.lowercase(), feature.name())
        assertTrue(feature.isOnByDefault)
    }

    private fun survivors(node: ClassNode, vararg methods: Pair<String, String>): List<String> {
        val filter = ComposeUiFilter(source)
        val mutations = methods.mapIndexed { index, (name, desc) -> mutation(node.name, name, desc, index) }
        filter.begin(ClassTree(node))
        val result = filter.intercept(mutations, null).map { it.method }
        filter.end()
        return result
    }

    private fun mutation(owner: String, name: String, desc: String, index: Int): MutationDetails {
        val location = Location.location(ClassName.fromString(owner), name, desc)
        return MutationDetails(MutationIdentifier(location, index, "mutator"), "File.kt", "description", 1, 0)
    }

    private fun register(node: ClassNode) {
        val writer = ClassWriter(0)
        node.accept(writer)
        classes[node.name.replace('/', '.')] = writer.toByteArray()
    }

    private fun classNode(name: String, vararg methods: MethodNode) = ClassNode().apply {
        version = Opcodes.V17
        access = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL
        this.name = name
        superName = "java/lang/Object"
        this.methods.addAll(methods)
    }

    private fun ClassNode.enclosedBy(owner: String, method: String, desc: String) = apply {
        outerClass = owner
        outerMethod = method
        outerMethodDesc = desc
    }

    private fun method(name: String, desc: String, composable: Boolean = false, visible: Boolean = false) =
        MethodNode(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_ABSTRACT, name, desc, null, null).apply {
            if (composable) {
                val annotation = AnnotationNode(ComposeUiFilter.COMPOSABLE)
                if (visible) visibleAnnotations = listOf(annotation) else invisibleAnnotations = listOf(annotation)
            }
        }

    private companion object {
        const val COMPOSABLE_DESC = "(Landroidx/compose/runtime/Composer;I)V"
        const val INVOKE_SUSPEND_DESC = "(Ljava/lang/Object;)Ljava/lang/Object;"
    }
}
