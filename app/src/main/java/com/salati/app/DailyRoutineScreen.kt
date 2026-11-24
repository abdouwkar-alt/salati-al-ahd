package com.salati.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private val RoutineGreen = Color(0xFF164B3A)
private val RoutinePale = Color(0xFFE5EBDD)
private val RoutineCream = Color(0xFFF6F2E9)
private const val ROUTINE_PREFS_KEY = "daily_routine_reminders_v1"
private const val ROUTINE_CHANNEL_ID = "daily_routine_reminders"
private const val ROUTINE_ACTION_FIRE = "com.salati.app.ROUTINE_FIRE"
private const val ROUTINE_ACTION_SNOOZE = "com.salati.app.ROUTINE_SNOOZE"
private const val ROUTINE_ACTION_SNOOZE_FIRE = "com.salati.app.ROUTINE_SNOOZE_FIRE"

private data class RoutineReminder(
    val id: String,
    val title: String,
    val category: String,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>,
    val enabled: Boolean = true
)

private object RoutineReminderStore {
    fun load(prefs: android.content.SharedPreferences): List<RoutineReminder> {
        return runCatching {
            val data = JSONArray(prefs.getString(ROUTINE_PREFS_KEY, "[]") ?: "[]")
            buildList {
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val daysJson = item.optJSONArray("days") ?: JSONArray()
                    val days = buildSet {
                        for (j in 0 until daysJson.length()) {
                            val day = daysJson.optInt(j, 0)
                            if (day in 1..7) add(day)
                        }
                    }
                    val hour = item.optInt("hour", 7)
                    val minute = item.optInt("minute", 0)
                    if (hour !in 0..23 || minute !in 0..59) continue
                    add(
                        RoutineReminder(
                            id = item.optString("id", UUID.randomUUID().toString()),
                            title = item.optString("title", "تذكير"),
                            category = item.optString("category", "مخصص"),
                            hour = hour,
                            minute = minute,
                            days = days.ifEmpty { (1..7).toSet() },
                            enabled = item.optBoolean("enabled", true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(prefs: android.content.SharedPreferences, reminders: List<RoutineReminder>) {
        val array = JSONArray()
        reminders.forEach { reminder ->
            val days = JSONArray()
            reminder.days.sorted().forEach(days::put)
            array.put(
                JSONObject()
                    .put("id", reminder.id)
                    .put("title", reminder.title)
                    .put("category", reminder.category)
                    .put("hour", reminder.hour)
                    .put("minute", reminder.minute)
                    .put("days", days)
                    .put("enabled", reminder.enabled)
            )
        }
        prefs.edit().putString(ROUTINE_PREFS_KEY, array.toString()).apply()
    }
}

private object RoutineAlarmScheduler {
    fun scheduleNext(context: Context, reminder: RoutineReminder, afterMillis: Long = System.currentTimeMillis()) {
        cancel(context, reminder.id)
        if (!reminder.enabled || reminder.days.isEmpty()) return
        val now = Instant.ofEpochMilli(afterMillis).atZone(ZoneId.systemDefault())
        var next: ZonedDateTime? = null
        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (date.dayOfWeek.value !in reminder.days) continue
            val candidate = date.atTime(reminder.hour, reminder.minute).atZone(now.zone)
            if (candidate.isAfter(now.plusSeconds(2))) {
                next = candidate
                break
            }
        }
        next?.let {
            scheduleAt(
                context = context,
                reminder = reminder,
                triggerAtMillis = it.toInstant().toEpochMilli(),
                isSnooze = false
            )
        }
    }

    fun scheduleNextAll(context: Context) {
        val prefs = context.getSharedPreferences("salati", Context.MODE_PRIVATE)
        RoutineReminderStore.load(prefs).forEach { scheduleNext(context, it) }
    }

    fun cancel(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = alarmPendingIntent(context, reminderId, ROUTINE_ACTION_FIRE, false)
        val snoozePendingIntent = alarmPendingIntent(context, reminderId, ROUTINE_ACTION_SNOOZE_FIRE, true)
        alarmManager.cancel(pendingIntent)
        alarmManager.cancel(snoozePendingIntent)
        pendingIntent.cancel()
        snoozePendingIntent.cancel()
    }

    private fun scheduleAt(context: Context, reminder: RoutineReminder, triggerAtMillis: Long, isSnooze: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val action = if (isSnooze) ROUTINE_ACTION_SNOOZE_FIRE else ROUTINE_ACTION_FIRE
        val pendingIntent = alarmPendingIntent(context, reminder.id, action, isSnooze)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                // Graceful fallback: delivery may be delayed by Android battery policies.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }

    fun scheduleSnooze(context: Context, reminderId: String, title: String, category: String) {
        val reminder = RoutineReminder(
            id = reminderId,
            title = title,
            category = category,
            hour = 0,
            minute = 0,
            days = emptySet()
        )
        scheduleAt(context, reminder, System.currentTimeMillis() + 10 * 60 * 1000L, isSnooze = true)
    }

    private fun alarmPendingIntent(context: Context, id: String, action: String, snooze: Boolean): PendingIntent {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("salati-routine://alarm/$id/$action")
            putExtra("routine_id", id)
            putExtra("is_snooze", snooze)
        }
        return PendingIntent.getBroadcast(
            context,
            id.hashCode() + if (snooze) 17 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class RoutineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("routine_id") ?: return
        val prefs = context.getSharedPreferences("salati", Context.MODE_PRIVATE)
        val reminder = RoutineReminderStore.load(prefs).firstOrNull { it.id == id }
        val title = reminder?.title ?: intent.getStringExtra("routine_title") ?: "تذكير يومي"
        val category = reminder?.category ?: intent.getStringExtra("routine_category") ?: "مخصص"

        // The notification's snooze action schedules one single reminder; the snooze alarm itself must not snooze forever.
        if (intent.action == ROUTINE_ACTION_SNOOZE) {
            RoutineAlarmScheduler.scheduleSnooze(context, id, title, category)
            return
        }
        val isSnoozeAlarm = intent.action == ROUTINE_ACTION_SNOOZE_FIRE
        if (!isSnoozeAlarm && reminder != null) {
            RoutineAlarmScheduler.scheduleNext(context, reminder, System.currentTimeMillis() + 1_000L)
        }
        showRoutineNotification(context, id, title, category)
    }
}

class RoutineBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_DATE_CHANGED
            )
        ) {
            RoutineAlarmScheduler.scheduleNextAll(context)
        }
    }
}

private fun showRoutineNotification(context: Context, id: String, title: String, category: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(
                ROUTINE_CHANNEL_ID,
                "روتين يومك",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تذكيرات محلية اختارها المستخدم"
            }
        )
    }
    if (Build.VERSION.SDK_INT >= 33 &&
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) return

