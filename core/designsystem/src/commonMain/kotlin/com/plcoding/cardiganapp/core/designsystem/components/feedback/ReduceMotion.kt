package com.plcoding.cardiganapp.core.designsystem.components.feedback

import androidx.compose.runtime.Composable

/**
 * True when the OS asks for reduced motion: Android "Remove animations" (animator duration
 * scale 0), iOS "Reduce Motion".
 */
@Composable
expect fun rememberReduceMotion(): Boolean
