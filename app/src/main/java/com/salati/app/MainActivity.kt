package com.salati.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateListOf
import kotlin.math.roundToInt
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import java.time.*
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Green = Color(0xFF164B3A)
private val Cream = Color(0xFFF6F2E9)
private val PaleGreen = Color(0xFFE5EBDD)
private val PrayerNames = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
private val PrayerStatuses = listOf("لم أسجل بعد", "في الوقت جماعة", "في الوقت منفردًا", "في البيت منفردًا", "قضاء منفردًا", "قضاء جماعة", "بعد الوقت", "لم أصلِّ")

class MainActivity : ComponentActivity() {
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        getLocationAndSchedule()
        PrayerNotifications.scheduleGazaReminder(this)
        requestNotificationPermission()
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) AppUpdateWorker.schedule(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrayerNotifications.createChannel(this)
        AppUpdateWorker.schedule(this)
        getSharedPreferences("salati", MODE_PRIVATE).edit().putLong("app_session_started_at", 0L).apply()
        setContent { SalatiScreen(onFirstLaunchAccepted = { requestLocationPermissions() }, initialRoute = when { intent?.getBooleanExtra("open_daily_routine", false) == true -> "روتين_اليوم"; intent?.getBooleanExtra("open_awareness", false) == true -> "وعي"; else -> null }) }
        if (getSharedPreferences("salati", MODE_PRIVATE).getBoolean("intro_seen", false) && getSharedPreferences("salati", MODE_PRIVATE).getBoolean("experience_v2_onboarding_done", false)) requestLocationPermissions()
    }
    private fun requestLocationPermissions() {
        val needed = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (needed.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            permissions.launch(needed)
        } else {
            getLocationAndSchedule()
            PrayerNotifications.scheduleGazaReminder(this)
            requestNotificationPermission()
        }
    }
    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            val prefs = getSharedPreferences("salati", MODE_PRIVATE)
            if (!prefs.getBoolean("update_notification_permission_asked", false)) {
                prefs.edit().putBoolean("update_notification_permission_asked", true).apply()
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("salati", MODE_PRIVATE)
        if (prefs.getLong("app_session_started_at", 0L) == 0L) {
            prefs.edit().putLong("app_session_started_at", System.currentTimeMillis()).apply()
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) getLocationAndSchedule()
    }

    override fun onPause() {
        recordForegroundSession()
        super.onPause()
    }

    private fun recordForegroundSession() {
        val prefs = getSharedPreferences("salati", MODE_PRIVATE)
        val start = prefs.getLong("app_session_started_at", 0L)
        val end = System.currentTimeMillis()
        if (start <= 0L || end <= start) {
            prefs.edit().putLong("app_session_started_at", 0L).apply()
            return
        }
        val zone = ZoneId.systemDefault()
        var cursor = start
        while (cursor < end) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val nextDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val segmentEnd = minOf(end, nextDay)
            val key = "app_time_ms_" + date.toString()
            prefs.edit().putLong(key, prefs.getLong(key, 0L) + (segmentEnd - cursor)).apply()
            cursor = segmentEnd
        }
        prefs.edit()
            .putLong("app_last_session_ms", end - start)
            .putLong("app_session_started_at", 0L)
            .apply()
    }
    private fun getLocationAndSchedule() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return
        try {
            LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { loc ->
                    val prefs = getSharedPreferences("salati", MODE_PRIVATE)
                    val lat = loc?.latitude ?: prefs.getFloat("lat", 30.0444f).toDouble()
                    val lon = loc?.longitude ?: prefs.getFloat("lon", 31.2357f).toDouble()
                    prefs.edit().putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat()).apply()
                    PrayerNotifications.schedule(this, lat, lon)
                }
        } catch (_: SecurityException) { }
    }
}

@Composable
private fun GuideCard(title: String, items: List<SunnahItem>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Green)
            items.forEach { item ->
                Text(item.title, fontWeight = FontWeight.SemiBold)
                Text(item.category, color = Color(0xFF56715F), style = MaterialTheme.typography.labelMedium)
                if (item.detail.isNotBlank()) Text(item.detail)
                if (item.evidence.isNotBlank()) Text("الدليل: " + item.evidence, color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(color = Color(0xFFECE8DE))
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = { onValue(it.filter(Char::isDigit).take(4)) },
        label = { Text(label) }, modifier = modifier,
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun CounterCard(title: String, remaining: Int, unit: String, actionLabel: String, onAdd: () -> Unit, onUndo: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
            Text(remaining.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(unit + " متبقية")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(actionLabel) }
                OutlinedButton(onClick = onUndo) { Text("تراجع") }
            }
        }
    }
}

