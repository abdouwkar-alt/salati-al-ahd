package com.salati.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class AppUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "SalatiAndroid")
            }
            val release = try {
                if (connection.responseCode !in 200..299) return@withContext Result.retry()
                JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            } finally {
                connection.disconnect()
            }

            val tag = release.optString("tag_name").removePrefix("v").trim()
            val releaseUrl = release.optString("html_url").trim()
            if (tag.isBlank() || releaseUrl.isBlank()) return@withContext Result.success()

            val installed = applicationContext.packageManager
                .getPackageInfo(applicationContext.packageName, 0).versionName.orEmpty()
            if (compareVersions(tag, installed) <= 0) return@withContext Result.success()

            val prefs = applicationContext.getSharedPreferences("salati", Context.MODE_PRIVATE)
            if (prefs.getString(LAST_NOTIFIED_TAG, null) == tag) return@withContext Result.success()

            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return@withContext Result.success()

            createChannel()
            val openRelease = PendingIntent.getActivity(
                applicationContext,
                UPDATE_NOTIFICATION_ID,
                Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(applicationContext, UPDATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_logo)
                .setContentTitle("يتوفر تحديث جديد للصلاة جامعة")
                .setContentText("الإصدار $tag متاح الآن. اضغط لمعرفة التفاصيل والتنزيل.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("الإصدار $tag متاح الآن. افتح صفحة الإصدار على GitHub لتنزيله. سيحتفظ التطبيق ببياناتك عند تثبيت التحديث فوق النسخة الحالية الموقعة بالمفتاح نفسه."))
                .setContentIntent(openRelease)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(UPDATE_NOTIFICATION_ID, notification)
            prefs.edit().putString(LAST_NOTIFIED_TAG, tag).apply()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(UPDATE_CHANNEL, "تحديثات التطبيق", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "إشعار عند توفر إصدار أحدث من الصلاة جامعة"
                }
            )
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = left.split(".", "-", "+").map { it.toIntOrNull() ?: 0 }
        val b = right.split(".", "-", "+").map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(a.size, b.size)) {
            val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    companion object {
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/abdouwkar-alt/salati-al-ahd/releases/latest"
        private const val UPDATE_CHANNEL = "salati_app_updates"
        private const val UPDATE_NOTIFICATION_ID = 740021
        private const val LAST_NOTIFIED_TAG = "last_update_notification_tag"

        fun schedule(context: Context) {
            val manager = WorkManager.getInstance(context)
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val immediate = OneTimeWorkRequestBuilder<AppUpdateWorker>().setConstraints(constraints).build()
            manager.enqueueUniqueWork("salati_update_check_now", ExistingWorkPolicy.KEEP, immediate)
            val periodic = PeriodicWorkRequestBuilder<AppUpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            manager.enqueueUniquePeriodicWork("salati_update_check_daily", ExistingPeriodicWorkPolicy.KEEP, periodic)
        }
    }
}
