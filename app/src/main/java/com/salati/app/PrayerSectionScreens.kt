package com.salati.app

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val SectionGreen = Color(0xFF164B3A)
private val SectionPale = Color(0xFFE5EBDD)


private val quranAyahCounts = listOf(7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6)
fun quranGlobalAyahIndex(surah: Int, ayah: Int): Int {
    val before = quranAyahCounts.take((surah - 1).coerceIn(0, quranAyahCounts.size)).sum()
    return before + ayah.coerceAtLeast(1)
}

@Composable
fun PrayerTopicLearningScreen(prayer: String, isFard: Boolean, prefs: SharedPreferences) {
    val names = mapOf("الفجر" to "الفجر", "الظهر" to "الظهر", "العصر" to "العصر", "المغرب" to "المغرب", "العشاء" to "العشاء", "الضحى" to "الضحى")
    val title = names[prayer] ?: prayer
    val entries = when (title) {
        "الفجر" -> listOf(
            Triple("فضل صلاة الفجر", "«مَن صلَّى الصبحَ فهو في ذمَّةِ الله»", "صحيح مسلم (657). المعنى: في عهد الله وأمانه."),
            Triple("القراءة في فرض الفجر", "كان النبي ﷺ يقرأ في صلاة الصبح بما بين الستين إلى المائة آية.", "صحيح البخاري (541)، وصحيح مسلم (461)."),
            Triple("الأذكار بعد الفريضة", "«أستغفر الله» ثلاثًا، ثم: «اللهم أنت السلام ومنك السلام تباركت يا ذا الجلال والإكرام».", "صحيح مسلم (591). هذا الذكر ثابت دبر الصلوات المكتوبة عمومًا.")
        )
        "الظهر" -> listOf(
            Triple("فضل المحافظة على الصلاة", "«مَن حافظ على الصلوات الخمس ركوعهن وسجودهن ووضوئهن ومواقيتهن...», وفيه الوعد بالنور والنجاة.", "مسند أحمد؛ يُراجع لفظ الرواية ودرجتها قبل اعتماد النص الكامل."),
            Triple("الإبراد عند شدة الحر", "«إذا اشتد الحر فأبردوا بالصلاة؛ فإن شدة الحر من فيح جهنم».", "صحيح البخاري (536)، وصحيح مسلم (616)."),
            Triple("الأذكار بعد الفريضة", "«أستغفر الله» ثلاثًا، ثم «اللهم أنت السلام ومنك السلام...».", "صحيح مسلم (591). ذكر عام بعد الصلوات المكتوبة.")
        )
        "العصر" -> listOf(
            Triple("فضل صلاة العصر", "«مَن ترك صلاة العصر فقد حبط عمله».", "صحيح البخاري (553)."),
            Triple("المحافظة على الصلاتين", "«مَن صلى البردين دخل الجنة».", "صحيح البخاري (574)، وصحيح مسلم (635). والبردان: الفجر والعصر."),
            Triple("الأذكار بعد الفريضة", "«أستغفر الله» ثلاثًا، ثم «اللهم أنت السلام ومنك السلام...».", "صحيح مسلم (591). ذكر عام بعد الصلوات المكتوبة.")
        )
        "المغرب" -> listOf(
            Triple("تعجيل صلاة المغرب", "«لا تزال أمتي بخير ما عجلوا المغرب».", "سنن أبي داود (418)، وجامع الترمذي (172)؛ حسنه الترمذي."),
            Triple("القراءة في المغرب", "كان النبي ﷺ يقرأ في المغرب بقصار المفصل في الغالب، ووردت قراءات أطول أحيانًا.", "صحيح البخاري (763)؛ وتتنوع القراءة."),
            Triple("الأذكار بعد الفريضة", "«أستغفر الله» ثلاثًا، ثم «اللهم أنت السلام ومنك السلام...».", "صحيح مسلم (591). ذكر عام بعد الصلوات المكتوبة.")
        )
        else -> listOf(
            Triple("فضل صلاة العشاء", "«إن أثقل صلاة على المنافقين صلاة العشاء وصلاة الفجر».", "صحيح البخاري (657)، وصحيح مسلم (651)."),
            Triple("تأخير العشاء", "«لولا أن أشق على أمتي لأمرتهم أن يؤخروا العشاء إلى ثلث الليل أو نصفه».", "جامع الترمذي (167)، وصححه أهل العلم."),
            Triple("الأذكار بعد الفريضة", "«أستغفر الله» ثلاثًا، ثم «اللهم أنت السلام ومنك السلام...».", "صحيح مسلم (591). ذكر عام بعد الصلوات المكتوبة.")
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = SectionPale)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (isFard) "صلاة $title: الفرض وفضائله وأذكاره" else "نافلة $title: فضلها وسننها", color = SectionGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(if (isFard) "تعرف على هدي الصلاة والأذكار الثابتة بعدها. الأذكار العامة لا تختص بهذه الصلاة وحدها." else "هذه نافلة مستحبة وليست فرضًا. خذ منها ما تقدر على المداومة عليه.")
            }
        }
        if (!isFard) {
            val extra = when (title) {
                "الفجر" -> listOf(
                    Triple("فضل ركعتي الفجر", "«ركعتا الفجر خير من الدنيا وما فيها».", "صحيح مسلم (725)."),
                    Triple("القراءة في الركعتين", "كان يقرأ في الأولى: «قُلْ يَا أَيُّهَا الْكَافِرُونَ»، وفي الثانية: «قُلْ هُوَ اللَّهُ أَحَدٌ».", "صحيح مسلم (726)."),
                    Triple("الاضطجاع بعد سنة الفجر", "كان النبي ﷺ إذا صلى ركعتي الفجر اضطجع على شقه الأيمن.", "صحيح البخاري (1160)، وصحيح مسلم (736) بألفاظ."),
                    Triple("أداء السنة في البيت", "«أفضل صلاة المرء في بيته إلا الصلاة المكتوبة».", "صحيح البخاري (731)، وصحيح مسلم (781).")
                )
                "الظهر" -> listOf(Triple("رواتب الظهر", "وردت أربع ركعات قبل الظهر وركعتان بعدها.", "صحيح مسلم (728)، وأبو داود (1250)."))
                "العصر" -> listOf(Triple("التطوع قبل العصر", "«رحم الله امرأً صلى قبل العصر أربعًا».", "سنن أبي داود (1271)، وجامع الترمذي (430)."))
                "المغرب" -> listOf(Triple("سنة المغرب", "«صلوا قبل المغرب... لمن شاء»؛ ثم ثبتت ركعتا السنة بعد المغرب.", "صحيح البخاري (1183)؛ وصحيح البخاري (1180) في راتبة المغرب."))
                "العشاء" -> listOf(Triple("راتبة العشاء", "كان من الرواتب ركعتان بعد العشاء.", "صحيح البخاري (1180)، وصحيح مسلم (729)."))
                else -> listOf(Triple("فضل الضحى", "«ويجزئ من ذلك ركعتان يركعهما من الضحى».", "صحيح مسلم (720)."))
            }
            extra.forEach { LearningEvidenceCard(it.first, it.second, it.third) }
        }
        entries.forEach { LearningEvidenceCard(it.first, it.second, it.third) }
        if (isFard) {
            Text("الأذكار بعد الصلاة", color = SectionGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text("العداد يزيد بلمسة واحدة، ويمكنك التراجع أو تصفيره.")
            PrayerDhikrCounter(prefs, "post_prayer_istighfar", "الاستغفار", "أَسْتَغْفِرُ اللهَ", 3, "ثلاث مرات بعد الصلاة المكتوبة", "صحيح مسلم (591)")
            PrayerDhikrCounter(prefs, "post_prayer_salam", "دعاء السلام", "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ", 1, "مرة واحدة بعد الصلاة المكتوبة", "صحيح مسلم (591)")
            PrayerDhikrCounter(prefs, "post_prayer_subhan", "التسبيح", "سُبْحَانَ اللهِ", 33, "ثلاث وثلاثون مرة", "صحيح مسلم (596)")
            PrayerDhikrCounter(prefs, "post_prayer_hamd", "التحميد", "الْحَمْدُ لِلَّهِ", 33, "ثلاث وثلاثون مرة", "صحيح مسلم (596)")
            PrayerDhikrCounter(prefs, "post_prayer_takbir", "التكبير", "اللهُ أَكْبَرُ", 34, "أربع وثلاثون مرة", "صحيح مسلم (596)")
        } else {
            Text("لا يثبت دعاء مخصوص بعد كل نافلة بعينها. يمكنك الدعاء بما شئت وقراءة الأذكار العامة دون اعتقاد عدد مخصوص لهذه النافلة.")
            PrayerDhikrCounter(prefs, "nafl_general_dhikr", "ذكر عام اختياري", "سُبْحَانَ اللهِ وَبِحَمْدِهِ", 100, "عداد اختياري، وليس عددًا مخصوصًا بهذه النافلة", "ورد فضل المائة في أحاديث صحيحة")
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("التيسير والمداومة", color = SectionGreen, fontWeight = FontWeight.Bold)
                Text("ابدأ بذكر أو ذكرين، أو بما تستطيع. لا تجعل الأذكار عبئًا عليك؛ المهم أن تداوم على ما تقدر عليه.")
                Text("السواك من السنن التي يُذكّر بها عند الوضوء والصلاة، ومن ذلك قول النبي ﷺ: «لولا أن أشق على أمتي لأمرتهم بالسواك عند كل صلاة».", style = MaterialTheme.typography.bodyMedium)
                Text("صحيح البخاري (887)، وصحيح مسلم (252).", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LearningEvidenceCard(title: String, hadith: String, explanation: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = SectionGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("نص الحديث أو الدليل", color = SectionGreen, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
            Text(hadith, style = MaterialTheme.typography.bodyLarge)
            Text(explanation, color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun QiyamSunnahLearningScreen() {
    val items = listOf(
        Triple("السواك عند الاستيقاظ", "«كان النبي ﷺ إذا قام من الليل يشوص فاه بالسواك».", "صحيح البخاري (245)، وصحيح مسلم (255). يستحب تنظيف الفم عند القيام."),
        Triple("افتتاح صلاة الليل بركعتين خفيفتين", "«إذا قام أحدكم من الليل فليفتتح صلاته بركعتين خفيفتين».", "صحيح مسلم (768). يبدأ بهما قبل إطالة الصلاة."),
        Triple("صلاة الليل مثنى مثنى", "«صلاة الليل مثنى مثنى».", "صحيح البخاري (990)، وصحيح مسلم (749)."),
        Triple("ختم الصلاة بالوتر", "«اجعلوا آخر صلاتكم بالليل وترًا».", "صحيح البخاري (998)، وصحيح مسلم (751)."),
        Triple("دعاء القنوت", "«اللهم اهدني فيمن هديت، وعافني فيمن عافيت...»", "سنن أبي داود (1425)، وجامع الترمذي (464).")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = SectionPale)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("سنن قيام الليل", color = SectionGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("خذ من هدي النبي ﷺ ما تستطيع، دون تكلف.")
            }
        }
        items.forEach { LearningEvidenceCard(it.first, it.second, it.third) }
    }
}

@Composable
fun QiyamSessionFinishScreen(prefs: SharedPreferences, onSaved: () -> Unit = {}) {
    val today = LocalDate.now().toString()
    var rakats by remember(today) { mutableStateOf("") }
    var witr by remember(today) { mutableStateOf(false) }
    var reflection by remember(today) { mutableStateOf("") }
    var saved by remember(today) { mutableStateOf(false) }
    val verses = prefs.getInt("qiyam_active_verses", 0).coerceAtLeast(0)
    val fromSurah = prefs.getString("qiyam_active_start_surah_name", "") ?: ""
    val fromAyah = prefs.getInt("qiyam_active_start_ayah", 0)
    val toSurah = prefs.getString("qiyam_active_end_surah_name", "") ?: ""
    val toAyah = prefs.getInt("qiyam_active_end_ayah", 0)
    val reward = when {
        verses >= 1000 -> "بفضل الله، بلغت ألف آية في قيامك؛ ورد في الحديث: «كُتب من المقنطرين»."
        verses >= 100 -> "بفضل الله، بلغت مئة آية في قيامك؛ ورد في الحديث: «كُتب من القانتين»."
        verses >= 10 -> "بفضل الله، بلغت عشر آيات في قيامك؛ ورد في الحديث: «لم يُكتب من الغافلين»."
        else -> "سجّلت $verses آية في هذه الجلسة. ما تيسر من القيام خير، ويمكنك الزيادة أو التوقف بحسب استطاعتك."
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = SectionPale)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("إنهاء جلسة قيام الليل", color = SectionGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("عدد الآيات المسجل في الجلسة: $verses")
                Text("قال رسول الله ﷺ: «مَن قام بعشر آيات لم يُكتب من الغافلين، ومن قام بمئة آية كُتب من القانتين، ومن قام بألف آية كُتب من المقنطرين».")
                Text("سنن أبي داود (1398)، صحيح الألباني.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Text(reward, color = SectionGreen, fontWeight = FontWeight.SemiBold)
                Text("هذا تذكير بما ورد في الحديث بناءً على عدد الآيات الذي سجلته، وليس حكمًا على القبول أو تحقق الأجر لشخص بعينه.")
            }
        }
        if (!saved) {
            OutlinedTextField(value = rakats, onValueChange = { rakats = it.filter(Char::isDigit).take(2) }, label = { Text("عدد الركعات") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = witr, onCheckedChange = { witr = it })
                Text("صليت الوتر")
            }
            OutlinedTextField(value = reflection, onValueChange = { reflection = it }, label = { Text("كيف كان شعورك بعد الجلسة؟ (اختياري)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Button(onClick = {
                val key = "qiyam_sessions_" + today
                val count = prefs.getInt(key, 0) + 1
                val editor = prefs.edit().putInt(key, count)
                    .putInt("qiyam_rakats_" + today, prefs.getInt("qiyam_rakats_" + today, 0) + (rakats.toIntOrNull() ?: 0))
                    .putBoolean("qiyam_witr_" + today, prefs.getBoolean("qiyam_witr_" + today, false) || witr)
                    .putInt("qiyam_verses_" + today, prefs.getInt("qiyam_verses_" + today, 0) + verses)
                val history = prefs.getString("qiyam_session_history", "[]") ?: "[]"
                val array = try { org.json.JSONArray(history) } catch (_: Exception) { org.json.JSONArray() }
                array.put(org.json.JSONObject()
                    .put("date", today).put("verses", verses)
                    .put("surah", if (fromSurah == toSurah) fromSurah else "$fromSurah إلى $toSurah")
                    .put("fromAyah", fromAyah).put("toAyah", toAyah).put("stopAyah", toAyah)
                    .put("rakats", rakats.toIntOrNull() ?: 0).put("witr", witr).put("reflection", reflection.trim()))
                editor.putString("qiyam_session_history", array.toString())
                    .remove("qiyam_active_verses").remove("qiyam_active_start").remove("qiyam_active_end")
                    .remove("qiyam_active_start_surah_name").remove("qiyam_active_start_ayah")
                    .remove("qiyam_active_end_surah_name").remove("qiyam_active_end_ayah").apply()
                saved = true
                onSaved()
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = SectionGreen)) { Text("حفظ الجلسة") }
        } else {
            Text("تم حفظ الجلسة.")
        }
    }
}


@Composable
private fun PrayerDhikrCounter(prefs: SharedPreferences, key: String, title: String, phrase: String, target: Int, note: String, source: String) {
    val dailyKey = key + "_" + title + "_" + LocalDate.now().toString()
    var count by remember(dailyKey) { mutableIntStateOf(prefs.getInt(dailyKey, 0).coerceAtLeast(0)) }
    Card(colors = CardDefaults.cardColors(containerColor = SectionPale)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = SectionGreen, fontWeight = FontWeight.Bold)
            Text(phrase, style = MaterialTheme.typography.titleMedium)
            Text("$count / $target", color = SectionGreen, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { count++; prefs.edit().putInt(dailyKey, count).apply() }, colors = ButtonDefaults.buttonColors(containerColor = SectionGreen)) { Text("+1") }
                OutlinedButton(onClick = { count = (count - 1).coerceAtLeast(0); prefs.edit().putInt(key, count).apply() }) { Text("تراجع") }
                TextButton(onClick = { count = 0; prefs.edit().putInt(dailyKey, 0).apply() }) { Text("تصفير") }
            }
            Text(note, style = MaterialTheme.typography.bodySmall)
            Text(source, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun QiyamHistoryScreen(prefs: SharedPreferences) {
    val history = remember { mutableStateListOf<org.json.JSONObject>().apply {
        val data = try { org.json.JSONArray(prefs.getString("qiyam_session_history", "[]")) } catch (_: Exception) { org.json.JSONArray() }
        for (i in data.length() - 1 downTo 0) data.optJSONObject(i)?.let { add(it) }
    } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("سجل قيام الليل", color = SectionGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        if (history.isEmpty()) Text("لم تسجل جلسات بعد. بعد إنهاء أول جلسة ستظهر تفاصيلها هنا.")
        else history.forEachIndexed { index, entry ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("جلسة ${history.size - index} — ${entry.optString("date")}", color = SectionGreen, fontWeight = FontWeight.Bold)
                    Text("السورة: ${entry.optString("surah", "غير محدد")}")
                    Text("من الآية ${entry.optInt("fromAyah")} إلى ${entry.optInt("toAyah")} — التوقف عند ${entry.optInt("stopAyah")}")
                    Text("إجمالي الآيات: ${entry.optInt("verses")} | الركعات: ${entry.optInt("rakats")} | الوتر: ${if (entry.optBoolean("witr")) "نعم" else "لا"}")
                    if (entry.optString("reflection").isNotBlank()) Text("ملاحظتك: ${entry.optString("reflection")}")
                }
            }
        }
    }
}


@Composable
fun QiyamVirtuesLearningScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = SectionPale)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("فضل قيام الليل", color = SectionGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("قال الله تعالى: ﴿وَمِنَ اللَّيْلِ فَتَهَجَّدْ بِهِ نَافِلَةً لَكَ عَسَىٰ أَنْ يَبْعَثَكَ رَبُّكَ مَقَامًا مَحْمُودًا﴾", style = MaterialTheme.typography.bodyLarge)
                Text("الإسراء: 79", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        }
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("أفضل الصلاة بعد الفريضة", color = SectionGreen, fontWeight = FontWeight.Bold)
                Text("قال رسول الله ﷺ: «أفضل الصلاة بعد الصلاة المكتوبة الصلاة في جوف الليل».")
                Text("صحيح مسلم (1163).", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Text("المعنى باختصار: صلاة الليل من أفضل صلوات التطوع بعد الفرائض.")
            }
        }
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("الاستمرار ولو بالقليل", color = SectionGreen, fontWeight = FontWeight.Bold)
                Text("ابدأ بما تستطيع، ولو بركعتين، واجعل لك وردًا يسيرًا تداوم عليه دون أن تحمّل نفسك ما لا تطيق.")
            }
        }
    }
}
