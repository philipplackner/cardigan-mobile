package com.plcoding.cardiganapp.pitest

import com.plcoding.cardiganapp.convention.MutationExclusions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NamedMemberFilterTest {

    private val filter = NamedMemberFilter(MutationExclusions.namedMembers)

    @Test
    fun `asStringAsync is dropped in UiText, its DefaultImpls and nested implementations`() {
        listOf(
            "$UI_TEXT",
            "$UI_TEXT\$DefaultImpls",
            "$UI_TEXT\$DynamicString",
            "$UI_TEXT\$Resource",
            "$UI_TEXT\$PluralResource",
        ).forEach { owner ->
            assertEquals(emptyList(), survive(owner, "asStringAsync"), owner)
        }
    }

    @Test
    fun `other UiText members are kept`() {
        assertEquals(listOf("getValue"), survive("$UI_TEXT\$DynamicString", "getValue"))
        assertEquals(listOf("getQuantity"), survive("$UI_TEXT\$PluralResource", "getQuantity"))
    }

    @Test
    fun `same-named method in another class is kept`() {
        assertEquals(listOf("asStringAsync"), survive("com/plcoding/cardiganapp/core/presentation/util/UiTextExtKt", "asStringAsync"))
        assertEquals(listOf("asStringAsync"), survive("com/plcoding/cardiganapp/other/Message", "asStringAsync"))
    }

    @Test
    fun `HttpClientFactory config lambdas are dropped and its other members are kept`() {
        assertEquals(emptyList(), survive(HTTP_CLIENT_FACTORY, "create\$lambda\$0"))
        assertEquals(emptyList(), survive(HTTP_CLIENT_FACTORY, "create\$lambda\$0\$0\$0"))
        assertEquals(listOf("create"), survive(HTTP_CLIENT_FACTORY, "create"))
        assertEquals(listOf("create\$lambda\$0"), survive("com/plcoding/cardiganapp/other/OtherFactory", "create\$lambda\$0"))
    }

    @Test
    fun `entry without a method part is rejected`() {
        assertFailsWith<IllegalArgumentException> { NamedMemberFilter(listOf("*.UiText")) }
        assertFailsWith<IllegalArgumentException> { NamedMemberFilter(listOf("*.UiText#")) }
    }

    @Test
    fun `factory provides a feature that is off by default`() {
        val feature = NamedMemberFilterFactory().provides()

        assertEquals(NamedMemberFilterFactory.FEATURE_NAME.lowercase(), feature.name())
        assertTrue(!feature.isOnByDefault)
    }

    private fun survive(owner: String, method: String): List<String> {
        val mutations = listOf(mutation(owner, method, "()V"))
        return filter.intercept(mutations, null).map { it.method }
    }

    private companion object {
        const val UI_TEXT = "com/plcoding/cardiganapp/core/presentation/util/UiText"
        const val HTTP_CLIENT_FACTORY = "com/plcoding/cardiganapp/core/data/networking/HttpClientFactory"
    }
}
