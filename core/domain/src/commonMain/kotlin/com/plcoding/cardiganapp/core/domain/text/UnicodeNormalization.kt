package com.plcoding.cardiganapp.core.domain.text

/** Unicode canonical composition (NFC), using the platform's Unicode tables. */
expect fun String.toNfc(): String

/** Unicode canonical decomposition (NFD), using the platform's Unicode tables. */
expect fun String.toNfd(): String
