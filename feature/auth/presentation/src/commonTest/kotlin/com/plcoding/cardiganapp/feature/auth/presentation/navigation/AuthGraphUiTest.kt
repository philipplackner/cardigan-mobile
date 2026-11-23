package com.plcoding.cardiganapp.feature.auth.presentation.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.plcoding.cardiganapp.core.designsystem.theme.AppTheme
import com.plcoding.cardiganapp.core.designsystem.theme.WindowClass
import com.plcoding.cardiganapp.feature.auth.domain.AuthRepository
import com.plcoding.cardiganapp.feature.auth.presentation.di.authPresentationModule
import com.plcoding.cardiganapp.feature.auth.presentation.fakes.FakeAuthRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/** The real [authGraph] with its Koin module and a fake repository. */
@OptIn(ExperimentalTestApi::class)
class AuthGraphUiTest {

    private val isTextField = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)
    private lateinit var navController: NavHostController

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(authPresentationModule, module { single<AuthRepository> { FakeAuthRepository() } })
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private fun ComposeUiTest.show(start: Any = LoginRoute()) {
        setContent {
            AppTheme(darkTheme = false, windowClass = WindowClass.Phone) {
                val controller = rememberNavController()
                LaunchedEffect(controller) { navController = controller }
                NavHost(controller, startDestination = start) { authGraph(controller) }
            }
        }
    }

    @Test
    fun createOneOpensRegisterAndBackKeepsLoginValues() = runComposeUiTest {
        show()
        onAllNodes(isTextField)[0].performTextReplacement("ann@example.com")
        onNodeWithText("Create one").performClick()
        onNodeWithText("Create your account").assertExists()

        runOnIdle { navController.popBackStack() }
        onNodeWithText("Log in to Cardigan").assertExists()
        onAllNodes(isTextField)[0].assertTextEquals("ann@example.com", includeEditableText = true)
    }

    @Test
    fun logInOnRegisterReturnsToTheLoginUnderIt() = runComposeUiTest {
        show()
        onNodeWithText("Create one").performClick()
        onNodeWithText("Log in").performClick()
        onNodeWithText("Log in to Cardigan").assertExists()
        runOnIdle {
            val routes = navController.currentBackStack.value.map { it.destination }
            assertThat(routes.count { it.hasRoute<LoginRoute>() }).isEqualTo(1)
            assertThat(routes.none { it.hasRoute<RegisterRoute>() }).isTrue()
        }
    }

    @Test
    fun logInOnRegisterWithoutLoginBelowOpensLogin() = runComposeUiTest {
        show(start = RegisterRoute)
        onNodeWithText("Log in").performClick()
        onNodeWithText("Log in to Cardigan").assertExists()
        runOnIdle {
            assertThat(navController.currentBackStack.value.none { it.destination.hasRoute<RegisterRoute>() }).isTrue()
        }
        onAllNodes(hasText("Create your account")).fetchSemanticsNodes().let { assertThat(it.isEmpty()).isTrue() }
    }
}
