package com.plcoding.cardiganapp.feature.transfer.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

/*
 * Break vectors: zero and one byte, the half-KB rounding edge (1,535 / 1,536 / 2,560), the
 * KB→MB switch (1,048,575 rounds to 1024 KB, 1,048,576 is exactly 1 MB), the limit and one byte
 * over it (round half up vs. round up), a typical over-limit size, a negative size and Long.MAX_VALUE.
 */
class FileSizeFormatterTest {

    private fun format(bytes: Long) = FileSizeFormatter.format(bytes)

    @Test
    fun zeroBytesIsZeroKb() {
        assertThat(format(0)).isEqualTo(FileSize("0", FileSizeUnit.KB))
    }

    @Test
    fun oneByteRoundsDownToZeroKb() {
        assertThat(format(1)).isEqualTo(FileSize("0", FileSizeUnit.KB))
    }

    @Test
    fun smallFileShowsWholeKb() {
        assertThat(format(14_000)).isEqualTo(FileSize("14", FileSizeUnit.KB))
    }

    @Test
    fun justBelowHalfAKilobyteRoundsDown() {
        assertThat(format(1_535)).isEqualTo(FileSize("1", FileSizeUnit.KB))
    }

    @Test
    fun exactlyHalfAKilobyteRoundsUp() {
        assertThat(format(1_536)).isEqualTo(FileSize("2", FileSizeUnit.KB))
        assertThat(format(2_560)).isEqualTo(FileSize("3", FileSizeUnit.KB))
    }

    @Test
    fun valueRoundingTo1024KbSwitchesToMb() {
        assertThat(format(1_048_575)).isEqualTo(FileSize("1.0", FileSizeUnit.MB))
    }

    @Test
    fun exactlyOneMebibyte() {
        assertThat(format(1_048_576)).isEqualTo(FileSize("1.0", FileSizeUnit.MB))
    }

    @Test
    fun atTheLimitShowsFivePointZero() {
        assertThat(format(5_242_880)).isEqualTo(FileSize("5.0", FileSizeUnit.MB))
    }

    @Test
    fun oneByteOverTheLimitRoundsUp() {
        assertThat(format(5_242_881)).isEqualTo(FileSize("5.1", FileSizeUnit.MB))
    }

    @Test
    fun typicalOverLimitFile() {
        assertThat(format(7_549_747)).isEqualTo(FileSize("7.2", FileSizeUnit.MB))
    }

    @Test
    fun negativeSizeIsTreatedAsZero() {
        assertThat(format(-5)).isEqualTo(FileSize("0", FileSizeUnit.KB))
    }

    @Test
    fun maxLongDoesNotOverflow() {
        assertThat(format(Long.MAX_VALUE)).isEqualTo(FileSize("8796093022208.0", FileSizeUnit.MB))
    }

    @Test
    fun limitIsInclusive() {
        assertThat(ImportFileRules.isTooLarge(5_242_880)).isFalse()
        assertThat(ImportFileRules.isTooLarge(5_242_881)).isTrue()
    }
}
