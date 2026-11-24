package com.salati.app

import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

private const val USAGE_SELECTED_KEY = "usage_selected_apps"
private const val USAGE_ALERTS_KEY = "usage_alerts_enabled"
private const val USAGE_LIMIT_KEY = "usage_limit_minutes"
private const val USAGE_WORK_NAME = "selected_app_usage_limit_monitor"
private const val USAGE_CHANNEL = "selected_app_usage_nudges"

private data class UsageAppChoice(val packageName: String, val label: String)

private val usageAppChoices = listOf(
    UsageAppChoice("com.facebook.katana", "Facebook"),
    UsageAppChoice("com.instagram.android", "Instagram"),
    UsageAppChoice("com.google.android.youtube", "YouTube"),
    UsageAppChoice("com.zhiliaoapp.musically", "TikTok"),
    UsageAppChoice("com.twitter.android", "X"),
    UsageAppChoice("com.snapchat.android", "Snapchat")
)

private object UsageAccess {
    fun isGranted(context: Context): Boolean = runCatching {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    fun installedChoices(context: Context): List<UsageAppChoice> =
        usageAppChoices.filter { choice ->
            runCatching {
                context.packageManager.getApplicationInfo(choice.packageName, 0)
                true
            }.getOrDefault(false)
        }

    fun label(context: Context, packageName: String): String {
        val known = usageAppChoices.firstOrNull { it.packageName == packageName }?.label
        if (known != null) return known
        return runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }

    fun todayUsage(context: Context, selectedPackages: Set<String>): Map<String, Long> {
        if (selectedPackages.isEmpty() || !isGranted(context)) return emptyMap()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val end = System.currentTimeMillis()
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        return stats.orEmpty()
            .filter { it.packageName in selectedPackages }
            .groupBy { it.packageName }
            .mapValues { (_, entries) -> entries.sumOf { it.totalTimeInForeground.coerceAtLeast(0L) } }
    }
}

object UsageLimitWork {
    fun update(context: Context) {
        val prefs = context.getSharedPreferences("salati", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(USAGE_ALERTS_KEY, false)
        val selected = prefs.getStringSet(USAGE_SELECTED_KEY, emptySet())?.toSet().orEmpty()
        if (!enabled || selected.isEmpty() || !UsageAccess.isGranted(context)) {
            WorkManager.getInstance(context).cancelUniqueWork(USAGE_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<SelectedAppUsageWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            USAGE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(USAGE_WORK_NAME)
    }
}

class SelectedAppUsageWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val context = applicationContext
        val prefs = context.getSharedPreferences("salati", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(USAGE_ALERTS_KEY, false) || !UsageAccess.isGranted(context)) return Result.success()
        val selected = prefs.getStringSet(USAGE_SELECTED_KEY, emptySet())?.toSet().orEmpty()
        if (selected.isEmpty()) return Result.success()

        val limitMinutes = prefs.getInt(USAGE_LIMIT_KEY, 60).coerceIn(15, 240)
        val limitMillis = limitMinutes * 60_000L
        val usage = runCatching { UsageAccess.todayUsage(context, selected) }.getOrDefault(emptyMap())
        val dateKey = LocalDate.now().toString()
        val notifiedKey = "usage_notified_$dateKey"
        val notified = prefs.getStringSet(notifiedKey, emptySet())?.toMutableSet() ?: mutableSetOf()

        usage.forEach { (packageName, duration) ->
            if (duration >= limitMillis && notified.add(packageName)) {
                postUsageLimitNotification(
                    context,
                    packageName,
                    UsageAccess.label(context, packageName),
                    limitMinutes
                )
            }
        }
        prefs.edit().putStringSet(notifiedKey, notified).apply()
        return Result.success()
    }
}

private fun postUsageLimitNotification(context: Context, packageName: String, appLabel: String, limitMinutes: Int) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(
                USAGE_CHANNEL,
                "تذكيرات وقت التطبيقات",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "تنبيه محلي عند بلوغ حد استخدام اختاره المستخدم" }
        )
    }

    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("open_daily_routine", true)
        putExtra("usage_limit_app", packageName)
    }
    val pending = android.app.PendingIntent.getActivity(
        context,
        packageName.hashCode(),
        openIntent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        android.app.Notification.Builder(context, USAGE_CHANNEL)
    } else {
        android.app.Notification.Builder(context)
    }
    manager.notify(
        packageName.hashCode(),
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("وقت مراجعة اختياري")
            .setContentText("وصل استخدام $appLabel إلى $limitMinutes دقيقة اليوم.")
            .setStyle(android.app.Notification.BigTextStyle().bigText("وصل استخدام $appLabel إلى الحد الذي اخترته ($limitMinutes دقيقة). يمكنك أخذ استراحة أو المتابعة بقرارك."))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
    )
}