@Composable
fun SalatiScreen(onFirstLaunchAccepted: () -> Unit = {}, initialRoute: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("salati", 0) }
    var introSeen by remember { mutableStateOf(prefs.getBoolean("intro_seen", false)) }
    var experienceOnboardingDone by remember { mutableStateOf(prefs.getBoolean("experience_v2_onboarding_done", false)) }
    var qadaPrayersDone by remember { mutableIntStateOf(prefs.getInt("prayers_done", 0)) }
    var qadaFastsDone by remember { mutableIntStateOf(prefs.getInt("fasts_done", 0)) }
    val qadaDoneByPrayer = remember { androidx.compose.runtime.mutableStateMapOf<String, Int>().apply { PrayerNames.forEach { put(it, prefs.getInt("qada_done_" + it, 0)) } } }
    val missedByPrayer = remember { androidx.compose.runtime.mutableStateMapOf<String, Int>().apply { PrayerNames.forEach { put(it, prefs.getInt("missed_" + it, 0)) } } }
    var missedSinceInstall by remember { mutableIntStateOf(prefs.getInt("missed_since_install", 0)) }
    var prayerBase by remember { mutableIntStateOf(prefs.getInt("prayer_base", -1)) }
    var fastBase by remember { mutableIntStateOf(prefs.getInt("fast_base", -1)) }
    var mainTab by remember { mutableStateOf(initialRoute ?: "الرئيسية") }
    val navigationHistory = remember { mutableStateListOf<Pair<String, String>>() }
    var activeSection by remember { mutableStateOf("البداية") }
    fun navigateTo(destination: String) {
        if (destination != mainTab) navigationHistory.add(mainTab to activeSection)
        mainTab = destination
    }
    var sectionSubTab by remember { mutableStateOf("") }
    var calculatorView by remember { mutableStateOf("الحاسبة") }
    var qadaNoticeAccepted by remember { mutableStateOf(prefs.getBoolean("qada_notice_accepted", false)) }
    var qiyamRefresh by remember { mutableIntStateOf(0) }
    var allNotificationsEnabled by remember { mutableStateOf(prefs.getBoolean("all_notifications_enabled", true)) }
    var selectedPrayerGuide by remember { mutableStateOf("الفجر") }
    var sunnahTab by remember { mutableStateOf("سنن الصلاة") }
    var wuduFiqhTopic by remember { mutableStateOf("أركان الوضوء") }
    var prayerFiqhTopic by remember { mutableStateOf("شروط الصلاة") }
    var fiqhMadhhab by remember { mutableStateOf("الحنفي") }
    var clockNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notifyBefore by remember { mutableStateOf(prefs.getBoolean("notify_before", true)) }
    var notifyAt by remember { mutableStateOf(prefs.getBoolean("notify_at", true)) }
    var notifyGaza by remember { mutableStateOf(prefs.getBoolean("notify_gaza", true)) }
    var tasbeehCount by remember { mutableIntStateOf(prefs.getInt("tasbeeh_count", 0)) }
    var fastSection by remember { mutableStateOf("رمضان") }
    var fastRamadanCount by remember { mutableIntStateOf(prefs.getInt("fast_ramadan_count", 0)) }
    var fastShawwalCount by remember { mutableIntStateOf(prefs.getInt("fast_shawwal_count", 0)) }
    LaunchedEffect(Unit) { while (true) { delay(1_000L); clockNow = System.currentTimeMillis() } }
    val today = Instant.ofEpochMilli(clockNow).atZone(ZoneId.systemDefault()).toLocalDate()
    var duhaDone by remember(today) { mutableStateOf(prefs.getBoolean("duha_" + today, false)) }
    val sunnahSlots = listOf("قبل الفجر" to 2, "قبل الظهر" to 4, "بعد الظهر" to 2, "بعد المغرب" to 2, "بعد العشاء" to 2)
    val sunnahDoneBySlot = remember(today) {
        androidx.compose.runtime.mutableStateMapOf<String, Int>().apply {
            sunnahSlots.forEach { (slot, _) -> put(slot, prefs.getInt("sunnah_slot_" + today + "_" + slot, 0)) }
        }
    }
    val sunnahRakaatCount = sunnahDoneBySlot.values.sum()
    var fastLogged by remember(today) { mutableStateOf(prefs.getString("fast_type_" + today, "") ?: "") }
    var actionFeedback by remember { mutableStateOf("") }
    LaunchedEffect(actionFeedback) {
        if (actionFeedback.isNotBlank()) {
            delay(1_400L)
            actionFeedback = ""
        }
    }
    val tomorrow = today.plusDays(1)
    val lat = prefs.getFloat("lat", 30.0444f).toDouble()
    val lon = prefs.getFloat("lon", 31.2357f).toDouble()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            PrayerNotifications.schedule(context, lat, lon)
            PrayerNotifications.scheduleGazaReminder(context, notifyGaza)
        }
    }
    val todayTimes = remember(today, lat, lon) { PrayerTimes.forDate(today, lat, lon, ZoneId.systemDefault()) }
    val duhaStartTime = remember(today, lat, lon) { PrayerTimes.duhaStartTime(today, lat, lon, ZoneId.systemDefault()) }
    val tomorrowTimes = remember(tomorrow, lat, lon) { PrayerTimes.forDate(tomorrow, lat, lon, ZoneId.systemDefault()) }
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale("ar"))
    val nightStart = todayTimes.first { it.name == "المغرب" }.time
    val nightEnd = tomorrowTimes.first { it.name == "الفجر" }.time
    val lastThirdStart = nightStart + ((nightEnd - nightStart) * 2L / 3L)
    val nightWindow = "الثلث الأخير تقريبًا: " + Instant.ofEpochMilli(lastThirdStart).atZone(ZoneId.systemDefault()).format(timeFormatter)
    val nextPrayer = (todayTimes + tomorrowTimes).firstOrNull { it.time > clockNow } ?: tomorrowTimes.first()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))
    val hijriDateText = try { DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar")).format(java.time.chrono.HijrahDate.from(today)) } catch (_: Exception) { "التاريخ الهجري غير متاح" }
    val nextDuration = ((nextPrayer.time - clockNow).coerceAtLeast(0L) / 1000L)
    val countdown = "%02d:%02d:%02d".format(nextDuration / 3600, (nextDuration % 3600) / 60, nextDuration % 60)

    val dailyStatus: SnapshotStateMap<String, String> = remember(today) {
        androidx.compose.runtime.mutableStateMapOf<String, String>().apply {
            PrayerNames.forEach { name -> put(name, prefs.getString("status_" + today + "_" + name, "لم أسجل بعد") ?: "لم أسجل بعد") }
        }
    }
    val qadaPrayerTotalForSnapshot = qadaDoneByPrayer.values.sum()
    LaunchedEffect(qadaPrayerTotalForSnapshot, qadaFastsDone, today) {
        prefs.edit()
            .putInt("qada_prayer_snapshot_" + today, qadaPrayerTotalForSnapshot)
            .putInt("qada_fast_snapshot_" + today, qadaFastsDone)
            .apply()
    }

    var birthDay by remember { mutableStateOf("") }
    var birthMonth by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf("") }
    var pubertyAge by remember { mutableStateOf(prefs.getInt("puberty_age_hijri", 15).toString()) }
    var estimate by remember { mutableStateOf<QadaEstimate?>(null) }
    var calcError by remember { mutableStateOf("") }
    var deductOneYear by remember { mutableStateOf(false) }
    var performedPrayers by remember { mutableStateOf("0") }
    var partialFastDays by remember { mutableStateOf("0") }
    var ramadanChecks by remember { mutableStateOf(setOf<Int>()) }

    val prayerRemaining = if (prayerBase >= 0) (prayerBase + missedByPrayer.values.sum() - qadaDoneByPrayer.values.sum()).coerceAtLeast(0) else 0
    val fastRemaining = if (fastBase >= 0) (fastBase - qadaFastsDone).coerceAtLeast(0) else 0

    if (!experienceOnboardingDone) {
        ExperienceOnboardingScreen(
            onFinish = {
                prefs.edit().putBoolean("experience_v2_onboarding_done", true).apply()
                experienceOnboardingDone = true
                if (introSeen) onFirstLaunchAccepted()
            }
        )
    } else if (!introSeen) {
        FirstLaunchNotice {
            prefs.edit().putBoolean("intro_seen", true).apply()
            introSeen = true
            onFirstLaunchAccepted()
        }
    } else {
    val dhikrBursts = remember { mutableStateListOf<DhikrBurst>() }
    var dhikrBurstId by remember { mutableIntStateOf(0) }
    Surface(Modifier.fillMaxSize(), color = Cream) {
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                val starts = mutableMapOf<Long, Offset>()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        event.changes.forEach { change ->
                            if (change.pressed && !change.previousPressed) {
                                starts[change.id.value] = change.position
                            } else if (!change.pressed && change.previousPressed) {
                                val start = starts.remove(change.id.value)
                                if (start != null && (change.position - start).getDistance() < 18.dp.toPx() && change.isConsumed) {
                                    dhikrBurstId += 1
                                    dhikrBursts.add(DhikrBurst(dhikrBurstId, change.position))
                                }
                            }
                        }
                    }
                }
            }
        ) {
        if (mainTab == "الرئيسية") {
            Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeDashboardV2(
                    dateText = hijriDateText,
                    nextPrayerName = nextPrayer.name,
                    nextPrayerTime = Instant.ofEpochMilli(nextPrayer.time).atZone(ZoneId.systemDefault()).format(timeFormatter),
                    nextPrayerCountdown = countdown,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onOpen = { section, destination, subTab ->
                        activeSection = section
                        navigationHistory.clear()
                        mainTab = destination
                        sectionSubTab = subTab.ifBlank {
                            when (section) {
                                "الصلاة" -> "تسجيل الصلاة"
                                "الصوم" -> "الصيام"
                                "الأذكار" -> "مكتبة الأذكار"
                                "الدعاء" -> "مكتبة الأدعية"
                                "سنن الرسول ﷺ" -> "سنن الصلاة"
                                "تدبر القرآن" -> "المصحف والتدبر"
                                "التفكر" -> "التفكر"
                                "التفقه" -> "فقه الصلاة"
                                else -> ""
                            }
                        }
                    }
                )
                BottomNavigationBar(selected = when (mainTab) {
                    "وعي" -> "وعي"
                    "الصلاة", "الصوم", "السنن", "الأذكار", "الأدعية", "قيام الليل", "المصحف", "تدبر القرآن", "فقه الصلاة", "التعلم_الأسبوعي" -> "العبادة"
                    "الإنفاق", "روتين_اليوم", "صلة_الرحم" -> "الخير والناس"
                    else -> "الرئيسية"
                }, onSelect = { destination ->
                    navigationHistory.clear()
                    sectionSubTab = ""
                    when (destination) {
                        "الرئيسية" -> { activeSection = "البداية"; mainTab = "الرئيسية" }
                        "وعي" -> { activeSection = "وعي"; mainTab = "وعي" }
                        "العبادة" -> { activeSection = "العبادة"; mainTab = "الصلاة"; sectionSubTab = "تسجيل الصلاة" }
                        "الخير والناس" -> { activeSection = "الخير والناس"; mainTab = "الإنفاق" }
                    }
                })
            }
        } else {
        androidx.activity.compose.BackHandler(enabled = mainTab != "الرئيسية") {
            if (navigationHistory.isNotEmpty()) {
                val previous = navigationHistory.removeAt(navigationHistory.lastIndex)
                mainTab = previous.first
                activeSection = previous.second
            } else {
                mainTab = "الرئيسية"
                activeSection = "البداية"
                sectionSubTab = ""
            }
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (actionFeedback.isNotBlank()) {
                item { Text(actionFeedback, color = Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
            }
            if (mainTab != "الرئيسية" && mainTab != "الإحصائيات" && mainTab != "الإعدادات" && mainTab != "التعلم_الأسبوعي" && mainTab != "روتين_اليوم") {
                item {
                    SectionNavigation(
                        section = when {
                            activeSection == "الصلاة" && (mainTab == "بوابة_الصلاة" || mainTab == "فرض" || mainTab == "الصلاة" || mainTab.startsWith("فرض_")) -> "الصلاة"
                            activeSection == "الصلاة" && (mainTab == "النوافل" || mainTab.startsWith("نافلة_")) -> "النوافل"
                            activeSection == "الصلاة" && (mainTab == "قيام الليل" || mainTab.startsWith("قيام_")) -> "قيام الليل"
                            activeSection == "الصلاة" && mainTab.startsWith("القضاء") -> "القضاء"
                            else -> activeSection
                        },
                        selected = sectionSubTab,
                        onSelect = { label, destination ->
                            sectionSubTab = label
                            if (destination == "القضاء_الحاسبة") {
                                navigationHistory.add(mainTab to activeSection)
                                mainTab = "القضاء"
                                calculatorView = "الحاسبة"
                            } else {
                                navigateTo(destination)
                            }
                        }
                    )
                }
                if (activeSection != "الصلاة") item { SectionSketch(section = activeSection) }
            }

            if (mainTab == "بوابة_الصلاة") {
                item { Text("اختر أحد أبواب الصلاة من القائمة أعلاه.", color = Green, style = MaterialTheme.typography.bodyMedium) }
            } else if (mainTab.startsWith("فرض_")) {
                item { PrayerTopicLearningScreen(prayer = mainTab.removePrefix("فرض_"), isFard = true, prefs = prefs) }
            } else if (mainTab.startsWith("نافلة_")) {
                item { PrayerTopicLearningScreen(prayer = mainTab.removePrefix("نافلة_"), isFard = false, prefs = prefs) }
            } else if (mainTab == "قيام_السنن") {
                item { QiyamSunnahLearningScreen() }
            } else if (mainTab == "قيام_الفضل") {
                item { QiyamVirtuesLearningScreen() }
            } else if (mainTab == "قيام_السجل") {
                item { QiyamHistoryScreen(prefs = prefs) }
            } else if (mainTab == "قيام_إنهاء") {
                item { QiyamSessionFinishScreen(prefs = prefs) { qiyamRefresh++ } }
            } else if (mainTab == "الإنفاق") {
                item { RemembranceCareScreen(prefs = prefs) }
            } else if (mainTab == "التعلم_الأسبوعي") {
                item { WeeklyLearningScreen(prefs = prefs) }
            } else if (mainTab == "روتين_اليوم") {
                item { DailyRoutineScreen(prefs = prefs) }
            } else if (mainTab == "وعي") {
                item { AwarenessHubScreen(prefs = prefs) }
            } else if (mainTab == "التفكر") {
                item {
                    SectionPlaceholder(
                        title = "التفكر",
                        subtitle = "التفكر في آيات الله ومحاسبة النفس",
                        detail = "مساحة لتدوين الخواطر والأسئلة والتأملات، مع الفصل بين نص الآية والتفسير والتأمل الشخصي."
                    )
                }
            } else if (mainTab == "الصلاة") {
                item {
                    val upcomingReminder = when (nextPrayer.name) {
                        "الفجر" -> "مَن صلَّى الصبحَ فهو في ذمَّةِ الله." to "صحيح مسلم (657)"
                        "الظهر" -> "إذا اشتدَّ الحرُّ فأبرِدوا بالصلاة؛ فإن شدة الحر من فيح جهنم." to "صحيح البخاري (536)، وصحيح مسلم (616)"
                        "العصر" -> "مَن ترك صلاةَ العصرِ فقد حَبِطَ عملُه." to "صحيح البخاري (553)"
                        "المغرب" -> "لا تزال أمتي بخيرٍ ما عجَّلوا المغرب." to "سنن أبي داود (418)، وجامع الترمذي (172)"
                        "العشاء" -> "إن أثقلَ صلاةٍ على المنافقين صلاةُ العشاء وصلاةُ الفجر." to "صحيح البخاري (657)، وصحيح مسلم (651)"
                        else -> "اللهم أعنِّي على ذكرك وشكرك وحسن عبادتك." to "سنن أبي داود (1522)"
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Green)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("الصلاة القادمة", color = Color.White.copy(alpha = 0.85f))
                            Text(nextPrayer.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(Instant.ofEpochMilli(nextPrayer.time).atZone(ZoneId.systemDefault()).format(timeFormatter), color = Color.White)
                            Text("متبقي " + countdown, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.28f))
                            Text("تذكير قبل الصلاة", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelMedium)
                            Text("«" + upcomingReminder.first + "»", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                            Text(upcomingReminder.second, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("مواقيت الصلاة اليوم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green)
                            Text(today.format(dateFormatter), color = Color(0xFF6D716A))
                            todayTimes.forEach { prayer ->
                                var expanded by remember(today, prayer.name) { mutableStateOf(false) }
                                Column(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(prayer.name, fontWeight = FontWeight.SemiBold)
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(Instant.ofEpochMilli(prayer.time).atZone(ZoneId.systemDefault()).format(timeFormatter))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box {
                                                    TextButton(onClick = { expanded = true }) {
                                                        Text(dailyStatus[prayer.name] ?: "سجل الصلاة", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                                        PrayerStatuses.forEach { status ->
                                                            DropdownMenuItem(text = { Text(status) }, onClick = {
                                                                val name = prayer.name
                                                                val old = dailyStatus[name] ?: "لم أسجل بعد"
                                                                dailyStatus[name] = status
                                                                if (status != "لم أسجل بعد") actionFeedback = "أستغفر الله"
                                                                prefs.edit().putString("status_" + today + "_" + name, status).apply()
                                                                if (old != "لم أصلِّ" && status == "لم أصلِّ") {
                                                                    missedSinceInstall++
                                                                    missedByPrayer[name] = (missedByPrayer[name] ?: 0) + 1
                                                                    prefs.edit().putInt("missed_since_install", missedSinceInstall).putInt("missed_" + name, missedByPrayer[name] ?: 0).apply()
                                                                } else if (old == "لم أصلِّ" && status != "لم أصلِّ") {
                                                                    missedSinceInstall = (missedSinceInstall - 1).coerceAtLeast(0)
                                                                    missedByPrayer[name] = ((missedByPrayer[name] ?: 0) - 1).coerceAtLeast(0)
                                                                    prefs.edit().putInt("missed_since_install", missedSinceInstall).putInt("missed_" + name, missedByPrayer[name] ?: 0).apply()
                                                                }
                                                                expanded = false
                                                            })
                                                        }
                                                    }
                                                }
                                                TextButton(onClick = { selectedPrayerGuide = prayer.name; sunnahTab = "سنن الصلاة"; navigateTo("فرض_" + prayer.name) }) {
                                                    Text("السنن والأحاديث", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFFD4DDCE))
                                }
                                if (prayer.name == "العشاء") {
                                    Column(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("قيام الليل — نافلة", color = Green, fontWeight = FontWeight.SemiBold)
                                        Text(nightWindow + "، وهو تقدير للثلث الأخير من الليل.")
                                        Text("أفضلُ الصلاةِ بعدَ الفريضةِ صلاةُ الليلِ.", color = Green, style = MaterialTheme.typography.bodyMedium)
                                        Text("صحيح مسلم (1163).", color = Color(0xFF77736B), style = MaterialTheme.typography.labelSmall)
                                        TextButton(onClick = { navigateTo("قيام الليل") }) { Text("متابعة قيام الليل والسنن والأحاديث") }
                                    }
                                }
                            }
                            Text("المواقيت محسوبة محليًا بطريقة تقريبية؛ راجعها مع التقويم الرسمي.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF755F45))
                        }
                    }
                }
            } else if (mainTab == "القضاء" && !QadaReadiness.isUnlocked(prefs, today)) {
                item {
                    QadaReadinessScreen(
                        prefs = prefs,
                        today = today,
                        onBack = { mainTab = "الصلاة" }
                    )
                }
            } else if (mainTab == "القضاء" && calculatorView == "الحاسبة" && !qadaNoticeAccepted) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("﴿لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا﴾", color = Green, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("البقرة: 286", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                            Text("قضاء ما فات من الصلاة مسألة فقهية فيها تفصيل، ويختلف الحكم باختلاف سبب الفوات. النوم والنسيان لهما حكم، وترك الصلاة عمدًا له بحث وخلاف بين أهل العلم.")
                            Text("قول جمهور الفقهاء: وجوب قضاء الصلاة الفائتة، حتى مع كثرة الفوائت، مع مراعاة القدرة والمشقة وتنظيم القضاء بما لا يضيّع الفرائض الحاضرة.")
                            Text("ونُقل قول لبعض أهل العلم، منهم ابن حزم، واختيار لشيخ الإسلام ابن تيمية فيمن ترك الصلاة عمدًا ثم تاب: أنه لا يقضي ما ترك عمدًا، بل يتوب ويكثر من العمل الصالح. هذا قول مخالف للجمهور، ويُعرض هنا للتعريف بالخلاف لا لإصدار فتوى شخصية.")
                            Text("أما ما فات بنوم أو نسيان، فالأصل فيه القضاء عند الاستيقاظ أو التذكر، لحديث: «مَن نسي صلاةً فليصلِّ إذا ذكرها».")
                            HorizontalDivider()
                            Text("«إن الدين يسر، ولن يشاد الدين أحد إلا غلبه» — صحيح البخاري (39).")
                            Text("«ما أعددت لها كثير صلاة ولا صيام، ولكني أحب الله ورسوله» — صحيح البخاري (6171)، وصحيح مسلم (2639)، في قصة الرجل الذي قال إنه يحب الله ورسوله.")
                            Text("هذه الحاسبة أداة تقديرية لتنظيم ما تختار متابعته، وليست فتوى ولا حكمًا ملزمًا. لا تعتمد رقمًا آليًا قبل مراجعة ظروفك وسؤال عالم موثوق عند الحاجة. إن لم تكن مستعدًا الآن، يمكنك الرجوع لاحقًا.")
                            Button(onClick = {
                                qadaNoticeAccepted = true
                                prefs.edit().putBoolean("qada_notice_accepted", true).apply()
                            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("أفهم التنبيه وأريد المتابعة") }
                            OutlinedButton(onClick = { mainTab = "الصلاة" }, modifier = Modifier.fillMaxWidth()) { Text("أعود لاحقًا") }
                        }
                    }
                }
            } else if (mainTab == "القضاء" && calculatorView == "الحاسبة") {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = calculatorView == "الحاسبة", onClick = { calculatorView = "الحاسبة" }, label = { Text("الحاسبة") })
                        FilterChip(selected = calculatorView == "القضاء", onClick = { calculatorView = "القضاء"; mainTab = "القضاء" }, label = { Text("القضاء") })
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EDDF))) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("\u200F﴿وَالَّذِينَ جَاهَدُوا فِينَا لَنَهْدِيَنَّهُمْ سُبُلَنَا﴾\u200F", color = Green, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("العنكبوت: 69", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("حاسبة بداية التكليف", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("أدخل تاريخ ميلادك، وحدد سن البلوغ الذي تريد أن يبدأ منه الحساب. التطبيق لا يفرض سنًا واحدًا على الجميع.")
                            OutlinedTextField(value = pubertyAge, onValueChange = { pubertyAge = it.filter(Char::isDigit).take(2); pubertyAge.toIntOrNull()?.let { age -> if (age in 1..40) prefs.edit().putInt("puberty_age_hijri", age).apply() } }, label = { Text("سن البلوغ بالسنوات الهجرية (اختيارك)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            Text("أدخل السن الذي تراه منطبقًا على حالتك وفق ما تعلمه أو ما ثبت لديك. هذا اختيار شخصي للحساب وليس حكمًا شرعيًا من التطبيق.", color = Color(0xFF755F45), style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NumberField("اليوم", birthDay, { birthDay = it }, Modifier.weight(1f))
                                NumberField("الشهر", birthMonth, { birthMonth = it }, Modifier.weight(1f))
                                NumberField("السنة", birthYear, { birthYear = it }, Modifier.weight(1.4f))
                            }
                            Button(onClick = {
                                calcError = ""
                                try {
                                    val date = LocalDate.of(birthYear.toInt(), birthMonth.toInt(), birthDay.toInt())
                                    val selectedAge = pubertyAge.toIntOrNull() ?: throw IllegalArgumentException("أدخل سن البلوغ بالأرقام")
                                    require(selectedAge in 1..40) { "سن البلوغ يجب أن يكون بين 1 و40 سنة" }
                                    prefs.edit().putInt("puberty_age_hijri", selectedAge).apply()
                                    estimate = QadaCalculator.calculate(date, today, taklifAgeYears = selectedAge)
                                    ramadanChecks = estimate!!.ramadans.filter { prefs.getBoolean("ramadan_" + it.hijriYear, false) }.map { it.hijriYear }.toSet()
                                } catch (e: Exception) {
                                    calcError = e.message ?: "راجع تاريخ الميلاد والبيانات المدخلة."
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth()) { Text("احسب بداية التكليف") }
                            if (calcError.isNotBlank()) Text(calcError, color = Color(0xFFB3261E))
                            estimate?.let { result ->
                                HorizontalDivider()
                                Text("تاريخ الميلاد الهجري: " + QadaCalculator.hijriLabel(result.birthHijri), fontWeight = FontWeight.SemiBold)
                                Text("بداية الحساب حسب السن الذي اخترته (" + pubertyAge + " سنة هجرية): " + QadaCalculator.hijriLabel(result.taklifHijri), fontWeight = FontWeight.SemiBold)
                                Text("الموافق ميلاديًا: " + result.taklifDate.toString())
                                Text("أيام الحساب حتى اليوم: " + result.prayerDays)
                                Text("إجمالي الفروض قبل الخصم: " + result.totalPrayers, color = Green, fontWeight = FontWeight.Bold)
                                HorizontalDivider()
                                Text("التقويم المستخدم: ضبط مصري مبدئي بإضافة يوم واحد لجميع التحويلات. هذا ضبط حسابي تقريبي وليس سجلًا رسميًا لرؤية الهلال؛ راجع التاريخ المحلي عند الحاجة.", color = Color(0xFF755F45), style = MaterialTheme.typography.bodySmall)
                                Text("التخفيف وتقدير ما سبق أداؤه", style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = deductOneYear, onCheckedChange = { deductOneYear = it })
                                    Text("خصم سنة هجرية تقديرية من الصلوات (1770 فرضًا)")
                                }
                                OutlinedTextField(value = performedPrayers, onValueChange = { performedPrayers = it.filter(Char::isDigit) }, label = { Text("صلوات سابقة تتذكر أنك أديتها") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                HorizontalDivider()
                                Text("رمضانات الفترة المحسوبة", style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                                Text("علّم كل رمضان صمته كاملًا. الأيام الجزئية أدخلها في الحقل التالي.")
                                result.ramadans.forEach { period ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val checked = ramadanChecks.contains(period.hijriYear)
                                        Checkbox(checked = checked, onCheckedChange = { on ->
                                            ramadanChecks = if (on) ramadanChecks + period.hijriYear else ramadanChecks - period.hijriYear
                                            prefs.edit().putBoolean("ramadan_" + period.hijriYear, on).apply()
                                        })
                                        Text("رمضان " + period.hijriYear + " هـ (" + period.days + " يوم)")
                                    }
                                }
                                OutlinedTextField(value = partialFastDays, onValueChange = { partialFastDays = it.filter(Char::isDigit) }, label = { Text("أيام صمتها متفرقة خارج الرمضانات الكاملة") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                val checkedRamadanDays = result.ramadans.filter { ramadanChecks.contains(it.hijriYear) }.sumOf { it.days }
                                val prayerDeduction = (if (deductOneYear) result.oneHijriYearDays * 5 else 0) + (performedPrayers.toIntOrNull() ?: 0)
                                val newPrayerBase = (result.totalPrayers - prayerDeduction).coerceAtLeast(0).toInt()
                                val newFastBase = (result.totalRamadanDays - checkedRamadanDays - (partialFastDays.toIntOrNull() ?: 0)).coerceAtLeast(0)
                                Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text("النتيجة التقديرية", fontWeight = FontWeight.Bold, color = Green)
                                        Text("قضاء الصلوات: " + newPrayerBase + " فرض")
                                        Text("إجمالي أيام رمضان: " + result.totalRamadanDays)
                                        Text("أيام الصيام المتبقية: " + newFastBase + " يوم")
                                    }
                                }
                                Button(onClick = {
                                    prayerBase = newPrayerBase
                                    fastBase = newFastBase
                                    prefs.edit().putInt("prayer_base", prayerBase).putInt("fast_base", fastBase).apply()
                                    mainTab = "القضاء"
                                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("اعتماد النتيجة وبدء المتابعة") }
                            }
                        }
                    }
                }
            } else if (mainTab == "القضاء" && calculatorView == "القضاء") {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = calculatorView == "الحاسبة", onClick = { calculatorView = "الحاسبة"; mainTab = "القضاء" }, label = { Text("الحاسبة") })
                        FilterChip(selected = calculatorView == "القضاء", onClick = { calculatorView = "القضاء" }, label = { Text("القضاء") })
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE7EDDF))) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("الْمُجَاهِدُ مَنْ جَاهَدَ نَفْسَهُ فِي طَاعَةِ اللَّهِ", color = Green, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("رواه أحمد (23967)، وأصل الحديث في سنن الترمذي (1621)، وقال الترمذي: حسن صحيح.", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    if (prayerBase < 0 || fastBase < 0) {
                        Card { Column(Modifier.padding(16.dp)) { Text("لم يتم إعداد رصيد القضاء بعد."); Button(onClick = { calculatorView = "الحاسبة"; mainTab = "القضاء" }) { Text("افتح الحاسبة") } } }
                    } else {
                        Text("قضاء كل صلاة على حدة", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                        PrayerNames.forEachIndexed { index, prayer ->
                            val baseForPrayer = (prayerBase / 5) + if (index < prayerBase % 5) 1 else 0
                            val missed = missedByPrayer[prayer] ?: 0
                            val done = qadaDoneByPrayer[prayer] ?: 0
                            val remaining = (baseForPrayer + missed - done).coerceAtLeast(0)
                            CounterCard("قضاء " + prayer, remaining, "فرض", "سجّلت قضاء " + prayer,
                                onAdd = { qadaDoneByPrayer[prayer] = done + 1; qadaPrayersDone = qadaDoneByPrayer.values.sum(); prefs.edit().putInt("qada_done_" + prayer, done + 1).putInt("prayers_done", qadaPrayersDone).apply() },
                                onUndo = { if (done > 0) { qadaDoneByPrayer[prayer] = done - 1; qadaPrayersDone = qadaDoneByPrayer.values.sum(); prefs.edit().putInt("qada_done_" + prayer, done - 1).putInt("prayers_done", qadaPrayersDone).apply() } })
                        }
                        CounterCard("قضاء الصيام", fastRemaining, "يوم", "سجّل يوم صيام قضيته",
                            onAdd = { qadaFastsDone++; prefs.edit().putInt("fasts_done", qadaFastsDone).apply() },
                            onUndo = { if (qadaFastsDone > 0) { qadaFastsDone--; prefs.edit().putInt("fasts_done", qadaFastsDone).apply() } })
                        Text("الرصيد = الفوائت المقدرة + ما تسجله من صلوات فائتة يوميًا - ما قضيته.")
                        TextButton(onClick = { calculatorView = "الحاسبة"; mainTab = "القضاء" }) { Text("مراجعة الحسبة") }
                    }
                }
            } else if (mainTab == "فقه الوضوء" || mainTab == "فقه الصلاة") {
                item {
                    val isWuduSection = mainTab == "فقه الوضوء"
                    val domainTopics = if (isWuduSection) {
                        listOf("أركان الوضوء", "سنن الوضوء", "نواقض الوضوء")
                    } else {
                        listOf("شروط الصلاة", "أركان الصلاة", "واجبات الصلاة", "سنن الصلاة", "مبطلات الصلاة")
                    }
                    val selectedTopic = if (isWuduSection) wuduFiqhTopic else prayerFiqhTopic
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (isWuduSection) "فقه الوضوء" else "فقه الصلاة", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                        Text("اختر الباب الفقهي ثم المذهب لعرض خلاصة الأحكام.", color = Green)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            domainTopics.forEach { topic ->
                                FilterChip(
                                    selected = selectedTopic == topic,
                                    onClick = { if (isWuduSection) wuduFiqhTopic = topic else prayerFiqhTopic = topic },
                                    label = { Text(topic) }
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("الحنفي", "المالكي", "الشافعي", "الحنبلي").forEach { madhhab ->
                                FilterChip(selected = fiqhMadhhab == madhhab, onClick = { fiqhMadhhab = madhhab }, label = { Text(madhhab) })
                            }
                        }
                        val entry = FiqhGuide.get(selectedTopic, fiqhMadhhab)
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(selectedTopic + " – المذهب " + fiqhMadhhab, style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                                Text(entry.summary)
                                Text("المراجع: " + entry.source, color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                                Button(onClick = {
                                    val query = Uri.encode(selectedTopic + " " + fiqhMadhhab + " شرح تعليمي")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + query)))
                                }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("مشاهدة شرح فيديو على يوتيوب") }
                                Text("تنبيه علمي مهم: هذا القسم مرجع تعريفي مساعد، وليس فتوى ولا مصدرًا نهائيًا للحكم الشرعي. قد تختلف التفاصيل والترجيحات، ويجب مراجعة المعلومات مع عالم مؤهل أو كتاب معتمد في المذهب قبل العمل بها.", color = Color(0xFF8A4B19), style = MaterialTheme.typography.bodySmall)
                                Text("المحتوى الحالي مختصر، ورابط الفيديو يفتح نتائج بحث للموضوع والمذهب المختارين؛ راجع هوية المحاضر ومصدره العلمي.", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else if (mainTab == "السنن_الصلوات") {
                item { PrayerVirtuesLearningScreen() }
            } else if (mainTab == "السنن") {
                item { NawafilLearningScreen() }
            } else if (mainTab == "النوافل") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("استثمار يومي في الجنة", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("قال رسول الله ﷺ: «ما من عبد مسلم يصلي لله كل يوم ثنتي عشرة ركعة تطوعًا غير فريضة، إلا بنى الله له بيتًا في الجنة».")
                            Text("صحيح مسلم (728)", color = Color(0xFF6D716A), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("متابعة السنن الرواتب اليوم", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("سجّل كل سنة بعد أدائها. يتجدد العداد يوميًا.")
                            Text(sunnahRakaatCount.toString() + " من 12 ركعة", style = MaterialTheme.typography.headlineMedium, color = Green, fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(progress = { (sunnahRakaatCount.toFloat() / 12f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Green)
                            sunnahSlots.forEach { (slot, target) ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(slot + " — " + (sunnahDoneBySlot[slot] ?: 0) + "/" + target)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = {
                                            val next = ((sunnahDoneBySlot[slot] ?: 0) + 2).coerceAtMost(target)
                                            sunnahDoneBySlot[slot] = next
                                            actionFeedback = "أستغفر الله"
                                            prefs.edit().putInt("sunnah_slot_" + today + "_" + slot, next).apply()
                                        }, enabled = (sunnahDoneBySlot[slot] ?: 0) < target) { Text("+2") }
                                        TextButton(onClick = {
                                            val next = ((sunnahDoneBySlot[slot] ?: 0) - 2).coerceAtLeast(0)
                                            sunnahDoneBySlot[slot] = next
                                            prefs.edit().putInt("sunnah_slot_" + today + "_" + slot, next).apply()
                                        }, enabled = (sunnahDoneBySlot[slot] ?: 0) > 0) { Text("تراجع") }
                                    }
                                }
                            }
                            Text("قد تختلف تفاصيل الرواتب وعددها المؤكد باختلاف المذاهب؛ هذا العداد لمتابعة الاثنتي عشرة ركعة الواردة في الحديث، وليس حكمًا على قبول العبادة.", color = Color(0xFF755F45), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

            } else if (mainTab == "الصوم") {
                item {
                    val hijriToday = HijrahDate.from(today)
                    val hijriMonth = hijriToday.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                    val hijriYear = hijriToday.get(java.time.temporal.ChronoField.YEAR)
                    val ramadanYear = if (hijriMonth > 9) hijriYear + 1 else hijriYear
                    val ramadanStart = LocalDate.ofEpochDay(HijrahDate.of(ramadanYear, 9, 1).toEpochDay())
                    val daysToRamadan = java.time.temporal.ChronoUnit.DAYS.between(today, ramadanStart).coerceAtLeast(0)
                    Card(colors = CardDefaults.cardColors(containerColor = Green)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("رمضان القادم", color = Color.White.copy(alpha = 0.85f))
                            Text(if (hijriMonth == 9) "نحن في شهر رمضان" else "متبقي تقريبًا " + daysToRamadan + " يومًا", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("بداية الشهر محسوبة بالتقويم الهجري الحسابي في الجهاز، وقد تختلف عن إعلان الرؤية الرسمي في مصر بيوم.", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("رمضان", "شوال", "النوافل", "الفوائت").forEach { section ->
                            FilterChip(selected = fastSection == section, onClick = { fastSection = section }, label = { Text(section) })
                        }
                    }
                }
                if (fastSection == "الفوائت") {
                    item {
                        CounterCard(title = "قضاء الصيام", remaining = fastRemaining, unit = "يوم", actionLabel = "سجل يوم قضاء",
                            onAdd = { qadaFastsDone++; prefs.edit().putInt("fasts_done", qadaFastsDone).apply() },
                            onUndo = { if (qadaFastsDone > 0) { qadaFastsDone--; prefs.edit().putInt("fasts_done", qadaFastsDone).apply() } })
                        TextButton(onClick = { mainTab = "القضاء"; calculatorView = "القضاء" }) { Text("فتح حاسبة الفوائت") }
                    }
                } else {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(when (fastSection) { "رمضان" -> "متابعة صيام رمضان"; "شوال" -> "ستة أيام من شوال"; else -> "صيام التطوع" }, style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                                Text(when (fastSection) {
                                    "رمضان" -> "الأيام المسجلة من رمضان: " + fastRamadanCount
                                    "شوال" -> "الأيام المسجلة من شوال: " + fastShawwalCount + " من 6. سجّل أيام شوال التي صمتها."
                                    else -> "من السنن: الاثنين والخميس، وثلاثة أيام من كل شهر، ويوم عرفة لغير الحاج، وعاشوراء."
                                })
                                if (fastLogged.isNotBlank()) {
                                    Text("تم تسجيل اليوم: " + fastLogged, color = Green, fontWeight = FontWeight.SemiBold)
                                    OutlinedButton(onClick = {
                                        if (fastLogged == "رمضان") { fastRamadanCount = (fastRamadanCount - 1).coerceAtLeast(0); prefs.edit().putInt("fast_ramadan_count", fastRamadanCount).apply() }
                                        if (fastLogged == "شوال") { fastShawwalCount = (fastShawwalCount - 1).coerceAtLeast(0); prefs.edit().putInt("fast_shawwal_count", fastShawwalCount).apply() }
                                        fastLogged = ""
                                        prefs.edit().remove("fast_type_" + today).apply()
                                    }, modifier = Modifier.fillMaxWidth()) { Text("إلغاء تسجيل صيام اليوم") }
                                } else {
                                    Button(onClick = {
                                        fastLogged = fastSection
                                        actionFeedback = "أستغفر الله"
                                        prefs.edit().putString("fast_type_" + today, fastSection).apply()
                                        if (fastSection == "رمضان") { fastRamadanCount++; prefs.edit().putInt("fast_ramadan_count", fastRamadanCount).apply() }
                                        if (fastSection == "شوال") { fastShawwalCount++; prefs.edit().putInt("fast_shawwal_count", fastShawwalCount).apply() }
                                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("سجّلت صيام اليوم") }
                                }
                                if (fastSection == "شوال") Text("العدد المسجل تراكمي؛ راجع أنه داخل شهر شوال نفسه.", color = Color(0xFF755F45), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item {
                    val recentFasts = (0..29).mapNotNull { offset ->
                        val loggedDate = today.minusDays(offset.toLong())
                        val kind = prefs.getString("fast_type_" + loggedDate, null)
                        if (kind.isNullOrBlank()) null else Pair(loggedDate, kind)
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("سجل الصيام — آخر 30 يومًا", style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                            if (recentFasts.isEmpty()) Text("لم تسجل أيام صيام بعد.")
                            recentFasts.forEach { record -> Text(record.first.toString() + " — " + record.second) }
                        }
                    }
                }
                item {
                    GuideCard("مواسم وصيام مستحب", listOf(
                        SunnahItem("الاثنين والخميس", "صيام متكرر", "كان النبي ﷺ يتحرى صيام يومي الاثنين والخميس.", "جامع الترمذي 747؛ سنن النسائي 2360"),
                        SunnahItem("الأيام البيض", "13 و14 و15 من الشهر الهجري", "صيام ثلاثة أيام من كل شهر، ومنه الأيام البيض.", "صحيح البخاري 1981؛ صحيح مسلم 721"),
                        SunnahItem("عاشوراء", "العاشر من المحرم", "يُستحب صومه، ومعه التاسع مخالفةً لليهود.", "صحيح مسلم 1134"),
                        SunnahItem("يوم عرفة", "التاسع من ذي الحجة لغير الحاج", "صيامه يكفّر السنة الماضية والباقية كما ورد في الحديث.", "صحيح مسلم 1162"),
                        SunnahItem("ستة أيام من شوال", "بعد رمضان", "من صام رمضان ثم أتبعه ستًا من شوال كان كصيام الدهر.", "صحيح مسلم 1164"),
                        SunnahItem("أفضل الصيام", "صيام داود عليه السلام", "كان يصوم يومًا ويفطر يومًا.", "صحيح البخاري 1131؛ صحيح مسلم 1159")
                    ))
                }
            } else if (mainTab == "قيام الليل") {
                item {
                    val zone = ZoneId.systemDefault()
                    val nightStartText = Instant.ofEpochMilli(nightStart).atZone(zone).format(timeFormatter)
                    val nightEndText = Instant.ofEpochMilli(nightEnd).atZone(zone).format(timeFormatter)
                    val thirdStartText = Instant.ofEpochMilli(lastThirdStart).atZone(zone).format(timeFormatter)
                    val nightIsOpen = clockNow >= nightStart && clockNow < nightEnd
                    val savedSession = prefs.contains("qiyam_active_start_surah_name")
                    val startLabel = if (savedSession) "استكمال القراءة" else "ابدأ جلسة قيام الليل"
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(colors = CardDefaults.cardColors(containerColor = Green)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("قيام الليل", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    if (nightIsOpen) "وقت الليل الآن — يمكنك البدء بما تيسر."
                                    else "وقت قيام الليل يبدأ بعد المغرب ويمتد إلى طلوع الفجر.",
                                    color = Color.White.copy(alpha = .94f)
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = .28f))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("المغرب", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
                                        Text(nightStartText, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("الفجر", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
                                        Text(nightEndText, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Text("بداية الثلث الأخير تقريبًا: $thirdStartText", color = Color(0xFFF3D99F), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text("ابدأ من حيث أنت", color = Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text("لا يلزم انتظار الثلث الأخير؛ يمكنك أن تصلي من بعد العشاء إلى الفجر، وتختار ما يناسب قدرتك.")
                                Button(onClick = { navigateTo("قيام_الجلسة") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                                    Text(startLabel)
                                }
                                if (savedSession) {
                                    Text("موضع قراءة سابق محفوظ على هذا الجهاز. يمكنك استكماله أو إنهاء الجلسة الحالية.", color = Color(0xFF756A55), style = MaterialTheme.typography.bodySmall)
                                    OutlinedButton(onClick = { navigateTo("قيام_إنهاء") }, modifier = Modifier.fillMaxWidth()) {
                                        Text("إنهاء الجلسة وحفظها")
                                    }
                                }
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ماذا تريد أن تفعل؟", color = Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                OutlinedButton(onClick = { navigateTo("قيام_السنن") }, modifier = Modifier.fillMaxWidth()) { Text("تعرف على صفة الصلاة والوتر") }
                                OutlinedButton(onClick = { navigateTo("قيام_الفضل") }, modifier = Modifier.fillMaxWidth()) { Text("فضل قيام الليل وأدلته") }
                                OutlinedButton(onClick = { navigateTo("قيام_السجل") }, modifier = Modifier.fillMaxWidth()) { Text("مراجعة الجلسات السابقة") }
                            }
                        }
                        Text(
                            "الثلث الأخير وقت فاضل، لكنه ليس الوقت الوحيد لقيام الليل. الأوقات المعروضة تقديرية بحسب مواقيت الصلاة المحفوظة في التطبيق.",
                            color = Color(0xFF755F45), style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else if (mainTab == "قيام_الجلسة") {
                item {
                    QuranReaderSection(
                        prefs = prefs, context = context, verseByVerse = true, showSearch = false,
                        onSessionStart = { surahNumber, surahName, ayahNumber ->
                            val global = quranGlobalAyahIndex(surahNumber, ayahNumber)
                            prefs.edit().putInt("qiyam_active_start", global).putInt("qiyam_active_end", global)
                                .putInt("qiyam_active_verses", 1)
                                .putInt("qiyam_active_start_surah", surahNumber).putInt("qiyam_active_start_ayah", ayahNumber)
                                .putString("qiyam_active_start_surah_name", surahName)
                                .putInt("qiyam_active_end_surah", surahNumber).putInt("qiyam_active_end_ayah", ayahNumber)
                                .putString("qiyam_active_end_surah_name", surahName).apply()
                            qiyamRefresh++
                        },
                        onPositionSelected = { surahNumber, surahName, ayahNumber ->
                            val global = quranGlobalAyahIndex(surahNumber, ayahNumber)
                            val uniqueReadCount = prefs.getStringSet("qiyam_active_ayah_keys", emptySet())?.size ?: 1
                            prefs.edit().putInt("qiyam_active_end", global).putInt("qiyam_active_verses", uniqueReadCount)
                                .putInt("qiyam_active_end_surah", surahNumber).putInt("qiyam_active_end_ayah", ayahNumber)
                                .putString("qiyam_active_end_surah_name", surahName).apply()
                            qiyamRefresh++
                        },
                        onSessionFinish = {
                             val readCount = prefs.getStringSet("qiyam_active_ayah_keys", emptySet())?.size ?: 0
                             prefs.edit().putInt("qiyam_active_verses", readCount.coerceAtLeast(1)).apply()
                             navigateTo("قيام_إنهاء")
                         }
                    )
                }
            } else if (mainTab == "الإحصائيات") {
                item {
                    PrayerStatisticsScreen(
                        prefs = prefs,
                        today = today,
                        prayerBase = prayerBase,
                        qadaDoneByPrayer = qadaDoneByPrayer.toMap(),
                        missedByPrayer = missedByPrayer.toMap(),
                        fastBase = fastBase,
                        qadaFastsDone = qadaFastsDone
                    )
                }
            } else if (mainTab == "الأذكار") {
                item { AuthenticDhikrScreen(prefs = prefs) }
            } else if (mainTab == "الأدعية") {
                item { DuaLibraryScreen() }
            } else if (mainTab == "تدبر القرآن") {
                item { QuranReflectionScreen(prefs = prefs, context = context) }
            } else if (mainTab == "الإعدادات") {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("إعدادات التنبيهات", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                            Text("إشعارات متنوعة من أبواب الصلاة والنوافل والضحى وقيام الليل والأذكار والأدعية. يمكنك تشغيل جميع إشعارات التطبيق أو إيقافها.") 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = allNotificationsEnabled, onCheckedChange = { value ->
                                    allNotificationsEnabled = value
                                    notifyBefore = value
                                    notifyAt = value
                                    notifyGaza = value
                                    prefs.edit().putBoolean("all_notifications_enabled", value).putBoolean("notify_before", value).putBoolean("notify_at", value).putBoolean("notify_gaza", value).apply()
                                    if (value) PrayerNotifications.schedule(context, lat, lon) else PrayerNotifications.cancelAll(context)
                                    PrayerNotifications.scheduleGazaReminder(context, value)
                                })
                                Text(if (allNotificationsEnabled) "إشعارات التطبيق مفعّلة" else "إشعارات التطبيق متوقفة")
                            }

                            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                OutlinedButton(
                                    onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("السماح بإشعارات التطبيق") }
                                Text("لن تصل تذكيرات غزة أو الصلاة قبل السماح بإشعارات التطبيق من النظام.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A4B19))
                            }


                                            Text("عنوان الإشعار ثابت: الصلاة جامعة. ويظهر تحته حديث أو آية مرتبطة بالصلاة. لا يصدر التطبيق أذانًا صوتيًا.", color = Color(0xFF6D716A))
                        }
                    }
                }
                item { AppDataBackupPanel(prefs = prefs, context = context) }
            } else {
                item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("سنن الصلاة", "الاستيقاظ", "النوم", "السواك", "الطعام", "البيت", "المسجد").forEach { tab ->
                            FilterChip(selected = sunnahTab == tab, onClick = { sunnahTab = tab }, label = { Text(tab) })
                        }
                    }
                }
                when (sunnahTab) {
                    "سنن الصلاة" -> item { GuideCard("سنن عامة في الصلاة وسنن صلاة " + selectedPrayerGuide, SunnahGuide.generalPrayer + (SunnahGuide.prayerItems[selectedPrayerGuide] ?: emptyList()) + SunnahGuide.afterPrayer) }
                    "البيت" -> item { GuideCard("سنن وآداب البيت والدخول والخروج", SunnahGuide.home) }
                    "المسجد" -> item { GuideCard("أذكار وآداب المسجد", SunnahGuide.mosque) }
                    "الاستيقاظ" -> item { GuideCard("سنن وأذكار الاستيقاظ", SunnahGuide.waking) }
                    "النوم" -> item { GuideCard("سنن وأذكار النوم", SunnahGuide.sleep) }
                    "السواك" -> item { GuideCard("مواضع السواك", SunnahGuide.waking.filter { it.title.contains("السواك") } + SunnahGuide.beforePrayer.filter { it.title.contains("السواك") }) }
                    "الطعام" -> item { GuideCard("سنن الطعام", SunnahGuide.food) }
                    "القراءة في الصلاة" -> item {
                        GuideCard("ماذا أقرأ في الصلاة؟", listOf(
                            SunnahItem("سنة الفجر", "وردت سنة مخصوصة", "في الركعة الأولى بعد الفاتحة: سورة الكافرون، وفي الثانية: سورة الإخلاص.", "صحيح مسلم 726"),
                            SunnahItem("الفجر المفروضة", "هدي القراءة", "كان النبي ﷺ يطيل القراءة في الفجر، ووردت القراءة بطوال المفصل.", "صحيح مسلم 461"),
                            SunnahItem("الظهر والعصر", "هدي القراءة", "القراءة سرًا؛ وكان يطيل في الأوليين من الظهر، ويقرأ في العصر أقصر من الظهر.", "صحيح البخاري 759؛ صحيح مسلم 451"),
                            SunnahItem("المغرب والعشاء", "هدي القراءة", "يجهر في الأوليين؛ ووردت قراءات متفاوتة الطول، ولا تلزم سورة بعينها لكل صلاة.", "صحيح البخاري 763، 769")
                        ))
                    }
                    else -> item { GuideCard("السنن العملية", SunnahGuide.home + SunnahGuide.waking + SunnahGuide.sleep + SunnahGuide.food) }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
        BottomNavigationBar(
            selected = when (mainTab) {
                "وعي" -> "وعي"
                "الصلاة", "الصوم", "السنن", "الأذكار", "الأدعية", "قيام الليل", "المصحف" -> "العبادة"
                "الإنفاق", "روتين_اليوم", "صلة_الرحم" -> "الخير والناس"
                else -> "الرئيسية"
            },
            onSelect = { destination ->
                navigationHistory.clear()
                sectionSubTab = ""
                when (destination) {
                    "الرئيسية" -> { activeSection = "البداية"; mainTab = "الرئيسية" }
                    "وعي" -> { activeSection = "وعي"; mainTab = "وعي" }
                    "العبادة" -> { activeSection = "العبادة"; mainTab = "الصلاة"; sectionSubTab = "تسجيل الصلاة" }
                    "الخير والناس" -> { activeSection = "الخير والناس"; mainTab = "الإنفاق" }
                }
            }
        )
        }
        dhikrBursts.forEach { burst ->
            DhikrBurstLabel(burst = burst, onFinished = { dhikrBursts.remove(burst) })
        }
        }
    }
    }
    }
}

@Composable
private fun FirstLaunchNotice(onContinue: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Cream) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("قبل أن تبدأ", style = MaterialTheme.typography.headlineMedium, color = Green, fontWeight = FontWeight.Bold)
                Text("الصَّلَاةُ جَامِعَةٌ", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                Text("تطبيق مساعد على تذكّر الصلاة وتعلّم ما يتصل بها، وتنظيم بعض أعمال العبادة والتدبّر.")
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("هدف التطبيق", color = Green, fontWeight = FontWeight.Bold)
                        Text("أن يكون وسيلة تذكير وتعلّم ومتابعة، لا أن يحل محل العبادة نفسها أو يحوّلها إلى أرقام ومنافسة. استخدمه بما يعينك على الإخلاص والمداومة.")
                    }
                }
            }
            item {
                Text("تنبيهات مهمة", style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                Text("• المعلومات الشرعية والأحاديث جُمعت للتعريف والتذكير، وقد تحتاج بعض المسائل إلى مراجعة أهل العلم. التطبيق ليس جهة إفتاء، ولا يغني عن سؤال عالم مؤهل.")
                Text("• قد تختلف بعض أحكام الصلاة والسنن وتفاصيلها بين المذاهب الفقهية؛ راعينا الإشارة إلى الخلاف حيث يظهر، فلا تجعل التطبيق سببًا للإنكار على الآخرين.")
                Text("• مواقيت الصلاة المحسوبة تقديرية وتتأثر بالموقع وطريقة الحساب وإعدادات الجهاز؛ راجع تقويم بلدك أو الجهة الرسمية، ولا تعتمد على التطبيق وحده عند الشك.")
                Text("• نص القرآن يُعرض من مصدر رقمي ويُخزّن على الجهاز بعد تحميله؛ راجع المصحف المعتمد عند وجود أي اختلاف أو شك في الرسم أو الضبط.")
                Text("• التنبيهات تحتاج إلى أذونات الإشعارات، وقد تتأثر بإعدادات توفير البطارية أو قيود الجهاز. التطبيق لا يشغّل أذانًا صوتيًا.")
            }
            item {
                Text("الخصوصية والبيانات", style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                Text("سجل الصلاة ودفتر التدبّر والإعدادات تُحفظ محليًا على جهازك. لا توجد مزامنة سحابية لحساب شخصي في الوقت الحالي. إذا حذفت التطبيق أو بياناته فقد تفقد سجلك؛ استخدم النسخة الاحتياطية من الإعدادات واحتفظ بها في مكان آمن.")
                Text("يُستخدم إذن الموقع لحساب مواقيت الصلاة، ويمكنك رفضه واستخدام الموقع الافتراضي أو إدخال/تعديل الموقع لاحقًا بحسب ما يتيحه التطبيق.")
            }
            item {
                Text("تنبيه أخير", style = MaterialTheme.typography.titleMedium, color = Green, fontWeight = FontWeight.Bold)
                Text("هذا التطبيق عمل مساعد قابل للتطوير، وقد توجد أخطاء أو معلومات ناقصة. نرحب بالتنبيه عنها ومراجعتها، ولا تنسب كل ما يظهر فيه إلى الشرع قبل التحقق من دليله.")
            }
            item {
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                    Text("قرأت التنبيهات — ابدأ الاستخدام")
                }
            }
        }
    }
}

@Composable
private fun QuranReflectionScreen(prefs: android.content.SharedPreferences, context: android.content.Context) {
    val today = LocalDate.now().toString()
    var currentSurah by remember { mutableStateOf(prefs.getString("quran_surah", "البقرة") ?: "البقرة") }
    var currentAyah by remember { mutableStateOf(prefs.getString("quran_ayah", "1") ?: "1") }
    val dailyTarget = 10
    var enteredAyahCount by remember { mutableStateOf("") }
    var quranSection by remember { mutableStateOf("المصحف") }
    var progressDate by remember { mutableStateOf(prefs.getString("quran_daily_ayah_progress_date", today) ?: today) }
    var dailyProgress by remember { mutableIntStateOf(if (progressDate == today) prefs.getInt("quran_daily_ayah_progress", 0) else 0) }
    var sessionSurah by remember { mutableStateOf("البقرة") }
    var sessionAyah by remember { mutableStateOf("") }
    var reflection by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var deleteIndex by remember { mutableIntStateOf(-1) }
    var sessions by remember { mutableStateOf(prefs.getString("quran_sessions", "[]") ?: "[]") }
    fun persist(array: org.json.JSONArray) {
        sessions = array.toString()
        prefs.edit().putString("quran_sessions", sessions).apply()
    }
    val saved = try { org.json.JSONArray(sessions) } catch (_: Exception) { org.json.JSONArray() }
    if (progressDate != today) {
        LaunchedEffect(today) {
            progressDate = today
            dailyProgress = 0
            prefs.edit().putString("quran_daily_ayah_progress_date", today).putInt("quran_daily_ayah_progress", 0).apply()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = quranSection == "المصحف",
                onClick = { quranSection = "المصحف" },
                label = { Text("المصحف") }
            )
            FilterChip(
                selected = quranSection == "دفتر التدبّر",
                onClick = { quranSection = "دفتر التدبّر" },
                label = { Text("دفتر التدبّر") }
            )
        }
        if (quranSection == "المصحف") {
            QuranReaderSection(prefs = prefs, context = context, onPositionSelected = { surahNumber, surahName, ayahNumber ->
                currentSurah = surahName
                currentAyah = ayahNumber.toString()
                sessionSurah = surahName
                sessionAyah = ayahNumber.toString()
                prefs.edit().putInt("quran_surah_number", surahNumber)
                    .putString("quran_surah", surahName)
                    .putString("quran_ayah", ayahNumber.toString()).apply()
            })
        } else {
        Card(colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الورد اليومي المقترح", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                Text("الهدف: $dailyTarget آيات — قرأت اليوم: $dailyProgress آية")
                LinearProgressIndicator(progress = { (dailyProgress.toFloat() / dailyTarget).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Green)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = enteredAyahCount,
                        onValueChange = { enteredAyahCount = it.filter(Char::isDigit).take(4) },
                        label = { Text("عدد الآيات التي قرأتها") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Button(
                        onClick = {
                            val amount = enteredAyahCount.toIntOrNull()
                            if (amount != null && amount in 1..1000) {
                                dailyProgress += amount
                                enteredAyahCount = ""
                                prefs.edit().putString("quran_daily_ayah_progress_date", today)
                                    .putInt("quran_daily_ayah_progress", dailyProgress).apply()
                            }
                        },
                        enabled = enteredAyahCount.toIntOrNull()?.let { it in 1..1000 } == true,
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) { Text("تسجيل") }
                }
                Text(
                    if (dailyProgress >= dailyTarget) "أتممت الورد المقترح. الزيادة خير، من غير إلزام."
                    else "عشر آيات هدف مقترح للتدرّب على المداومة، وليس حدًا شرعيًا لازمًا. يتجدد العداد يوميًا.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("موضع القراءة الحالي", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = currentSurah, onValueChange = { currentSurah = it; prefs.edit().putString("quran_surah", it).apply() }, label = { Text("السورة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = currentAyah, onValueChange = { currentAyah = it; prefs.edit().putString("quran_ayah", it).apply() }, label = { Text("رقم الآية أو موضع القراءة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (editingIndex >= 0) "تعديل جلسة تدبّر" else "تسجيل جلسة تدبّر", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = sessionSurah, onValueChange = { sessionSurah = it }, label = { Text("السورة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = sessionAyah, onValueChange = { sessionAyah = it }, label = { Text("الآيات من - إلى (اختياري)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = reflection, onValueChange = { reflection = it }, label = { Text("ما الذي لفت انتباهك أو تدبّرته؟") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("أسئلة تحتاج إلى بحث أو مراجعة") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = action, onValueChange = { action = it }, label = { Text("كيف تريد العمل بما تدبّرته؟") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Button(onClick = {
                    if (sessionSurah.isNotBlank() && (reflection.isNotBlank() || question.isNotBlank() || action.isNotBlank())) {
                        val array = try { org.json.JSONArray(sessions) } catch (_: Exception) { org.json.JSONArray() }
                        val entry = org.json.JSONObject()
                        entry.put("surah", sessionSurah.trim()); entry.put("ayah", sessionAyah.trim())
                        entry.put("reflection", reflection.trim()); entry.put("question", question.trim()); entry.put("action", action.trim())
                        entry.put("date", if (editingIndex >= 0) (array.optJSONObject(editingIndex)?.optString("date") ?: today) else today)
                        if (editingIndex >= 0 && editingIndex < array.length()) array.put(editingIndex, entry) else array.put(entry)
                        persist(array)
                        currentSurah = sessionSurah.trim()
                        if (sessionAyah.isNotBlank()) currentAyah = sessionAyah.trim()
                        prefs.edit().putString("quran_surah", currentSurah).putString("quran_ayah", currentAyah).apply()
                        editingIndex = -1; sessionSurah = "البقرة"; sessionAyah = ""; reflection = ""; question = ""; action = ""
                    }
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(if (editingIndex >= 0) "حفظ التعديلات" else "حفظ جلسة التدبّر") }
                if (editingIndex >= 0) TextButton(onClick = { editingIndex = -1; sessionSurah = "البقرة"; sessionAyah = ""; reflection = ""; question = ""; action = "" }) { Text("إلغاء التعديل") }
                Text("خواطرك وأسئلتك الشخصية ليست تفسيرًا معتمدًا؛ راجع كتب التفسير وأهل العلم عند البحث عن معنى الآية.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF755F45))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("دفتر جلساتك السابقة", style = MaterialTheme.typography.titleLarge, color = Green, fontWeight = FontWeight.Bold)
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية من دفتر تدبّر القرآن")
                    putExtra(Intent.EXTRA_TEXT, sessions)
                }
                context.startActivity(Intent.createChooser(send, "تصدير نسخة احتياطية"))
            }) { Text("تصدير") }
        }
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("ابحث في السورة أو الملاحظات أو الأسئلة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        val matching = (0 until saved.length()).filter { index ->
            val e = saved.optJSONObject(index) ?: return@filter false
            val haystack = listOf("surah", "ayah", "reflection", "question", "action", "date").joinToString(" ") { e.optString(it) }
            haystack.contains(search.trim(), ignoreCase = true)
        }
        if (matching.isEmpty()) Text(if (search.isBlank()) "لا توجد جلسات محفوظة حتى الآن." else "لا توجد نتائج مطابقة.")
        matching.asReversed().forEach { index ->
            val entry = saved.optJSONObject(index) ?: return@forEach
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(entry.optString("surah") + if (entry.optString("ayah").isNotBlank()) " — " + entry.optString("ayah") else "", color = Green, fontWeight = FontWeight.Bold)
                    Text(entry.optString("date"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    if (entry.optString("reflection").isNotBlank()) Text("تدبّري: " + entry.optString("reflection"))
                    if (entry.optString("question").isNotBlank()) Text("سؤال للبحث: " + entry.optString("question"))
                    if (entry.optString("action").isNotBlank()) Text("عمل: " + entry.optString("action"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            editingIndex = index
                            sessionSurah = entry.optString("surah"); sessionAyah = entry.optString("ayah")
                            reflection = entry.optString("reflection"); question = entry.optString("question"); action = entry.optString("action")
                        }) { Text("تعديل") }
                        TextButton(onClick = { deleteIndex = index }) { Text("حذف", color = Color(0xFFB3261E)) }
                    }
                }
            }
        }
        }
    }
    if (deleteIndex >= 0) {
        AlertDialog(
            onDismissRequest = { deleteIndex = -1 },
            title = { Text("حذف جلسة التدبّر؟") },
            text = { Text("لن يمكن استعادة هذه الجلسة بعد حذفها.") },
            confirmButton = { TextButton(onClick = {
                val array = try { org.json.JSONArray(sessions) } catch (_: Exception) { org.json.JSONArray() }
                if (deleteIndex in 0 until array.length()) array.remove(deleteIndex)
                persist(array); if (editingIndex == deleteIndex) editingIndex = -1
                deleteIndex = -1
            }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { deleteIndex = -1 }) { Text("إلغاء") } }
        )
    }
}


@Composable
private fun ExperienceOnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple("الصلاة جامعة", "رفيق يذكّرك ويعينك على الفهم والعمل", "ليس التطبيق مقياسًا لإيمانك، ولا بديلًا عن أهل العلم. هو مساحة تجمع التذكير والعبادة والتعلّم والخير، وتترك لك حرية الاختيار."),
        Triple("أربعة أبواب واضحة", "كل باب له وظيفة محددة", "الرئيسية: تعرفك على يومك والصلاة القادمة.\nوعي: تتعلم من مصادر واضحة وتربط العلم بالواقع.\nالعبادة: الصلاة والقرآن والقيام والصيام والقضاء.\nالخير والناس: صلة الرحم والدعاء والإنفاق والتذكير."),
        Triple("ابدأ بما تحتاجه الآن", "لا يلزمك إعداد كل شيء", "تقدر تتصفح التطبيق دون مشاركة موقعك أو تفعيل الإشعارات. سنطلب الإذن فقط عندما تستخدم ميزة تحتاج إليه، وسنوضح لك السبب قبل ذلك.")
    )
    val current = pages[page]
    Surface(Modifier.fillMaxSize(), color = Color(0xFFF6F2E9)) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("دليل البداية", color = Color(0xFF765F3C), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onFinish) { Text("تخطي", color = Color(0xFF52645A)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(pages.size) { index ->
                        Surface(
                            Modifier.weight(1f).height(5.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = if (index <= page) Color(0xFF164B3A) else Color(0xFFD8D2C4)
                        ) {}
                    }
                }
                Spacer(Modifier.height(18.dp))
                Surface(
                    Modifier.size(76.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    color = Color(0xFFE5EBDD)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (page == 0) "ص" else if (page == 1) "٤" else "✓", color = Color(0xFF164B3A), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Text(current.first, color = Color(0xFF164B3A), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(current.second, color = Color(0xFF8A7048), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(current.third, color = Color(0xFF343A35), style = MaterialTheme.typography.bodyLarge, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25)
                if (page == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "الرئيسية" to "ملخص يومك وخطوتك التالية",
                            "وعي" to "تعلم منظم ومصادر واضحة",
                            "العبادة" to "أدوات عملية للعبادات",
                            "الخير والناس" to "تذكير بالعلاقات وأبواب الخير"
                        ).forEach { item ->
                            Surface(color = Color.White, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) {
                                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.first, color = Color(0xFF164B3A), fontWeight = FontWeight.Bold, modifier = Modifier.width(112.dp))
                                    Text(item.second, color = Color(0xFF555D55), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { if (page < pages.lastIndex) page++ else onFinish() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF164B3A)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) { Text(if (page == pages.lastIndex) "ابدأ استخدام التطبيق" else "التالي", style = MaterialTheme.typography.titleMedium) }
                if (page > 0) {
                    TextButton(onClick = { page-- }, modifier = Modifier.fillMaxWidth()) { Text("رجوع", color = Color(0xFF52645A)) }
                }
                Text("يمكنك فتح دليل البداية مرة أخرى من المساعدة داخل التطبيق.", color = Color(0xFF77766F), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