    val reminderText = when (category) {
        "استيقاظ" -> "الحمد لله الذي أحيانا بعدما أماتنا وإليه النشور. — صحيح البخاري (6325)"
        "وجبة" -> "سم الله، وكل بيمينك، وكل مما يليك. — متفق عليه"
        "نوم" -> "باسمك اللهم أموت وأحيا. — صحيح البخاري (6325)"
        else -> "هذا موعد اخترته لنفسك. افتح التذكرة أو أجّلها كما يناسبك."
    }
    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("open_daily_routine", true)
        putExtra("routine_focus_id", id)
    }
    val openPending = PendingIntent.getActivity(
        context, id.hashCode(), openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val snoozeIntent = Intent(context, RoutineAlarmReceiver::class.java).apply {
        action = ROUTINE_ACTION_SNOOZE
        data = Uri.parse("salati-routine://snooze/$id")
        putExtra("routine_id", id)
        putExtra("routine_title", title)
        putExtra("routine_category", category)
        putExtra("is_snooze", true)
    }
    val snoozePending = PendingIntent.getBroadcast(
        context, id.hashCode() + 17, snoozeIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        android.app.Notification.Builder(context, ROUTINE_CHANNEL_ID)
    } else {
        android.app.Notification.Builder(context)
    }
    val notification = builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle(title)
        .setContentText(reminderText)
        .setStyle(android.app.Notification.BigTextStyle().bigText(reminderText))
        .setContentIntent(openPending)
        .setAutoCancel(true)
        .setCategory(android.app.Notification.CATEGORY_REMINDER)
        .addAction(0, "تأجيل 10 دقائق", snoozePending)
        .addAction(0, "افتح التذكرة", openPending)
        .build()
    manager.notify(id.hashCode(), notification)
}

