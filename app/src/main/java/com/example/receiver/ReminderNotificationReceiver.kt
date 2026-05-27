package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "Alarm received, checking if notification is scheduled for today.")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = com.example.data.database.AppDatabase.getDatabase(context)
                val profile = db.userProfileDao().getUserProfileDirect()
                if (profile != null) {
                    if (profile.reminderEnabled) {
                        val cal = Calendar.getInstance()
                        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                        val dayAbbrev = when (dayOfWeek) {
                            Calendar.MONDAY -> "Mon"
                            Calendar.TUESDAY -> "Tue"
                            Calendar.WEDNESDAY -> "Wed"
                            Calendar.THURSDAY -> "Thu"
                            Calendar.FRIDAY -> "Fri"
                            Calendar.SATURDAY -> "Sat"
                            Calendar.SUNDAY -> "Sun"
                            else -> "Mon"
                        }
                        
                        val freq = profile.reminderFrequency
                        // Handle legacy "每日", "週一至週五", "每週", "Daily", "Mon to Fri", "Weekly"
                        // as well as new comma-separated week checkbox abbreviations like "Mon,Tue" etc.
                        val shouldNotify = freq == "每日" || freq == "Daily" ||
                                freq.contains(dayAbbrev, ignoreCase = true) ||
                                (freq == "週一至週五" && dayOfWeek in 2..6) ||
                                (freq == "Mon to Fri" && dayOfWeek in 2..6) ||
                                (freq == "每週" && dayOfWeek == Calendar.MONDAY) ||
                                (freq == "Weekly" && dayOfWeek == Calendar.MONDAY)
                        
                        if (shouldNotify) {
                            showNotification(context)
                        }
                        
                        // Reschedule for next day using the profile's specific hours & minutes
                        scheduleFromProfile(context, profile)
                    }
                } else {
                    // Fallback to daily if profile is not initialized
                    showNotification(context)
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    scheduleExactAlarm(context, cal.timeInMillis)
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Failed onReceive processing: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context) {
        val channelId = "bmi_tracker_reminders_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "BMI 體重記錄提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "每日提醒您記錄體重與查看 BMI 變化"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tips = listOf(
            "早晨空腹是測量體重最精準的黃金時刻喔！",
            "點選一鍵預填，3 秒快速記錄今日體重！",
            "持之以恆、定時記錄是養成健康身材的第一步！",
            "今天也來記錄體重，讓 Gemini AI 教練為您提供專屬分析吧！",
            "少喝一杯含糖飲料，能幫助您更輕鬆達成體重目標喔！"
        )
        val selectedTip = tips.random()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Using standard system backup icon
            .setContentTitle("🧘 體重與 BMI 紀錄時間到囉！")
            .setContentText(selectedTip)
            .setStyle(NotificationCompat.BigTextStyle().bigText(selectedTip))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    companion object {
        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d("Scheduler", "Alarm cancelled.")
            }
        }

        fun scheduleExactAlarm(context: Context, triggerTimeMs: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
                Log.d("Scheduler", "Successfully scheduled alarm at: ${Date(triggerTimeMs)}")
            } catch (e: SecurityException) {
                // Handle missing exact alarm schedule permission on Android 12+ (scheduling normally)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
                Log.d("Scheduler", "Fallback alarm scheduled due to permission constraints: ${e.message}")
            }
        }

        fun scheduleFromProfile(context: Context, profile: UserProfile) {
            cancelAlarm(context)
            if (!profile.reminderEnabled) return

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, profile.reminderHour)
                set(Calendar.MINUTE, profile.reminderMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If time is in the past, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            scheduleExactAlarm(context, calendar.timeInMillis)
        }
    }
}
