package com.salati.app

import android.util.Xml
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val AwarenessGreen = Color(0xFF164B3A)
private val AwarenessPale = Color(0xFFE5EBDD)
private val AwarenessCream = Color(0xFFF6F2E9)

private data class AwarenessPath(val id: String, val title: String, val description: String, val modules: List<String>, val videoQuery: String)

private val awarenessPaths = listOf(
    AwarenessPath("iman", "الإيمان وشُعَب الإيمان", "فهم الإيمان وأعمال القلب والجوارح، وربط الاعتقاد بالسلوك اليومي.", listOf("معنى الإيمان وأركانه", "حديث شُعَب الإيمان", "التوحيد والإخلاص", "الحياء والأمانة والصدق", "إماطة الأذى وكف الأذى عن الناس"), "شرح حديث الإيمان بضع وسبعون شعبة"),
    AwarenessPath("akhlaq", "الأخلاق النبوية والقرآنية", "الأخلاق ليست شعارات؛ نتعلم أصلها ودليلها وممارستها في البيت والعمل والشارع.", listOf("الصدق والأمانة", "الرحمة والرفق", "العدل والإنصاف", "الصبر وكظم الغيظ", "حفظ اللسان والحقوق", "الحياء والتواضع"), "الأخلاق النبوية دروس موثقة"),
    AwarenessPath("seerah", "السيرة النبوية", "رحلة زمنية لفهم سياق الدعوة، والابتلاء، والهجرة، وبناء المجتمع.", listOf("مكة وبداية الوحي", "الدعوة والصبر", "الهجرة إلى الحبشة والمدينة", "بدر وأحد والخندق", "الحديبية وفتح مكة", "حجة الوداع"), "السيرة النبوية دروس موثقة ترتيب الأحداث"),
    AwarenessPath("companions", "الأنبياء والصحابة", "نتعلم من مواقفهم مع تمييز ما ثبت في الوحي وما يروى في كتب التاريخ.", listOf("قصص الأنبياء في القرآن", "أبو بكر والصحبة", "عمر والعدل", "عثمان والحياء والإنفاق", "علي والعلم والشجاعة", "نساء حول النبوة"), "سير الصحابة دروس علمية موثقة"),
    AwarenessPath("prophets", "الأنبياء والرسل في القرآن", "قصص الوحي بوصفها هداية ومواقف عملية، لا مجرد سرد تاريخي.", listOf("آدم والتوبة", "نوح والثبات", "إبراهيم والتوحيد", "يوسف والعفة والصبر", "موسى ومواجهة الطغيان", "عيسى ومريم عليهما السلام"), "قصص الأنبياء تدبر القرآن دروس"),
    AwarenessPath("modern", "فقه الواقع وأسئلة العصر", "كيف نفكر في الشائعات، والخوارزميات، والاستهلاك، والعلاقات، والاختلاف، والذكاء الاصطناعي؟", listOf("التثبت من الأخبار", "الخصوصية والكرامة الرقمية", "إدارة الانتباه والتشتت", "الاختلاف والحوار", "المال والاستهلاك", "العمل والمعنى والمسؤولية"), "فقه الواقع الشباب الإعلام الرقمي التفكير النقدي"),
    AwarenessPath("gaza", "غزة: الخبر والإنسان", "متابعة ما يحدث في غزة من مصادر واضحة، مع الفصل بين الخبر والتقرير الإنساني والتحليل.", listOf("آخر الأخبار", "الوضع الإنساني والإغاثي", "التعليم والصحة والنزوح", "التحقق من الصور والمقاطع", "طرق المساعدة عبر جهات موثوقة"), "غزة تقارير إنسانية وتحليل موثق")
)

private const val GAZA_DIGEST_WORK = "daily_gaza_news_digest"
private const val GAZA_DIGEST_CHANNEL = "gaza_news_digest"