@Composable
fun DailyRoutineScreen(prefs: android.content.SharedPreferences) {
    val context = LocalContext.current
    val reminders: SnapshotStateList<RoutineReminder> = remember {
        mutableStateListOf<RoutineReminder>().apply { addAll(RoutineReminderStore.load(prefs)) }
    }
    var showEditor by remember { mutableStateOf(false) }
    var editingReminderId by remember { mutableStateOf<String?>(null) }
    var draftTitle by remember { mutableStateOf("") }
    var draftCategory by remember { mutableStateOf("استيقاظ") }
    var draftHour by remember { mutableIntStateOf(7) }
    var draftMinute by remember { mutableIntStateOf(0) }
    val selectedDays = remember { mutableStateListOf<Int>().apply { addAll(1..7) } }
    var feedback by remember { mutableStateOf("") }
    var clockNow by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            clockNow = System.currentTimeMillis()
        }
    }

    val today = LocalDate.now()
    val todayKey = today.toString()
    val previousForegroundTime = prefs.getLong("app_time_ms_$todayKey", 0L)
    val startedAt = prefs.getLong("app_session_started_at", 0L)
    val currentSession = if (startedAt > 0L && Instant.ofEpochMilli(startedAt).atZone(ZoneId.systemDefault()).toLocalDate() == today) {
        (clockNow - startedAt).coerceAtLeast(0L)
    } else 0L
    val inAppTime = previousForegroundTime + currentSession
    val lastSession = prefs.getLong("app_last_session_ms", 0L)

    fun persistAndSchedule() {
        RoutineReminderStore.save(prefs, reminders)
        reminders.forEach { reminder ->
            if (reminder.enabled) RoutineAlarmScheduler.scheduleNext(context, reminder)
            else RoutineAlarmScheduler.cancel(context, reminder.id)
        }
        feedback = "تم حفظ تذكيراتك على هذا الجهاز."
    }

    fun removeReminder(reminder: RoutineReminder) {
        RoutineAlarmScheduler.cancel(context, reminder.id)
        reminders.removeAll { it.id == reminder.id }
        persistAndSchedule()
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = RoutineGreen)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("روتين يومك", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "رتّب يومك حول الصلاة وما تختاره من تعلم وعادات وعبادات. التذكير تذكرة قابلة للتخطي، وليس اختبارًا لك.",
                    color = Color.White.copy(alpha = .92f)
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = RoutinePale)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("وقت وجودك داخل التطبيق", color = RoutineGreen, fontWeight = FontWeight.Bold)
                Text(formatRoutineDuration(inAppTime), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("آخر جلسة: ${formatRoutineDuration(lastSession)}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "هذا يحسب بقاء التطبيق في الواجهة فقط؛ لا يعني أن كل هذه المدة كانت تعلمًا أو عبادة.",
                    color = Color(0xFF765B38), style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("تذكيراتك", color = RoutineGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(
                onClick = {
                    editingReminderId = null
                    draftTitle = ""
                    draftCategory = "استيقاظ"
                    draftHour = 7
                    draftMinute = 0
                    selectedDays.clear()
                    selectedDays.addAll(1..7)
                    showEditor = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoutineGreen)
            ) { Text("إضافة منبه") }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("استيقاظ", "وجبة", "نوم", "مخصص").forEach { category ->
                OutlinedButton(
                    onClick = {
                        editingReminderId = null
                        draftCategory = category
                        draftTitle = when (category) {
                            "استيقاظ" -> "الاستيقاظ"
                            "وجبة" -> "موعد الطعام"
                            "نوم" -> "الاستعداد للنوم"
                            else -> "تذكير شخصي"
                        }
                        draftHour = when (category) { "استيقاظ" -> 7; "وجبة" -> 14; "نوم" -> 22; else -> 9 }
                        draftMinute = 0
                        selectedDays.clear()
                        selectedDays.addAll(1..7)
                        showEditor = true
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) { Text(category, style = MaterialTheme.typography.labelSmall) }
            }
        }

        if (reminders.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Text(
                    "لم تضف تذكيرات بعد. ابدأ بمنبه واحد فقط يناسب يومك، ويمكنك تعديل المواعيد أو إيقافها وقتما تشاء.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            reminders.sortedWith(compareBy<RoutineReminder>({ it.hour }, { it.minute })).forEach { reminder ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(reminder.title, color = RoutineGreen, fontWeight = FontWeight.Bold)
                                Text(
                                    "${String.format("%02d:%02d", reminder.hour, reminder.minute)} • ${reminder.category}",
                                    color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    if (reminder.days.size == 7) "كل يوم" else reminder.days.sorted().joinToString("، ") { dayName(it) },
                                    color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = reminder.enabled,
                                onCheckedChange = { enabled ->
                                    val index = reminders.indexOfFirst { it.id == reminder.id }
                                    if (index >= 0) reminders[index] = reminder.copy(enabled = enabled)
                                    persistAndSchedule()
                                }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                editingReminderId = reminder.id
                                draftTitle = reminder.title
                                draftCategory = reminder.category
                                draftHour = reminder.hour
                                draftMinute = reminder.minute
                                selectedDays.clear()
                                selectedDays.addAll(reminder.days)
                                showEditor = true
                            }) { Text("تعديل") }
                            TextButton(onClick = { removeReminder(reminder) }) { Text("حذف", color = Color(0xFF8A3C33)) }
                        }
                    }
                }
            }
        }

        if (feedback.isNotBlank()) Text(feedback, color = RoutineGreen, style = MaterialTheme.typography.bodySmall)

        RoutineReminderLearningCard(category = "استيقاظ")
        RoutineReminderLearningCard(category = "وجبة")
        RoutineReminderLearningCard(category = "نوم")

        Card(colors = CardDefaults.cardColors(containerColor = RoutineCream)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("دقة المنبه", fontWeight = FontWeight.Bold, color = RoutineGreen)
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
                Text(
                    if (exactAllowed) "إذن المنبهات الدقيقة متاح على جهازك."
                    else "قد تتأخر بعض التذكيرات بسبب إعدادات توفير البطارية. يمكنك السماح بالمنبهات الدقيقة من إعدادات النظام.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAllowed) {
                    OutlinedButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) { Text("فتح إذن المنبهات الدقيقة") }
                }
                Text(
                    "على Android 13 وما بعده، يلزم أيضًا السماح بإشعارات التطبيق من إعدادات النظام كي تصلك التذكيرات.",
                    color = Color(0xFF765B38), style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }) { Text("إعدادات إشعارات التطبيق") }
            }
        }

        Text(
            "لا يشترط التطبيق قراءة الذكر لإيقاف المنبه. يمكنك فتح التذكرة أو تأجيلها أو إيقاف التذكير.",
            color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall
        )
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false; editingReminderId = null },
            title = { Text("إضافة تذكير") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it.take(50) },
                        label = { Text("اسم التذكير") },
                        singleLine = true
                    )
                    Text("نوع التذكرة", color = RoutineGreen, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("استيقاظ", "وجبة", "نوم", "مخصص").forEach { category ->
                            FilterChip(
                                selected = draftCategory == category,
                                onClick = { draftCategory = category },
                                label = { Text(category, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedButton(onClick = {
                        android.app.TimePickerDialog(
                            context,
                            { _, hour, minute -> draftHour = hour; draftMinute = minute },
                            draftHour,
                            draftMinute,
                            true
                        ).show()
                    }) {
                        Text("الوقت: ${String.format("%02d:%02d", draftHour, draftMinute)}")
                    }
                    Text("أيام التكرار", color = RoutineGreen, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        (1..7).forEach { day ->
                            FilterChip(
                                selected = day in selectedDays,
                                onClick = {
                                    if (day in selectedDays) selectedDays.remove(day) else selectedDays.add(day)
                                },
                                label = { Text(dayNameShort(day), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Text("لن يرن المنبه في الأيام غير المحددة.", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draftTitle.isNotBlank() && selectedDays.isNotEmpty()) {
                        val id = editingReminderId ?: UUID.randomUUID().toString()
                        val old = reminders.firstOrNull { it.id == id }
                        val updated = RoutineReminder(
                            id = id,
                            title = draftTitle.trim(),
                            category = draftCategory,
                            hour = draftHour,
                            minute = draftMinute,
                            days = selectedDays.toSet(),
                            enabled = old?.enabled ?: true
                        )
                        val existingIndex = reminders.indexOfFirst { it.id == id }
                        if (existingIndex >= 0) reminders[existingIndex] = updated else reminders.add(updated)
                        RoutineReminderStore.save(prefs, reminders)
                        RoutineAlarmScheduler.scheduleNext(context, updated)
                        feedback = if (old == null) "تمت إضافة التذكير." else "تم تحديث التذكير."
                        editingReminderId = null
                        showEditor = false
                    } else {
                        feedback = "اكتب اسمًا للتذكير واختر يومًا واحدًا على الأقل."
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false; editingReminderId = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun RoutineReminderLearningCard(category: String) {
    val content = when (category) {
        "استيقاظ" -> Triple(
            "عند الاستيقاظ",
            "«الحمد لله الذي أحيانا بعدما أماتنا وإليه النشور»",
            "صحيح البخاري (6325) — ذكر الاستيقاظ"
        )
        "وجبة" -> Triple(
            "من هدي الطعام",
            "«سم الله، وكل بيمينك، وكل مما يليك»",
            "صحيح البخاري وصحيح مسلم — رياض الصالحين (727)"
        )
        else -> Triple(
            "قبل النوم",
            "«باسمك اللهم أموت وأحيا»",
            "صحيح البخاري (6325)"
        )
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(content.first, color = RoutineGreen, fontWeight = FontWeight.Bold)
            Text(content.second, style = MaterialTheme.typography.titleMedium)
            Text(content.third, color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
            if (category == "وجبة") {
                HorizontalDivider(color = RoutinePale)
                Text("معلومة عن الطعام الذي أحبه النبي ﷺ", color = RoutineGreen, fontWeight = FontWeight.SemiBold)
                Text("ورد أنه ﷺ كان يحب الحلواء والعسل (جامع الترمذي 1831)، كما ورد أنه كان يعجبه القرع في خبر الثريد (سنن ابن ماجه 3303).")
                Text("هذه أخبار عن طعام ورد في السنة، وليست خطة غذائية أو توصية طبية.", color = Color(0xFF765B38), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatRoutineDuration(milliseconds: Long): String {
    val totalMinutes = (milliseconds.coerceAtLeast(0L) / 60_000L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours} ساعة و${minutes} دقيقة" else "${minutes} دقيقة"
}

private fun dayName(day: Int): String = when (day) {
    1 -> "الإثنين"
    2 -> "الثلاثاء"
    3 -> "الأربعاء"
    4 -> "الخميس"
    5 -> "الجمعة"
    6 -> "السبت"
    else -> "الأحد"
}

private fun dayNameShort(day: Int): String = when (day) {
    1 -> "ن"
    2 -> "ث"
    3 -> "ر"
    4 -> "خ"
    5 -> "ج"
    6 -> "س"
    else -> "ح"
}
