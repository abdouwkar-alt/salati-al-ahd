package com.salati.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.SharedPreferences
import java.time.LocalDate

private val WorshipGreen = Color(0xFF164B3A)
private val WorshipPale = Color(0xFFE5EBDD)

@Composable
fun QiyamTrackerScreen(prefs: SharedPreferences, refreshToken: Int = 0) {
    val today = LocalDate.now().toString()
    val history = remember(refreshToken, today) {
        try { org.json.JSONArray(prefs.getString("qiyam_session_history", "[]")) }
        catch (_: Exception) { org.json.JSONArray() }
    }
    val sessions = (0 until history.length()).mapNotNull { history.optJSONObject(it) }
    val recent = sessions.take(5)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = WorshipGreen)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("قيام الليل", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("اجعل لك من الليل نصيبًا بما تستطيع، دون تكلف أو مقارنة.", color = Color.White)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("جلساتك المسجلة", color = WorshipGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("عدد الجلسات المسجلة: ${sessions.size}")
                if (recent.isEmpty()) Text("لم تسجل جلسات بعد.")
                recent.forEach { entry ->
                    HorizontalDivider()
                    Text(entry.optString("date"))
                    Text("السورة: ${entry.optString("surah", "غير محدد")}")
                    Text("الركعات: ${entry.optInt("rakats")} | الوتر: ${if (entry.optBoolean("witr")) "نعم" else "لا"}")
                    if (entry.optString("reflection").isNotBlank()) Text(entry.optString("reflection"), color = Color(0xFF756A55), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = WorshipPale)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("فضل قيام الليل بالقرآن", color = WorshipGreen, fontWeight = FontWeight.Bold)
                Text("قال رسول الله ﷺ: «مَنْ قَامَ بِعَشْرِ آيَاتٍ لَمْ يُكْتَبْ مِنَ الْغَافِلِينَ، وَمَنْ قَامَ بِمِائَةِ آيَةٍ كُتِبَ مِنَ الْقَانِتِينَ، وَمَنْ قَامَ بِأَلْفِ آيَةٍ كُتِبَ مِنَ الْمُقَنْطِرِينَ».")
                Text("سنن أبي داود (1398)، وصححه الألباني. يذكر التطبيق هذا الحديث للترغيب، لا لتحويل القراءة إلى سباق أو تقييم.", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AuthenticDhikrScreen(prefs: SharedPreferences) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("أذكار ثابتة بأعدادها الواردة", color = WorshipGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text("لكل ذكر عدّاد مستقل. الأعداد المعروضة مرتبطة بالرواية المذكورة أسفل الذكر.")
        DhikrCounterCard(
            prefs = prefs,
            keyName = "dhikr_istighfar_100",
            title = "الاستغفار",
            phrase = "أستغفر الله",
            target = 100,
            occasion = "مائة مرة في اليوم",
            source = "صحيح مسلم (2702)"
        )
        DhikrCounterCard(
            prefs = prefs,
            keyName = "dhikr_subhan_100",
            title = "التسبيح والتحميد",
            phrase = "سُبْحَانَ اللهِ وَبِحَمْدِهِ",
            target = 100,
            occasion = "مائة مرة في اليوم",
            source = "صحيح البخاري (6405)",
            reward = "ورد في الحديث أن من قال «سبحان الله وبحمده» مائة مرة حُطّت خطاياه وإن كانت مثل زبد البحر. يُعرض الفضل كما ورد، ولا يحكم العداد على القبول."
        )
        Text("أذكار دبر الصلاة المفروضة", color = WorshipGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text("هذه العدادات لجلسة واحدة بعد الصلاة؛ أعد ضبطها بعد كل صلاة.")
        DhikrCounterCard(prefs, "dhikr_subhan_33", "التسبيح", "سُبْحَانَ اللهِ", 33, "ثلاث وثلاثون مرة بعد كل صلاة مكتوبة", "صحيح مسلم (596)")
        DhikrCounterCard(prefs, "dhikr_alhamd_33", "التحميد", "الْحَمْدُ لِلَّهِ", 33, "ثلاث وثلاثون مرة بعد كل صلاة مكتوبة", "صحيح مسلم (596)")
        DhikrCounterCard(prefs, "dhikr_akbar_34", "التكبير", "اللهُ أَكْبَرُ", 34, "أربع وثلاثون مرة بعد كل صلاة مكتوبة", "صحيح مسلم (596)")
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("الذكر المفتوح", color = WorshipGreen, fontWeight = FontWeight.Bold)
                Text("يمكنك ذكر الله بأي صيغة مشروعة دون التقيد بعدد. لا ينسب التطبيق عددًا محددًا إلى ذكر إلا إذا ورد به دليل.")
            }
        }
    }
}

@Composable
private fun DhikrCounterCard(
    prefs: SharedPreferences,
    keyName: String,
    title: String,
    phrase: String,
    target: Int,
    occasion: String,
    source: String,
    reward: String = ""
) {
    var count by remember(keyName) { mutableIntStateOf(prefs.getInt(keyName, 0).coerceAtLeast(0)) }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = WorshipGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(phrase, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(count.toString() + " / " + target, color = WorshipGreen, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { (count.toFloat() / target.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = WorshipGreen
            )
            Text(occasion, style = MaterialTheme.typography.bodySmall)
            Text("الدليل: $source", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    count++
                    prefs.edit().putInt(keyName, count).apply()
                }, colors = ButtonDefaults.buttonColors(containerColor = WorshipGreen)) { Text("+1") }
                OutlinedButton(onClick = {
                    if (count > 0) {
                        count--
                        prefs.edit().putInt(keyName, count).apply()
                    }
                }) { Text("تراجع") }
                TextButton(onClick = {
                    count = 0
                    prefs.edit().putInt(keyName, 0).apply()
                }) { Text("تصفير") }
            }
            if (count >= target) {
                Text("اكتمل العدد المحدد.", color = WorshipGreen, fontWeight = FontWeight.SemiBold)
                if (reward.isNotBlank()) Text(reward, color = WorshipGreen, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private data class DuaEntry(val title: String, val dua: String, val matn: String, val source: String)

@Composable
fun DuaLibraryScreen() {
    var selected by remember { mutableStateOf("أدعية نبوية") }
    var propheticStart by remember { mutableIntStateOf(0) }
    var quranStart by remember { mutableIntStateOf(0) }
    val prophetic = listOf(
        DuaEntry(
            "دعاء للاستعانة على العبادة",
            "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
            "عن معاذ بن جبل رضي الله عنه أن رسول الله ﷺ أخذ بيده وقال: «يا معاذ، والله إني لأحبك»، فقال: «أوصيك يا معاذ، لا تدعن في دبر كل صلاة أن تقول: اللهم أعني على ذكرك وشكرك وحسن عبادتك».",
            "سنن أبي داود (1522)، وصححه الألباني."
        ),
        DuaEntry(
            "دعاء طلب الهداية والعفاف",
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى وَالتُّقَى وَالْعَفَافَ وَالْغِنَى",
            "عن عبد الله بن مسعود رضي الله عنه أن النبي ﷺ كان يقول: «اللهم إني أسألك الهدى والتقى والعفاف والغنى».",
            "صحيح مسلم (2721)."
        ),
        DuaEntry(
            "دعاء جامع بالمغفرة والرحمة والهداية والعافية والرزق",
            "اللَّهُمَّ اغْفِرْ لِي، وَارْحَمْنِي، وَاهْدِنِي، وَعَافِنِي، وَارْزُقْنِي",
            "عن طارق بن أشيم رضي الله عنه: أن الرجل إذا أسلم علّمه النبي ﷺ الصلاة، ثم أمره أن يدعو بهذه الكلمات: «اللهم اغفر لي، وارحمني، واهدني، وعافني، وارزقني».",
            "صحيح مسلم (2697) — حديث صحيح."
        ),
        DuaEntry(
            "دعاء سؤال العفو والعافية",
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ",
            "عن عبد الله بن عمر رضي الله عنهما: أن رسول الله ﷺ لم يكن يدع هؤلاء الدعوات حين يمسي وحين يصبح: «اللهم إني أسألك العافية في الدنيا والآخرة، اللهم إني أسألك العفو والعافية في ديني ودنياي وأهلي ومالي، اللهم استر عوراتي وآمن روعاتي، اللهم احفظني من بين يدي ومن خلفي وعن يميني وعن شمالي ومن فوقي، وأعوذ بعظمتك أن أغتال من تحتي».",
            "سنن أبي داود (5074)، وصححه الألباني."
        ),
        DuaEntry(
            "دعاء الثبات على الدين",
            "يَا مُقَلِّبَ الْقُلُوبِ، ثَبِّتْ قَلْبِي عَلَى دِينِكَ",
            "عن أنس رضي الله عنه قال: كان رسول الله ﷺ يكثر أن يقول: «يا مقلب القلوب ثبت قلبي على دينك».",
            "جامع الترمذي (2140)، حديث حسن؛ وصححه الألباني."
        ),
        DuaEntry(
            "دعاء الاستعاذة من الهم والحزن والدين",
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْجُبْنِ وَالْبُخْلِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ",
            "عن أنس رضي الله عنه قال: كان النبي ﷺ يقول: «اللهم إني أعوذ بك من الهم والحزن، والعجز والكسل، والجبن والبخل، وضلع الدين وغلبة الرجال».",
            "صحيح البخاري (6369)، وله رواية في صحيح مسلم (2706)."
        ),
        DuaEntry(
            "دعاء الاعتراف بالتقصير وطلب المغفرة",
            "اللَّهُمَّ إِنِّي ظَلَمْتُ نَفْسِي ظُلْمًا كَثِيرًا، وَلَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ، فَاغْفِرْ لِي مَغْفِرَةً مِنْ عِنْدِكَ، وَارْحَمْنِي، إِنَّكَ أَنْتَ الْغَفُورُ الرَّحِيمُ",
            "عن أبي بكر الصديق رضي الله عنه أنه سأل النبي ﷺ دعاءً يدعو به في صلاته، فعلّمه هذا الدعاء.",
            "صحيح البخاري (834)."
        ),
        DuaEntry(
            "دعاء تزكية النفس والاستعاذة من العلم الذي لا ينفع",
            "اللَّهُمَّ آتِ نَفْسِي تَقْوَاهَا، وَزَكِّهَا أَنْتَ خَيْرُ مَنْ زَكَّاهَا، أَنْتَ وَلِيُّهَا وَمَوْلَاهَا",
            "ومن تمام الحديث: «اللهم إني أعوذ بك من علم لا ينفع، ومن قلب لا يخشع، ومن نفس لا تشبع، ومن دعوة لا يستجاب لها».",
            "صحيح مسلم (2722)."
        ),
        DuaEntry(
            "دعاء دوام النعمة والعافية",
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ زَوَالِ نِعْمَتِكَ، وَتَحَوُّلِ عَافِيَتِكَ، وَفُجَاءَةِ نِقْمَتِكَ، وَجَمِيعِ سَخَطِكَ",
            "كان هذا من دعاء رسول الله ﷺ.",
            "صحيح مسلم (2739)."
        ),
        DuaEntry(
            "دعاء ليلة القدر بالعفو",
            "اللَّهُمَّ إِنَّكَ عَفُوٌّ كَرِيمٌ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي",
            "علّم النبي ﷺ عائشة رضي الله عنها أن تقوله إذا وافقت ليلة القدر.",
            "جامع الترمذي (3513)، وقال: حسن صحيح."
        ),
        DuaEntry(
            "دعاء الهداية والسداد",
            "اللَّهُمَّ اهْدِنِي وَسَدِّدْنِي",
            "قاله النبي ﷺ لعلي رضي الله عنه، وأرشده أن يستحضر معنى الهداية إلى الطريق ومعنى السداد كسداد السهم.",
            "صحيح مسلم (2725)."
        ),
        DuaEntry(
            "دعاء النور عند الخروج إلى الصلاة",
            "اللَّهُمَّ اجْعَلْ فِي قَلْبِي نُورًا، وَفِي لِسَانِي نُورًا، وَاجْعَلْ فِي سَمْعِي نُورًا، وَاجْعَلْ فِي بَصَرِي نُورًا، وَاجْعَلْ مِنْ خَلْفِي نُورًا، وَمِنْ أَمَامِي نُورًا، وَاجْعَلْ مِنْ فَوْقِي نُورًا، وَمِنْ تَحْتِي نُورًا، اللَّهُمَّ أَعْطِنِي نُورًا",
            "ورد في حديث ابن عباس في سياق قيام النبي ﷺ من الليل وخروجه إلى الصلاة.",
            "صحيح مسلم (763l)."
        ),
    )
    val quranic = listOf(
        DuaEntry("دعاء خيري الدنيا والآخرة", "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "", "سورة البقرة، الآية 201."),
        DuaEntry("دعاء الثبات", "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِنْ لَدُنْكَ رَحْمَةً إِنَّكَ أَنْتَ الْوَهَّابُ", "", "سورة آل عمران، الآية 8."),
        DuaEntry("دعاء الاعتراف بالظلم وطلب المغفرة", "رَبَّنَا ظَلَمْنَا أَنْفُسَنَا وَإِنْ لَمْ تَغْفِرْ لَنَا وَتَرْحَمْنَا لَنَكُونَنَّ مِنَ الْخَاسِرِينَ", "", "سورة الأعراف، الآية 23."),
        DuaEntry("دعاء طلب الصبر والثبات", "رَبَّنَا أَفْرِغْ عَلَيْنَا صَبْرًا وَثَبِّتْ أَقْدَامَنَا وَانْصُرْنَا عَلَى الْقَوْمِ الْكَافِرِينَ", "", "سورة البقرة، الآية 250."),
        DuaEntry("دعاء صلاح الأهل والذرية", "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا", "", "سورة الفرقان، الآية 74."),
        DuaEntry("دعاء تيسير الأمر وفصاحة اللسان", "رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي وَاحْلُلْ عُقْدَةً مِنْ لِسَانِي يَفْقَهُوا قَوْلِي", "", "سورة طه، الآيات 25–28."),
        DuaEntry("دعاء طلب الخير والرزق", "رَبِّ إِنِّي لِمَا أَنْزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ", "", "سورة القصص، الآية 24."),
        DuaEntry("دعاء طلب الرحمة والرشد", "رَبَّنَا آتِنَا مِنْ لَدُنْكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا", "", "سورة الكهف، الآية 10."),
        DuaEntry("دعاء إقامة الصلاة وقبول الدعاء", "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِنْ ذُرِّيَّتِي رَبَّنَا وَتَقَبَّلْ دُعَاءِ", "", "سورة إبراهيم، الآية 40."),
        DuaEntry("دعاء ذي النون", "لَا إِلَهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ", "", "سورة الأنبياء، الآية 87."),
        DuaEntry("دعاء طلب زيادة العلم", "رَبِّ زِدْنِي عِلْمًا", "", "سورة طه، الآية 114."),
        DuaEntry("دعاء الاعتذار وطلب المغفرة", "رَبِّ إِنِّي أَعُوذُ بِكَ أَنْ أَسْأَلَكَ مَا لَيْسَ لِي بِهِ عِلْمٌ وَإِلَّا تَغْفِرْ لِي وَتَرْحَمْنِي أَكُنْ مِنَ الْخَاسِرِينَ", "", "سورة هود، الآية 47."),
        DuaEntry("دعاء للمؤمنين بالمغفرة", "رَبَّنَا وَسِعْتَ كُلَّ شَيْءٍ رَحْمَةً وَعِلْمًا فَاغْفِرْ لِلَّذِينَ تَابُوا وَاتَّبَعُوا سَبِيلَكَ وَقِهِمْ عَذَابَ الْجَحِيمِ", "", "سورة غافر، الآية 7."),
        DuaEntry("دعاء العفو والرحمة والنصر", "رَبَّنَا لَا تُؤَاخِذْنَا إِنْ نَسِينَا أَوْ أَخْطَأْنَا رَبَّنَا وَلَا تَحْمِلْ عَلَيْنَا إِصْرًا كَمَا حَمَلْتَهُ عَلَى الَّذِينَ مِنْ قَبْلِنَا رَبَّنَا وَلَا تُحَمِّلْنَا مَا لَا طَاقَةَ لَنَا بِهِ وَاعْفُ عَنَّا وَاغْفِرْ لَنَا وَارْحَمْنَا أَنْتَ مَوْلَانَا فَانْصُرْنَا عَلَى الْقَوْمِ الْكَافِرِينَ", "", "سورة البقرة، الآية 286."),
        DuaEntry("دعاء الصبر والثبات", "رَبَّنَا أَفْرِغْ عَلَيْنَا صَبْرًا وَتَوَفَّنَا مُسْلِمِينَ", "", "سورة الأعراف، الآية 126.")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selected == "أدعية نبوية", onClick = { selected = "أدعية نبوية" }, label = { Text("أدعية نبوية") })
            FilterChip(selected = selected == "أدعية القرآن", onClick = { selected = "أدعية القرآن" }, label = { Text("أدعية القرآن") })
        }
        val entries = if (selected == "أدعية نبوية") prophetic else quranic
        val startIndex = if (selected == "أدعية نبوية") propheticStart else quranStart
        val visibleEntries = if (startIndex + 1 < entries.size) {
            entries.drop(startIndex).take(2)
        } else {
            listOf(entries[startIndex], entries.first())
        }
        visibleEntries.forEach { entry ->
            DuaCard(entry.title, entry.dua, entry.source, entry.matn)
        }
        OutlinedButton(
            onClick = {
                if (selected == "أدعية نبوية") propheticStart = (propheticStart + 2) % prophetic.size
                else quranStart = (quranStart + 2) % quranic.size
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("عرض دعاءين آخرين") }
        Card(colors = CardDefaults.cardColors(containerColor = WorshipPale)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("التعلّم على مهل", color = WorshipGreen, fontWeight = FontWeight.Bold)
                Text("يمكنك أن تكتفي بتعلّم دعاء واحد هذا الأسبوع؛ القليل الذي تداوم عليه خير من الكثير الذي تنقطع عنه.")
                Text("قال رسول الله ﷺ: «أحب الأعمال إلى الله أدومها وإن قل».", style = MaterialTheme.typography.bodySmall)
                Text("صحيح مسلم (783)، وصحيح البخاري (6464) بلفظ أطول.", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DuaCard(title: String, dua: String, source: String, matn: String = "") {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = WorshipGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(dua, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (matn.isNotBlank()) {
                Text("نص الحديث", color = WorshipGreen, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                Text(matn, style = MaterialTheme.typography.bodyMedium)
            }
            Text(source, color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
        }
    }
}
