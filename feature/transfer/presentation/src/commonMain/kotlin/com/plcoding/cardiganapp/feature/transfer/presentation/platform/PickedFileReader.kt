package com.plcoding.cardiganapp.feature.transfer.presentation.platform

import com.plcoding.cardiganapp.feature.transfer.domain.ImportFileRules

/** Bytes kept while reading a picked file: one more than the limit, so an oversized file is detected. */
internal const val PICKED_FILE_READ_CAP: Long = ImportFileRules.MAX_FILE_BYTES + 1
