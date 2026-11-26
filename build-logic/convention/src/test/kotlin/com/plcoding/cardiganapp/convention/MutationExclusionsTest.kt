package com.plcoding.cardiganapp.convention

import org.pitest.util.Glob
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MutationExclusionsTest {

    private val sharedGlobs = CodeExclusions.pitClassGlobs.map(::Glob)
    private val globs = sharedGlobs + MutationExclusions.classGlobs.map(::Glob)

    private fun excluded(className: String) = globs.any { it.matches(className) }

    private fun excludedFromCoverage(className: String) = sharedGlobs.any { it.matches(className) }

    @Test
    fun `generated code, inline copies and navigation wiring are excluded`() {
        listOf(
            "com.plcoding.cardiganapp.feature.boards.presentation.boardlist.BoardListScreenKt\$WhenMappings",
            "com.plcoding.cardiganapp.core.data.networking.HttpClientExtKt\$get\$\$inlined\$safeCall\$1",
            "com.plcoding.cardiganapp.feature.transfer.presentation.importboard.ImportPhase\$ChoosingKey\$\$serializer",
            "com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardRoute",
            "com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardRoute\$\$serializer",
            "com.plcoding.cardiganapp.feature.auth.presentation.navigation.LoginRoute\$Companion",
            "com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardsGraphKt",
            "com.plcoding.cardiganapp.feature.auth.presentation.navigation.AuthGraphKt\$authGraph\$2\$1\$1",
            "com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardViewNavigationKt",
            "com.plcoding.cardiganapp.navigation.AppNavHostKt",
        ).forEach { assertTrue(excluded(it), it) }
    }

    @Test
    fun `design tokens are excluded`() {
        listOf(
            "AccentColors", "AppColors", "LabelColors", "ColorsKt", "AppSpacing", "AppRadius", "AppHeight",
            "AppWidth", "AppShadow", "AppShadows", "AppTextStyles", "AppTypography", "AppTheme", "AppThemeKt",
            "ShadowsKt", "TextStylesKt", "TypographyKt",
        ).forEach {
            val name = "com.plcoding.cardiganapp.core.designsystem.theme.$it"
            assertTrue(excluded(name), name)
        }
    }

    @Test
    fun `logic next to tokens and navigation stays in`() {
        val kept = listOf(
            "com.plcoding.cardiganapp.core.designsystem.theme.WindowBreakpoints",
            "com.plcoding.cardiganapp.core.designsystem.theme.WindowClassKt",
            "com.plcoding.cardiganapp.core.designsystem.theme.WindowClass",
            "com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardListReloader",
            "com.plcoding.cardiganapp.shell.AppShellKt",
            "com.plcoding.cardiganapp.session.SessionGateKt",
            "com.plcoding.cardiganapp.feature.boards.presentation.detail.BoardViewModel",
            "com.plcoding.cardiganapp.feature.boards.presentation.detail.BoardViewModel\$confirmDelete\$2",
            "com.plcoding.cardiganapp.feature.transfer.presentation.importboard.ImportPhase\$ChoosingKey",
        ).filter(::excluded)

        assertEquals(emptyList(), kept)
    }

    @Test
    fun `design tokens and navigation wiring come from the shared list`() {
        listOf(
            "com.plcoding.cardiganapp.core.designsystem.theme.AppColors",
            "com.plcoding.cardiganapp.core.designsystem.theme.AppThemeKt",
            "com.plcoding.cardiganapp.core.designsystem.theme.TypographyKt",
            "com.plcoding.cardiganapp.core.designsystem.theme.TypographyKt\$appTypography\$1",
            "com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardRoute",
            "com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardsGraphKt",
            "com.plcoding.cardiganapp.feature.boards.presentation.navigation.BoardViewNavigationKt",
            "com.plcoding.cardiganapp.navigation.AppNavHostKt",
        ).forEach { assertTrue(excludedFromCoverage(it), it) }
    }

    @Test
    fun `compiler noise stays mutation-only`() {
        val shared = listOf(
            "com.plcoding.cardiganapp.feature.boards.presentation.boardlist.BoardListScreenKt\$WhenMappings",
            "com.plcoding.cardiganapp.core.data.networking.HttpClientExtKt\$get\$\$inlined\$safeCall\$1",
            "com.plcoding.cardiganapp.feature.transfer.presentation.importboard.ImportPhase\$ChoosingKey\$\$serializer",
        ).filter(::excludedFromCoverage)

        assertEquals(emptyList(), shared)
        assertTrue(MutationExclusions.classGlobs.none { it in CodeExclusions.pitClassGlobs })
    }

    @Test
    fun `features configure FANN with Composable and PIT's defaults, plus the Compose UI filter`() {
        assertEquals(
            "+FANN(annotation[Composable] annotation[Generated] annotation[DoNotMutate] annotation[CoverageIgnore]),+FCOMPOSEUI",
            MutationExclusions.features,
        )
    }
}