private fun setGazaDigest(context: android.content.Context, enabled: Boolean) {
    val manager = WorkManager.getInstance(context)
    if (!enabled) {
        manager.cancelUniqueWork(GAZA_DIGEST_WORK)
        return
    }
    val request = PeriodicWorkRequestBuilder<GazaDigestWorker>(24, java.util.concurrent.TimeUnit.HOURS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    manager.enqueueUniquePeriodicWork(GAZA_DIGEST_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
}

class GazaDigestWorker(context: android.content.Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("salati", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("gaza_digest_enabled", false)) return Result.success()
        return try {
            val query = URLEncoder.encode("غزة OR قطاع غزة", "UTF-8")
            val connection = (URL("https://news.google.com/rss/search?q=$query&hl=ar&gl=EG&ceid=EG:ar").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 12_000
                setRequestProperty("User-Agent", "SalatiJamea-Android/1.0")
            }
            val titles = try {
                connection.inputStream.use { stream ->
                    val parser = Xml.newPullParser().apply {
                        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                        setInput(stream, "UTF-8")
                    }
                    val found = mutableListOf<String>()
                    var event = parser.eventType
                    var inItem = false
                    var inTitle = false
                    var title = ""
                    while (event != XmlPullParser.END_DOCUMENT && found.size < 3) {
                        when (event) {
                            XmlPullParser.START_TAG -> {
                                if (parser.name == "item") { inItem = true; title = "" }
                                if (inItem && parser.name == "title") inTitle = true
                            }
                            XmlPullParser.TEXT -> if (inItem && inTitle) title += parser.text
                            XmlPullParser.END_TAG -> {
                                if (parser.name == "title") inTitle = false
                                if (parser.name == "item" && inItem) {
                                    if (title.isNotBlank()) found.add(title.trim())
                                    inItem = false
                                }
                            }
                        }
                        event = parser.next()
                    }
                    found
                }
            } finally { connection.disconnect() }
            if (titles.isNotEmpty()) {
                val manager = applicationContext.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    manager.createNotificationChannel(
                        android.app.NotificationChannel(GAZA_DIGEST_CHANNEL, "موجز أخبار غزة", android.app.NotificationManager.IMPORTANCE_DEFAULT)
                    )
                }
                if (android.os.Build.VERSION.SDK_INT < 33 ||
                    androidx.core.content.ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    val intent = android.content.Intent(applicationContext, MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("open_awareness", true)
                    }
                    val pending = android.app.PendingIntent.getActivity(
                        applicationContext, 7712, intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    val builder = if (android.os.Build.VERSION.SDK_INT >= 26)
                        android.app.Notification.Builder(applicationContext, GAZA_DIGEST_CHANNEL)
                    else android.app.Notification.Builder(applicationContext)
                    manager.notify(7712, builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("موجز غزة")
                        .setContentText(titles.first())
                        .setStyle(android.app.Notification.BigTextStyle().bigText(titles.joinToString("\\n")))
                        .setContentIntent(pending)
                        .setAutoCancel(true)
                        .build())
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

private data class GazaNewsItem(val title: String, val link: String, val source: String, val published: String)

private suspend fun fetchGazaNews(): List<GazaNewsItem> = withContext(Dispatchers.IO) {
    val query = URLEncoder.encode("غزة OR قطاع غزة", "UTF-8")
    val connection = (URL("https://news.google.com/rss/search?q=$query&hl=ar&gl=EG&ceid=EG:ar").openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 12_000
        requestMethod = "GET"
        setRequestProperty("User-Agent", "SalatiJamea-Android/1.0")
    }
    try {
        connection.inputStream.use { stream ->
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(stream, "UTF-8")
            }
            val items = mutableListOf<GazaNewsItem>()
            var event = parser.eventType
            var inItem = false
            var tag = ""
            var title = ""
            var link = ""
            var source = ""
            var published = ""
            while (event != XmlPullParser.END_DOCUMENT && items.size < 20) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        tag = parser.name
                        if (tag == "item") { inItem = true; title = ""; link = ""; source = ""; published = "" }
                    }
                    XmlPullParser.TEXT -> if (inItem) when (tag) {
                        "title" -> title += parser.text
                        "link" -> link += parser.text
                        "source" -> source += parser.text
                        "pubDate" -> published += parser.text
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "item" && inItem) {
                        val cleanTitle = title.replace(Regex("<[^>]*>"), "").trim()
                        if (cleanTitle.isNotBlank() && link.trim().startsWith("https://")) {
                            items.add(GazaNewsItem(cleanTitle, link.trim(), source.trim().ifBlank { "المصدر داخل الرابط" }, published.trim()))
                        }
                        inItem = false
                    }
                }
                event = parser.next()
            }
            items
        }
    } finally { connection.disconnect() }
}

@Composable
fun AwarenessHubScreen(prefs: android.content.SharedPreferences) {
    val uriHandler = LocalUriHandler.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedPathId by remember { mutableStateOf(prefs.getString("awareness_path_id", "iman") ?: "iman") }
    var expandedModule by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf(prefs.getStringSet("awareness_interests", setOf("iman", "akhlaq", "seerah"))?.toSet().orEmpty()) }
    var news by remember { mutableStateOf<List<GazaNewsItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var refreshToken by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf("") }
    var digestEnabled by remember { mutableStateOf(prefs.getBoolean("gaza_digest_enabled", false)) }
    val path = awarenessPaths.firstOrNull { it.id == selectedPathId } ?: awarenessPaths.first()

    LaunchedEffect(refreshToken) {
        loading = true
        try { news = fetchGazaNews(); error = if (news.isEmpty()) "لا توجد نتائج متاحة الآن." else "" }
        catch (_: Exception) { error = "تعذر تحديث الموجز. تحقق من الاتصال وحاول مرة أخرى." }
        finally { loading = false }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = AwarenessGreen)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("وَعْي", color = Color(0xFFE8D7A9), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("افهم دينك، وافهم واقعك، وابنِ خُلُقك", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("مسار تعلم وتزكية وفهم للواقع، يبدأ من الوحي والسيرة، ويصل إلى اختياراتك ومواقفك اليوم.", color = Color.White.copy(alpha = .92f))
                Text("لا سباق ولا درجات إيمان؛ خطوة فهم وعمل في كل مرة.", color = Color(0xFFE8D7A9), style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = AwarenessCream)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("ما الموضوعات التي تريد أن تتعلمها؟", color = AwarenessGreen, fontWeight = FontWeight.Bold)
                Text("اختر ما يهمك، وستظهر لك مساراته أولًا. يمكنك تعديل الاختيارات لاحقًا.", style = MaterialTheme.typography.bodySmall)
                awarenessPaths.filter { it.id != "gaza" }.forEach { option ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = option.id in interests, onCheckedChange = { checked ->
                            interests = if (checked) interests + option.id else interests - option.id
                            prefs.edit().putStringSet("awareness_interests", interests).apply()
                        })
                        Text(option.title)
                    }
                }
            }
        }

        Text("مسارات الوعي", color = AwarenessGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            awarenessPaths.forEach { option ->
                FilterChip(selected = selectedPathId == option.id, onClick = {
                    selectedPathId = option.id
                    prefs.edit().putString("awareness_path_id", option.id).apply()
                    expandedModule = ""
                }, label = { Text(option.title) })
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(path.title, color = AwarenessGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(path.description)
                path.modules.forEachIndexed { index, module ->
                    Card(colors = CardDefaults.cardColors(containerColor = AwarenessCream), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text("${index + 1}. $module", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { expandedModule = if (expandedModule == module) "" else module }) {
                                    Text(if (expandedModule == module) "إخفاء" else "افتح")
                                }
                            }
                            if (expandedModule == module) {
                                Text("هذا عنوان تعلّم. قبل اعتماد درس تفصيلي نثبت نصوصه ومصادره، ونفصل بين القرآن والحديث الصحيح والأثر والتاريخ والتحليل المعاصر.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B6B62))
                                TextButton(onClick = {
                                    val query = URLEncoder.encode("$module ${path.title} درس موثق", "UTF-8")
                                    runCatching { uriHandler.openUri("https://www.youtube.com/results?search_query=$query") }
                                }) { Text("ابحث عن فيديوهات لهذا الموضوع") }
                            }
                        }
                    }
                }
                OutlinedButton(onClick = {
                    val query = URLEncoder.encode(path.videoQuery, "UTF-8")
                    runCatching { uriHandler.openUri("https://www.youtube.com/results?search_query=$query") }
                }) { Text("استكشف فيديوهات المسار") }
                Text("نتائج البحث اقتراحات للاكتشاف وليست تزكية تلقائية لكل قناة أو فيديو. راجع المتحدث والمصدر والمحتوى قبل الاعتماد.", color = Color(0xFF765B38), style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = AwarenessPale)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("شُعَب الإيمان: من الاعتقاد إلى السلوك", color = AwarenessGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("قال رسول الله ﷺ: «الإيمان بضع وسبعون — أو بضع وستون — شعبة، فأفضلها قول لا إله إلا الله، وأدناها إماطة الأذى عن الطريق، والحياء شعبة من الإيمان».")
                Text("صحيح مسلم (35). الحديث يربط بين أصل التوحيد، والعمل الظاهر، وخلق القلب. لا نحصر شعب الإيمان في قائمة مخترعة؛ ندرسها من النصوص وشروح أهل العلم.")
                TextButton(onClick = { runCatching { uriHandler.openUri("https://sunnah.com/muslim:35") } }) { Text("افتح الحديث في صحيح مسلم") }
                Text("محاور للتعلم: التوحيد، الصلاة، الصدق، الأمانة، الحياء، الرحمة، العدل، كف الأذى، إماطة الأذى، حفظ الحقوق.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("غزة | موجز الأخبار", color = AwarenessGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("روابط منشورة من مصادر متعددة يجمعها موجز بحث إخباري. هذا ليس تحققًا مستقلًا من كل خبر؛ افتح المصدر الأصلي وانتبه لتاريخ النشر والتحديث.")
                Button(onClick = { refreshToken++; error = "" }, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = AwarenessGreen)) {
                    Text(if (loading) "جارٍ التحديث…" else "تحديث الأخبار")
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = digestEnabled, onCheckedChange = { enabled ->
                        digestEnabled = enabled
                        prefs.edit().putBoolean("gaza_digest_enabled", enabled).apply()
                        setGazaDigest(context = context, enabled = enabled)
                    })
                    Column(Modifier.weight(1f)) {
                        Text("أرسل لي موجز غزة مرة يوميًا")
                        Text("اختياري، عبر إشعار محلي تقريبي عند توفر الإنترنت.", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (error.isNotBlank()) Text(error, color = Color(0xFF8A3C33))
                news.forEach { item ->
                    HorizontalDivider(color = AwarenessPale)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        Text(listOf(item.source, item.published).filter { it.isNotBlank() }.joinToString(" • "), color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { runCatching { uriHandler.openUri(item.link) } }) { Text("اقرأ الخبر من المصدر") }
                    }
                }
                HorizontalDivider(color = AwarenessPale)
                Text("مصادر إنسانية مرجعية", color = AwarenessGreen, fontWeight = FontWeight.Bold)
                TextButton(onClick = { runCatching { uriHandler.openUri("https://www.ochaopt.org/content/humanitarian-situation-report-18-november-2026") } }) { Text("OCHA — التقرير الإنساني 18 نوفمبر 2026") }
                TextButton(onClick = { runCatching { uriHandler.openUri("https://www.unops.org/news-and-stories/news/crisis-response-in-gaza") } }) { Text("UNOPS — الاستجابة الإنسانية في غزة") }
                Text("لا نعرض أعداد الضحايا أو الادعاءات كحقائق نهائية دون نسبة واضحة للمصدر وتاريخ التقرير، ولا نعيد نشر الصور الصادمة تلقائيًا.", color = Color(0xFF765B38), style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("منهج الوعي: نص شرعي بمصدره، ثم شرح مميز بوضوح، ثم سؤال أو تطبيق واقعي. الأخبار لها مصادر وتواريخ، والفيديوهات روابط اكتشاف وليست تزكية.", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
    }
}
