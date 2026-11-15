package com.plcoding.cardiganapp.feature.transfer.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.plcoding.cardiganapp.feature.transfer.domain.CheckedFile
import kotlin.test.Test

class JsonExportFileCheckerTest {

    private val checker = JsonExportFileChecker()

    private fun check(json: String) = checker.check(json.encodeToByteArray())

    @Test
    fun validExportPassesAndReturnsItsKey() {
        assertThat(check(exportJson())).isEqualTo(CheckedFile(key = "CRD"))
    }

    @Test
    fun notJsonFails() {
        assertThat(check("hello")).isNull()
        assertThat(check("")).isNull()
    }

    @Test
    fun leadingBomIsRemoved() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + exportJson().encodeToByteArray()
        assertThat(checker.check(bytes)).isEqualTo(CheckedFile(key = "CRD"))
    }

    @Test
    fun arrayAndNullRootsFail() {
        assertThat(check("[${exportJson()}]")).isNull()
        assertThat(check("null")).isNull()
    }

    @Test
    fun formatIsCaseSensitive() {
        assertThat(check(exportJson(format = "\"Cardigan.Board\""))).isNull()
    }

    @Test
    fun plainJsonObjectFails() {
        assertThat(check("""{"todo": []}""")).isNull()
    }

    @Test
    fun formatVersionMustBeTheIntegerLiteralOne() {
        assertThat(check(exportJson(formatVersion = "\"1\""))).isNull()
        assertThat(check(exportJson(formatVersion = "1.0"))).isNull()
        assertThat(check(exportJson(formatVersion = "1e0"))).isNull()
        assertThat(check(exportJson(formatVersion = "2"))).isNull()
        assertThat(check(exportJson(formatVersion = "100000000000000000000000001"))).isNull()
        assertThat(check(exportJson(formatVersion = "null"))).isNull()
        assertThat(check("""{"format": "cardigan.board", "board": {}}""")).isNull()
    }

    @Test
    fun lowercaseKeyIsNormalized() {
        assertThat(check(exportJson(key = "\"crd\""))).isEqualTo(CheckedFile(key = "CRD"))
    }

    @Test
    fun missingBoardPassesWithoutKey() {
        assertThat(check("""{"format": "cardigan.board", "formatVersion": 1}""")).isEqualTo(CheckedFile(key = null))
    }

    @Test
    fun nonStringKeyPassesWithoutKey() {
        assertThat(check(exportJson(key = "42"))).isEqualTo(CheckedFile(key = null))
        assertThat(check(exportJson(key = "null"))).isEqualTo(CheckedFile(key = null))
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val json = """{"format": "cardigan.board", "formatVersion": 1, "extra": true, "board": {"key": "CRD", "x": 1}}"""
        assertThat(check(json)).isEqualTo(CheckedFile(key = "CRD"))
    }

    @Test
    fun invalidUtf8Fails() {
        val bytes = exportJson(key = "\"CR\"").encodeToByteArray().toMutableList()
        val keyIndex = bytes.indexOf('R'.code.toByte())
        bytes[keyIndex] = 0xC3.toByte()
        assertThat(checker.check(bytes.toByteArray())).isNull()
    }
}
