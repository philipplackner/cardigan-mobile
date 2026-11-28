package com.plcoding.cardiganapp.core.domain.label

import com.plcoding.cardiganapp.core.domain.util.DataError
import com.plcoding.cardiganapp.core.domain.util.Result
import kotlin.uuid.Uuid

/** A user's label, shared across all of their boards. */
data class Label(
    val id: Uuid,
    val name: String,
    val color: LabelColor,
)

/** The 20 palette names from cardigan-shared-specs `domain-label`. */
enum class LabelColor {
    Red,
    Orange,
    Amber,
    Yellow,
    Lime,
    Green,
    Emerald,
    Teal,
    Cyan,
    Sky,
    Blue,
    Indigo,
    Violet,
    Purple,
    Fuchsia,
    Pink,
    Rose,
    Brown,
    Olive,
    Slate,
}

/** Read-only access to the user's labels. The labels feature implements it. */
fun interface LabelSource {
    suspend fun labels(): Result<List<Label>, DataError>
}

/**
 * Label order on a card (cardigan-shared-specs `domain-card`): by the Unicode code points of the
 * case-folded name. Common Kotlin has no full Unicode case folding, so this uses the
 * locale-independent [lowercase]. Ties keep their original order.
 */
val LabelOrder: Comparator<Label> = Comparator { a, b -> compareByCodePoints(a.name.lowercase(), b.name.lowercase()) }

fun List<Label>.sortedForCard(): List<Label> = sortedWith(LabelOrder)

// String.compareTo compares UTF-16 units, which puts surrogate pairs before U+E000..U+FFFF.
private fun compareByCodePoints(
    a: String,
    b: String,
): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        val ca = codePointAt(a, i)
        val cb = codePointAt(b, j)
        if (ca != cb) return ca.compareTo(cb)
        i += if (ca > 0xFFFF) 2 else 1
        j += if (cb > 0xFFFF) 2 else 1
    }
    return (a.length - i).compareTo(b.length - j)
}

private fun codePointAt(
    text: String,
    index: Int,
): Int {
    val high = text[index]
    if (high.isHighSurrogate() && index + 1 < text.length) {
        val low = text[index + 1]
        if (low.isLowSurrogate()) return 0x10000 + ((high.code - 0xD800) shl 10) + (low.code - 0xDC00)
    }
    return high.code
}
