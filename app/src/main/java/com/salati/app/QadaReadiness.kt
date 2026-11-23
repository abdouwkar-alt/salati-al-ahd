package com.salati.app

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val ReadinessGreen = Color(0xFF164B3A)
private val ReadinessPale = Color(0xFFE5EBDD)
private val ReadinessPrayers = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
private val ReadinessSunnahSlots = listOf("قبل الفجر", "قبل الظهر", "بعد الظهر", "بعد المغرب", "بعد العشاء")

object QadaReadiness {
    fun isUnlocked(prefs: SharedPreferences, today: LocalDate): Boolean {
        if (prefs.getBoolean("qada_readiness_unlocked", false)) return true
        val streak = consecutivePrayerDays(prefs, today)
        val dates = (0L..29L).map { today.minusDays(it) }
        val houses = dates.count { date -> rawatibCount(prefs, date) >= 12 }
        val qiyamNights = dates.count { date -> qiyamVerses(prefs, date) >= 10 }
        return streak >= 30 && houses >= 1 && qiyamNights >= 10
    }

    private fun consecutivePrayerDays(prefs: SharedPreferences, today: LocalDate): Int {
        var count = 0
        for (offset in 0L..29L) {
            val date = today.minusDays(offset)
            val complete = ReadinessPrayers.all { prayer ->
                val status = prefs.getString("status_" + date + "_" + prayer, "لم أسجل بعد")
                !status.isNullOrBlank() && status != "لم أسجل بعد" && status != "لم أصلِّ"
            }
            if (!complete) break
            count++
        }
        return count
    }

    internal fun rawatibCount(prefs: SharedPreferences, date: LocalDate): Int =
        ReadinessSunnahSlots.sumOf { slot -> prefs.getInt("sunnah_slot_" + date + "_" + slot, 0).coerceAtLeast(0) }

    internal fun qiyamVerses(prefs: SharedPreferences, date: LocalDate): Int =
        prefs.getInt("qiyam_verses_" + date, if (date == LocalDate.now()) prefs.getInt("qiyam_verses", 0) else 0).coerceAtLeast(0)
}

@Composable
fun QadaReadinessScreen(
    prefs: SharedPreferences,
    today: LocalDate,
    onBack: () -> Unit
) {
    val dates = (0L..29L).map { today.minusDays(it) }
    val streak = run {
        var count = 0
        for (date in dates) {
            val complete = ReadinessPrayers.all { prayer ->
                val status = prefs.getString("status_" + date + "_" + prayer, "لم أسجل بعد")
                !status.isNullOrBlank() && status != "لم أسجل بعد" && status != "لم أصلِّ"
            }
            if (!complete) break
            count++
        }
        count
    }
    val houseDays = dates.count { QadaReadiness.rawatibCount(prefs, it) >= 12 }
    val qiyamNights = dates.count { QadaReadiness.qiyamVerses(prefs, it) >= 10 }
    val unlocked = streak >= 30 && houseDays >= 1 && qiyamNights >= 10
    if (unlocked) androidx.compose.runtime.SideEffect {
        prefs.edit().putBoolean("qada_readiness_unlocked", true).apply()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = ReadinessGreen)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الاستعداد لفتح باب القضاء", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("﴿لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا﴾", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("البقرة: 286", color = Color.White.copy(alpha = .8f))
                Text("ثلاثون يومًا متتابعًا لإثبات الاستمرار، وخلالها تحقق السنن الرواتب في يوم واحد على الأقل، وقيام الليل بعشر آيات في عشر ليالٍ على الأقل.", color = Color.White)
            }
        }
        ReadinessCard(
            title = "1. المحافظة على الفرائض",
            progress = "$streak / 30 يومًا متتابعًا",
            detail = "سجّل الصلوات الخمس كل يوم. يُحتسب ما سجلته سواء صليت في الوقت أو منفردًا أو في البيت أو قضيتها. إذا مر يوم دون اكتمال الصلوات الخمس، تبدأ سلسلة الثلاثين يومًا من اليوم التالي.",
            complete = streak >= 30
        )
        ReadinessCard(
            title = "2. بيت في الجنة — السنن الرواتب",
            progress = "$houseDays يومًا اكتملت فيه 12 ركعة",
            detail = "إذا سجلت الاثنتي عشرة ركعة الراتبة في يوم واحد، يُضاف بيت إلى سجل فضائل الحديث. وتُسجل بقية الركعات أيضًا في الإحصائيات، ولو لم تكتمل الاثنتا عشرة.",
            complete = houseDays >= 1
        )
        ReadinessCard(
            title = "3. قيام الليل",
            progress = "$qiyamNights / 10 ليالٍ بعشر آيات على الأقل",
            detail = "تُحتسب الليلة إذا سجلت قراءة عشر آيات أو أكثر في صلاة الليل. ويُعرض عدد الآيات والليالي المسجلة دون تحويلها إلى حكم على قبول العمل.",
            complete = qiyamNights >= 10
        )
        Card(colors = CardDefaults.cardColors(containerColor = ReadinessPale)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (unlocked) "اكتملت مراحل الاستعداد" else "تذكير", color = ReadinessGreen, fontWeight = FontWeight.Bold)
                Text("هذه مراحل تنظيمية وتحفيزية داخل التطبيق، وليست شروطًا شرعية لوجوب القضاء أو صحته. لا تمنعك من سؤال أهل العلم أو البدء في القضاء متى أردت. والإحصائيات تسجل ما أدخلته أنت، ولا تحكم على الإخلاص أو قبول العمل.")
                if (unlocked) {
                    Button(onClick = {
                        prefs.edit().putBoolean("qada_readiness_unlocked", true).apply()
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ReadinessGreen)) {
                        Text("فتح باب القضاء")
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("العودة إلى الصلاة") }
    }
}

@Composable
private fun ReadinessCard(title: String, progress: String, detail: String, complete: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = if (complete) ReadinessPale else Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = ReadinessGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(progress, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
            Text(if (complete) "اكتمل الشرط" else "قيد المتابعة", color = ReadinessGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}
