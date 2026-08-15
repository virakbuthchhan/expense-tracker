package com.example.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

enum class HapticType {
    SUCCESS,        // Adding an expense, saving a budget, successful import/export
    TOGGLE,         // Toggling switches, radio buttons
    DESTRUCTIVE,    // Deleting a transaction, resetting data, deleting a category/budget
    TAP,            // Clicking chips, buttons, filters, theme presets
    WARNING,        // Budget threshold warning trigger, validation error
    SELECTION       // Tab navigation, currency picker, date picker
}

class HapticHelper(
    private val view: View,
    private val context: Context,
    private val isHapticEnabled: Boolean = true
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun perform(type: HapticType) {
        if (!isHapticEnabled) return

        try {
            when (type) {
                HapticType.SUCCESS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 45), intArrayOf(0, 160, 0, 220), -1))
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                }

                HapticType.TOGGLE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(20, 120))
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                }

                HapticType.DESTRUCTIVE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 60, 70), intArrayOf(0, 220, 0, 255), -1))
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                }

                HapticType.TAP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(12, 100))
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                }

                HapticType.WARNING -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 40), intArrayOf(0, 180, 0, 180), -1))
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                }

                HapticType.SELECTION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(10, 80))
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback for devices without haptic motors or strict restrictions
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
        }
    }

    fun success() = perform(HapticType.SUCCESS)
    fun toggle() = perform(HapticType.TOGGLE)
    fun destructive() = perform(HapticType.DESTRUCTIVE)
    fun tap() = perform(HapticType.TAP)
    fun warning() = perform(HapticType.WARNING)
    fun selection() = perform(HapticType.SELECTION)
}

@Composable
fun rememberHapticFeedbackHelper(isHapticEnabled: Boolean = true): HapticHelper {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context, isHapticEnabled) {
        HapticHelper(view = view, context = context, isHapticEnabled = isHapticEnabled)
    }
}
