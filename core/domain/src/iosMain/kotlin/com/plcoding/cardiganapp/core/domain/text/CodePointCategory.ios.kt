package com.plcoding.cardiganapp.core.domain.text

import platform.Foundation.NSCharacterSet

actual fun codePointCategory(codePoint: Int): CodePointCategory {
    if (codePoint <= 0xFFFF) {
        return when (codePoint.toChar().category) {
            CharCategory.LOWERCASE_LETTER -> CodePointCategory.Lower
            CharCategory.UPPERCASE_LETTER -> CodePointCategory.Upper
            CharCategory.DECIMAL_DIGIT_NUMBER -> CodePointCategory.Digit
            else -> CodePointCategory.Other
        }
    }
    // Apple's uppercase set also holds Lt, but no Lt character lies outside the BMP.
    val member = codePoint.toUInt()
    return when {
        NSCharacterSet.lowercaseLetterCharacterSet.longCharacterIsMember(member) -> CodePointCategory.Lower
        NSCharacterSet.uppercaseLetterCharacterSet.longCharacterIsMember(member) -> CodePointCategory.Upper
        NSCharacterSet.decimalDigitCharacterSet.longCharacterIsMember(member) -> CodePointCategory.Digit
        else -> CodePointCategory.Other
    }
}
