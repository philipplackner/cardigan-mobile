package com.plcoding.cardiganapp.core.domain.text

actual fun codePointCategory(codePoint: Int): CodePointCategory = when (Character.getType(codePoint).toByte()) {
    Character.LOWERCASE_LETTER -> CodePointCategory.Lower
    Character.UPPERCASE_LETTER -> CodePointCategory.Upper
    Character.DECIMAL_DIGIT_NUMBER -> CodePointCategory.Digit
    else -> CodePointCategory.Other
}
