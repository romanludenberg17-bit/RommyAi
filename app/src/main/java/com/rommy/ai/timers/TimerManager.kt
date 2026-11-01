package com.rommy.ai.timers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class TimerManager(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(durationSeconds: Long, requestCode: Int = (System.currentTimeMillis() and 0x7fffffff).toInt()): Boolean {
        if (durationSeconds <= 0L) return false
        val triggerAt = System.currentTimeMillis() + durationSeconds * 1000L
        val intent = Intent(appContext, TimerReceiver::class.java).putExtra(EXTRA_REQUEST_CODE, requestCode)
        val pending = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return false
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        return true
    }

    companion object { const val EXTRA_REQUEST_CODE = "request_code" }
}
