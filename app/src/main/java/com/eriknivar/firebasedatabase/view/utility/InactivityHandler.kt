package com.eriknivar.firebasedatabase.view.utility

import android.os.Handler
import android.os.Looper

class InactivityHandler(private val timeoutMillis: Long = 1_800_000, val onTimeout: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable { onTimeout() }

    fun startTimer() {
        stopTimer()
        handler.postDelayed(runnable, timeoutMillis)
    }

    private fun stopTimer() {
        handler.removeCallbacks(runnable)
    }

    fun userInteracted() {
        startTimer()
    }
}

