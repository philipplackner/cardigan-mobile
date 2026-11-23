package com.plcoding.cardiganapp.core.domain.text

/** The Unicode general categories the password rule needs. Everything else is [Other]. */
enum class CodePointCategory { Lower, Upper, Digit, Other }

/** The Unicode general category of [codePoint]: Ll, Lu, Nd or other, using the platform's tables. */
expect fun codePointCategory(codePoint: Int): CodePointCategory
