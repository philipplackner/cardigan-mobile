package com.plcoding.cardiganapp.core.domain.text

import java.text.Normalizer

actual fun String.toNfc(): String = Normalizer.normalize(this, Normalizer.Form.NFC)

actual fun String.toNfd(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
