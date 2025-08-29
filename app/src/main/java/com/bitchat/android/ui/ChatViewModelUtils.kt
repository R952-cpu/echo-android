package com.bitchat.android.ui

/**
 * Utility functions for the ChatViewModel.
 * Implemented via reflection only to avoid hard Android imports that can
 * confuse IDE highlighting on some setups. Runtime behavior is unchanged.
 */
object ChatViewModelUtils {

    /**
     * Trigger haptic feedback (50ms), best-effort and silent on failure.
     * Accepts Any? to avoid requiring android.content.Context at compile time.
     */
    fun triggerHapticFeedback(context: Any?) {
        // Try modern API (VibrationEffect) first, then fall back to legacy vibrate(long)
        try {
            val ctx = context ?: return
            val ctxClass = ctx::class.java

            // Obtain Vibrator via Context.getSystemService(Context.VIBRATOR_SERVICE)
            val contextClass = Class.forName("android.content.Context")
            val svcName = contextClass.getField("VIBRATOR_SERVICE").get(null) as String
            val getSystemService = ctxClass.getMethod("getSystemService", String::class.java)
            val vibrator = getSystemService.invoke(ctx, svcName)
                ?: return

            val vibratorClass = Class.forName("android.os.Vibrator")
            val hasVibrator = vibratorClass.getMethod("hasVibrator").invoke(vibrator) as Boolean
            if (!hasVibrator) return

            // Use VibrationEffect if available (API 26+)
            val veClass = Class.forName("android.os.VibrationEffect")
            val createOneShot = veClass.getMethod(
                "createOneShot",
                java.lang.Long.TYPE,
                java.lang.Integer.TYPE
            )
            val defaultAmp = veClass.getField("DEFAULT_AMPLITUDE").get(null) as Int
            val effect = createOneShot.invoke(null, 50L, defaultAmp)
            val vibrateWithEffect = vibratorClass.getMethod("vibrate", veClass)
            vibrateWithEffect.invoke(vibrator, effect)
            return
        } catch (_: Throwable) {
            // Fall through to legacy path
        }

        try {
            val ctx = context ?: return
            val ctxClass = ctx::class.java
            val contextClass = Class.forName("android.content.Context")
            val svcName = contextClass.getField("VIBRATOR_SERVICE").get(null) as String
            val getSystemService = ctxClass.getMethod("getSystemService", String::class.java)
            val vibrator = getSystemService.invoke(ctx, svcName)
                ?: return
            val vibratorClass = Class.forName("android.os.Vibrator")
            val vibrateLegacy = vibratorClass.getMethod("vibrate", java.lang.Long.TYPE)
            vibrateLegacy.invoke(vibrator, 50L)
        } catch (_: Throwable) {
            // Give up silently
        }
    }
}