@Composable
fun ScreenTimeUsageCard(prefs: android.content.SharedPreferences) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var accessGranted by remember { mutableStateOf(UsageAccess.isGranted(context)) }
    var installedApps by remember { mutableStateOf(UsageAccess.installedChoices(context)) }
    var selectedPackages by remember {
        mutableStateOf(prefs.getStringSet(USAGE_SELECTED_KEY, emptySet())?.toSet().orEmpty())
    }
    var alertsEnabled by remember { mutableStateOf(prefs.getBoolean(USAGE_ALERTS_KEY, false)) }
    var limitMinutes by remember { mutableIntStateOf(prefs.getInt(USAGE_LIMIT_KEY, 60).coerceIn(15, 240)) }
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var feedback by remember { mutableStateOf("") }

    fun refreshUsage() {
        accessGranted = UsageAccess.isGranted(context)
        installedApps = UsageAccess.installedChoices(context)
        usage = if (accessGranted) UsageAccess.todayUsage(context, selectedPackages) else emptyMap()
        UsageLimitWork.update(context)
    }

    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("وقت التطبيقات الأخرى — اختياري", color = RoutineGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                "يمكنك اختيار تطبيقات محددة لمعرفة مدة ظهورها على الشاشة. يقرأ التطبيق اسم التطبيق والمدة الإجمالية فقط؛ لا يقرأ الرسائل أو المنشورات أو سجل البحث. البيانات تبقى على هذا الجهاز.",
                style = MaterialTheme.typography.bodySmall
            )
            if (!accessGranted) {
                Text(
                    "هذه الميزة تحتاج إذن «الوصول إلى بيانات الاستخدام» من إعدادات أندرويد. لن تعمل دون اختيارك ومنح الإذن يدويًا.",
                    color = Color(0xFF765B38), style = MaterialTheme.typography.bodySmall
                )
                Button(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = RoutineGreen)) {
                    Text("فتح إعدادات الوصول إلى الاستخدام")
                }
                OutlinedButton(onClick = { refreshUsage() }) { Text("تحقق بعد الرجوع من الإعدادات") }
            } else {
                Text("اختر التطبيقات التي تريد متابعتها", color = RoutineGreen, fontWeight = FontWeight.SemiBold)
                if (installedApps.isEmpty()) {
                    Text("لم نجد التطبيقات المعروفة المثبتة على جهازك.", style = MaterialTheme.typography.bodySmall)
                }
                installedApps.forEach { app ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(
                            checked = app.packageName in selectedPackages,
                            onCheckedChange = { checked ->
                                selectedPackages = if (checked) selectedPackages + app.packageName else selectedPackages - app.packageName
                                prefs.edit().putStringSet(USAGE_SELECTED_KEY, selectedPackages).apply()
                                usage = UsageAccess.todayUsage(context, selectedPackages)
                                UsageLimitWork.update(context)
                            }
                        )
                        Text(app.label)
                    }
                }
                if (selectedPackages.isNotEmpty()) {
                    HorizontalDivider(color = RoutinePale)
                    Text("الحد اليومي لكل تطبيق مختار", color = RoutineGreen, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(30, 45, 60, 90, 120).forEach { minutes ->
                            FilterChip(
                                selected = limitMinutes == minutes,
                                onClick = {
                                    limitMinutes = minutes
                                    prefs.edit().putInt(USAGE_LIMIT_KEY, minutes).apply()
                                    UsageLimitWork.update(context)
                                },
                                label = { Text("${minutes}د") }
                            )
                        }
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Switch(
                            checked = alertsEnabled,
                            onCheckedChange = { enabled ->
                                alertsEnabled = enabled
                                prefs.edit().putBoolean(USAGE_ALERTS_KEY, enabled).apply()
                                UsageLimitWork.update(context)
                            }
                        )
                        Column(Modifier.weight(1f)) {
                            Text("نبّهني عندما أصل إلى الحد")
                            Text("الفحص دوري تقريبي، وقد يتأخر بسبب توفير البطارية.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF77736B))
                        }
                    }
                    installedApps.filter { it.packageName in selectedPackages }.forEach { app ->
                        val duration = usage[app.packageName] ?: 0L
                        val minutes = duration / 60_000L
                        Text("${app.label}: ${minutes} دقيقة اليوم", style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(onClick = { refreshUsage() }) { Text("تحديث وقت الاستخدام الآن") }
                }
                OutlinedButton(onClick = {
                    selectedPackages = emptySet()
                    alertsEnabled = false
                    usage = emptyMap()
                    prefs.edit().putStringSet(USAGE_SELECTED_KEY, emptySet()).putBoolean(USAGE_ALERTS_KEY, false).apply()
                    UsageLimitWork.stop(context)
                    feedback = "تم إيقاف متابعة التطبيقات وإلغاء التنبيهات."
                }) { Text("إيقاف المتابعة وحذف اختياراتي") }
            }
            if (feedback.isNotBlank()) Text(feedback, color = RoutineGreen, style = MaterialTheme.typography.bodySmall)
        }
    }
}
