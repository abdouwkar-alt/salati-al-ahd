package com.salati.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private data class QuranSurahData(
    val number: Int,
    val name: String,
    val englishName: String,
    val ayahs: List<QuranAyahData>
)

private data class QuranAyahData(val number: Int, val text: String)

private object QuranTextRepository {
    private const val ENDPOINT = "https://api.alquran.cloud/v1/quran/quran-uthmani"
    private const val CACHE_NAME = "quran-uthmani-alqurancloud-v1.json"

    fun load(context: Context, forceRefresh: Boolean = false): List<QuranSurahData> {
        val cache = File(context.filesDir, CACHE_NAME)
        if (!forceRefresh && cache.isFile && cache.length() > 100_000L) {
            try {
                return parse(cache.readText(Charsets.UTF_8))
            } catch (_: Exception) {
                cache.delete()
            }
        }

        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SalatiAndroid/1.1")
        }

        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IOException("لم يصل رد من مصدر النص القرآني.")
            if (status !in 200..299) throw IOException("تعذر تحميل النص القرآني (HTTP $status).")

            val verified = parse(response)
            val temporary = File(context.filesDir, "$CACHE_NAME.tmp")
            temporary.writeText(response, Charsets.UTF_8)
            if (cache.exists() && !cache.delete()) throw IOException("تعذر تحديث ملف النص المحفوظ.")
            if (!temporary.renameTo(cache)) {
                temporary.copyTo(cache, overwrite = true)
                temporary.delete()
            }
            return verified
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(raw: String): List<QuranSurahData> {
        val root = JSONObject(raw)
        if (root.optInt("code") != 200) throw IOException("مصدر النص القرآني أعاد استجابة غير مكتملة.")
        val data = root.getJSONObject("data")
        val editionId = data.optJSONObject("edition")?.optString("identifier").orEmpty()
        if (editionId.isNotBlank() && editionId != "quran-uthmani") {
            throw IOException("مصدر النص أعاد إصدارًا مختلفًا عن الرسم العثماني المطلوب.")
        }
        val array = data.getJSONArray("surahs")
        if (array.length() != 114) throw IOException("فشل التحقق من عدد سور القرآن.")

        var totalAyahs = 0
        val result = ArrayList<QuranSurahData>(114)
        for (surahIndex in 0 until array.length()) {
            val obj = array.getJSONObject(surahIndex)
            val number = obj.getInt("number")
            if (number != surahIndex + 1) throw IOException("ترتيب السور في البيانات غير صحيح.")
            val name = obj.getString("name")
            val ayahArray = obj.getJSONArray("ayahs")
            // بعض استجابات المصدر لا ترسل numberOfAyahs؛ عدد عناصر ayahs هو المرجع البنيوي هنا.
            val expectedAyahs = obj.optInt("numberOfAyahs", ayahArray.length())
            if (ayahArray.length() != expectedAyahs || expectedAyahs <= 0) throw IOException("فشل التحقق من آيات سورة $number.")

            val ayahs = ArrayList<QuranAyahData>(ayahArray.length())
            for (ayahIndex in 0 until ayahArray.length()) {
                val ayah = ayahArray.getJSONObject(ayahIndex)
                val ayahNumber = ayah.getInt("numberInSurah")
                val text = ayah.getString("text")
                if (ayahNumber != ayahIndex + 1 || text.isBlank()) {
                    throw IOException("فشل التحقق من نص آية في سورة $number.")
                }
                ayahs.add(QuranAyahData(ayahNumber, text))
            }
            totalAyahs += ayahs.size
            result.add(QuranSurahData(number, name, obj.optString("englishName"), ayahs))
        }
        if (totalAyahs != 6236) throw IOException("فشل التحقق من إجمالي آيات القرآن.")
        return result
    }
}


@Composable
fun QuranReaderSection(
    prefs: android.content.SharedPreferences,
    context: Context,
    onPositionSelected: (Int, String, Int) -> Unit,
    verseByVerse: Boolean = false,
    showSearch: Boolean = true,
    onSessionStart: ((Int, String, Int) -> Unit)? = null,
    onSessionFinish: (() -> Unit)? = null
) {
    var surahs by remember { mutableStateOf<List<QuranSurahData>>(emptyList()) }
    var selectedNumber by remember { mutableIntStateOf(prefs.getInt("quran_surah_number", 2).coerceIn(1, 114)) }
    var selectedAyah by remember { mutableIntStateOf(prefs.getString("quran_ayah", "1")?.toIntOrNull() ?: 1) }
    var search by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var readerOpen by remember { mutableStateOf(false) }
    var readingTheme by remember { mutableStateOf(prefs.getString("qiyam_reader_theme", "dark") ?: "dark") }
    var readAyahKeys by remember { mutableStateOf(prefs.getStringSet("qiyam_active_ayah_keys", emptySet())?.toSet() ?: emptySet()) }

    fun load(force: Boolean = false) {
        scope.launch {
            loading = true
            error = null
            try {
                surahs = withContext(Dispatchers.IO) { QuranTextRepository.load(context, force) }
                val selected = surahs.firstOrNull { it.number == selectedNumber } ?: surahs.first()
                selectedNumber = selected.number
                selectedAyah = selectedAyah.coerceIn(1, selected.ayahs.size)
            } catch (e: Exception) {
                error = if (e is IOException && !e.message.isNullOrBlank()) e.message else "تعذر تحميل المصحف أو التحقق من بياناته. تحقق من الاتصال ثم أعد المحاولة."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE5EBDD))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (verseByVerse) "مصحف قيام الليل" else "مصحف القرآن الكريم", style = MaterialTheme.typography.titleLarge, color = Color(0xFF164B3A), fontWeight = FontWeight.Bold)
            Text("فهرس السور والآيات بالنص العثماني. اضغط على الآية لتسجيل موضع قراءتك.")
            Text("مصدر النص العثماني: AlQuran Cloud — Islamic Network.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF56715F))
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF164B3A))
                Text("يُحمّل النص أو يُقرأ من النسخة المحفوظة على هذا الجهاز…")
            } else if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { load() }, modifier = Modifier.fillMaxWidth()) { Text("إعادة المحاولة") }
            } else {
                val current = surahs.firstOrNull { it.number == selectedNumber }
                OutlinedButton(onClick = { menuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (current == null) "اختر السورة" else "${current.number}. ${current.name}")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.heightIn(max = 460.dp)
                ) {
                    surahs.forEach { surah ->
                        DropdownMenuItem(
                            text = { Text("${surah.number}. ${surah.name}") },
                            onClick = {
                                selectedNumber = surah.number
                                selectedAyah = 1
                                search = ""
                                menuExpanded = false
                                prefs.edit().putInt("quran_surah_number", surah.number)
                                    .putString("quran_surah", surah.name)
                                    .putString("quran_ayah", "1").apply()
                                onPositionSelected(surah.number, surah.name, 1)
                            }
                        )
                    }
                }
                if (showSearch) OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("ابحث عن سورة في الفهرس") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (showSearch && search.isNotBlank()) {
                    surahs.filter { it.name.contains(search.trim(), ignoreCase = true) || it.englishName.contains(search.trim(), ignoreCase = true) }
                        .take(8)
                        .forEach { match ->
                            TextButton(onClick = {
                                selectedNumber = match.number
                                selectedAyah = 1
                                search = ""
                                prefs.edit().putInt("quran_surah_number", match.number)
                                    .putString("quran_surah", match.name)
                                    .putString("quran_ayah", "1").apply()
                                onPositionSelected(match.number, match.name, 1)
                            }) { Text("${match.number}. ${match.name}") }
                        }
                }
                if (current != null) {
                    if (verseByVerse) {
                        Text("اختر موضع البداية، ثم اختر مظهر المصحف قبل القراءة.", color = Color(0xFF555B54))
                        Text("موضع البداية", fontWeight = FontWeight.SemiBold, color = Color(0xFF164B3A))
                        Text("الآية: $selectedAyah (اتركها كما هي للبدء من أول السورة، أو اختر رقمًا)", style = MaterialTheme.typography.bodyMedium)
                        var ayahMenuExpanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { ayahMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("اختيار الآية: $selectedAyah")
                        }
                        DropdownMenu(expanded = ayahMenuExpanded, onDismissRequest = { ayahMenuExpanded = false }, modifier = Modifier.heightIn(max = 420.dp)) {
                            current.ayahs.forEach { item ->
                                DropdownMenuItem(text = { Text("الآية ${item.number}") }, onClick = {
                                    selectedAyah = item.number
                                    ayahMenuExpanded = false
                                    prefs.edit().putInt("quran_surah_number", current.number).putString("quran_surah", current.name).putString("quran_ayah", selectedAyah.toString()).apply()
                                    onPositionSelected(current.number, current.name, selectedAyah)
                                })
                            }
                        }
                        Text("مظهر المصحف", fontWeight = FontWeight.SemiBold, color = Color(0xFF164B3A))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("dark" to "أسود", "light" to "أبيض", "green" to "أخضر").forEach { (key, label) ->
                                FilterChip(selected = readingTheme == key, onClick = {
                                    readingTheme = key
                                    prefs.edit().putString("qiyam_reader_theme", key).apply()
                                }, label = { Text(label) })
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFD6E5D7))) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("فضل قيام الليل", color = Color(0xFF164B3A), fontWeight = FontWeight.Bold)
                                Text("أَفْضَلُ الصَّلَاةِ بَعْدَ الْفَرِيضَةِ صَلَاةُ اللَّيْلِ.")
                                Text("صحيح مسلم، حديث ١١٦٣", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        Button(onClick = {
                            readAyahKeys = setOf("${current.number}:${selectedAyah}")
                            prefs.edit().putStringSet("qiyam_active_ayah_keys", readAyahKeys).apply()
                            onSessionStart?.invoke(current.number, current.name, selectedAyah)
                            readerOpen = true
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF164B3A))) {
                            Text("ابدأ القراءة")
                        }
                    } else {
                        Text("اضغط على أي آية لتثبيت موضع القراءة الحالي.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5F665F))
                        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            current.ayahs.forEach { ayah ->
                                Surface(color = if (ayah.number == selectedAyah) Color(0xFFD6E5D7) else MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                                    Column(Modifier.fillMaxWidth().clickable {
                                        selectedAyah = ayah.number
                                        prefs.edit().putInt("quran_surah_number", current.number).putString("quran_surah", current.name).putString("quran_ayah", ayah.number.toString()).apply()
                                        onPositionSelected(current.number, current.name, ayah.number)
                                    }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text("الآية ${ayah.number}", color = Color(0xFF164B3A), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text(ayah.text, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    "النص العثماني: مشروع تنزيل (Tanzil)، عبر AlQuran Cloud. يُحفظ النص كما ورد دون تعديل بعد أول تحميل ناجح.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F665F)
                )
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tanzil.net/docs/Text_License")))
                }) { Text("ترخيص نصّ تنزيل") }
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://alquran.cloud/terms-and-conditions")))
                }) { Text("شروط خدمة AlQuran Cloud") }
            }
        }
    if (verseByVerse && readerOpen) {
        val activeSurah = surahs.firstOrNull { it.number == selectedNumber }
        val activeAyah = activeSurah?.ayahs?.getOrNull(selectedAyah - 1)
        val pageBackground = when (readingTheme) {
            "light" -> Color(0xFFF8F5EC)
            "green" -> Color(0xFF183D32)
            else -> Color.Black
        }
        val pageForeground = if (readingTheme == "light") Color(0xFF171717) else Color.White
        val accent = Color(0xFFE0A12B)
        Dialog(onDismissRequest = {
            prefs.edit().putInt("quran_surah_number", selectedNumber).putString("quran_surah", activeSurah?.name ?: "").putString("quran_ayah", selectedAyah.toString()).apply()
            readerOpen = false
            onSessionFinish?.invoke()
        }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Surface(Modifier.fillMaxSize(), color = pageBackground) {
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("مصحف قيام الليل", color = pageForeground, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Box(
                        Modifier.weight(1f).fillMaxWidth().pointerInput(selectedNumber, selectedAyah, surahs) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onHorizontalDrag = { _, amount -> totalDrag += amount },
                                onDragEnd = {
                                    // A physical right swipe advances to the next ayah in RTL reading.
                                    if (totalDrag > 55f) {
                                        if (activeSurah != null && selectedAyah < activeSurah.ayahs.size) selectedAyah++
                                        else if (selectedNumber < 114) { selectedNumber++; selectedAyah = 1 }
                                    } else if (totalDrag < -55f) {
                                        if (selectedAyah > 1) selectedAyah--
                                        else if (selectedNumber > 1) { selectedNumber--; selectedAyah = surahs.firstOrNull { it.number == selectedNumber }?.ayahs?.size ?: 1 }
                                    }
                                    totalDrag = 0f
                                    val name = surahs.firstOrNull { it.number == selectedNumber }?.name.orEmpty()
                                    readAyahKeys = readAyahKeys + "${selectedNumber}:${selectedAyah}"
                                    prefs.edit().putInt("quran_surah_number", selectedNumber).putString("quran_surah", name).putString("quran_ayah", selectedAyah.toString()).putStringSet("qiyam_active_ayah_keys", readAyahKeys).apply()
                                    onPositionSelected(selectedNumber, name, selectedAyah)
                                },
                                onDragCancel = { totalDrag = 0f }
                            )
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeAyah != null && activeSurah != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 18.dp)
                            ) {
                                Text(activeSurah.name, color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text(activeAyah.text, color = pageForeground, textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineLarge, lineHeight = MaterialTheme.typography.headlineLarge.lineHeight)

                            }
                        }
                    }
                    Button(onClick = {
                        prefs.edit().putInt("quran_surah_number", selectedNumber).putString("quran_surah", activeSurah?.name ?: "").putString("quran_ayah", selectedAyah.toString()).apply()
                        readerOpen = false
                        onSessionFinish?.invoke()
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) {
                        Text("إغلاق المصحف")
                    }
                }
            }
        }
    }

    }
}
