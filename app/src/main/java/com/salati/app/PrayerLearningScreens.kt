package com.salati.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val LearningGreen = Color(0xFF164B3A)
private val LearningPale = Color(0xFFE5EBDD)

private data class PrayerPage(val title: String, val subtitle: String, val entries: List<LearningEntry>)

private data class LearningEntry(
    val title: String,
    val label: String,
    val detail: String,
    val source: String
)

@Composable
private fun LearningCard(title: String, subtitle: String, entries: List<LearningEntry>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(title, color = LearningGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFF656B62), style = MaterialTheme.typography.bodyMedium)
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = Color(0xFFECE8DE))
                Text(entry.title, fontWeight = FontWeight.SemiBold, color = LearningGreen)
                Text(entry.label, color = Color(0xFF687568), style = MaterialTheme.typography.labelMedium)
                Text(entry.detail)
                if (entry.source.isNotBlank()) Text("الدليل: " + entry.source, color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun PrayerVirtuesLearningScreen() {
    var selected by remember { mutableStateOf("الفجر") }
    val prayers = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء", "الضحى", "قيام الليل")
    val content = mapOf(
        "الفجر" to PrayerPage(
            "فضل صلاة الفجر",
            "صلاة مفروضة، ولها فضل عظيم، ولا تُخلط بسنة الفجر القبلية.",
            listOf(
                LearningEntry("الصلاة في ذمة الله", "فضل ثابت", "من صلى الصبح فهو في ذمة الله.", "صحيح مسلم (657)"),
                LearningEntry("ركعتا سنة الفجر", "نافلة قبل الفريضة وليستا جزءًا من الفرض", "كان النبي ﷺ يحافظ على ركعتي الفجر، وقال إنهما خير من الدنيا وما فيها.", "صحيح مسلم (725)"),
                LearningEntry("القراءة في فريضة الفجر", "هدي نبوي", "كان يطيل القراءة في صلاة الفجر أكثر من كثير من الصلوات، ويُقرأ بما تيسر دون إلزام بسورة بعينها.", "صحيح مسلم (458)، وأحاديث صفة صلاة النبي ﷺ")
            )
        ),
        "الظهر" to PrayerPage(
            "فضل صلاة الظهر وهديها",
            "الصلاة المفروضة في وسط النهار، وتُراعى فيها أحوال الناس والحر.",
            listOf(
                LearningEntry("الإبراد عند شدة الحر", "سنة عند شدة الحر", "إذا اشتد الحر شُرع تأخير الظهر حتى يخف الحر، كما ورد في الحديث.", "صحيح البخاري (536)، صحيح مسلم (616)"),
                LearningEntry("القراءة في الظهر", "هدي ثابت", "كان مقدار القراءة في الظهر أطول من العصر، وقد يُسمعهم النبي ﷺ أحيانًا بعض الآيات.", "صحيح البخاري (759)، صحيح مسلم (451)"),
                LearningEntry("الرواتب حول الظهر", "نافلة مستقلة عن الفريضة", "وردت أربع ركعات قبل الظهر وركعتان بعدها ضمن السنن الرواتب.", "صحيح مسلم (728)، وسنن أبي داود (1250)")
            )
        ),
        "العصر" to PrayerPage(
            "فضل صلاة العصر",
            "من الصلوات الخمس، والمحافظة عليها من أعظم أبواب المحافظة على الصلاة.",
            listOf(
                LearningEntry("المحافظة على العصر", "تنبيه نبوي شديد", "ورد الوعيد فيمن يترك صلاة العصر.", "صحيح البخاري (553)"),
                LearningEntry("الصلاة الوسطى", "فضل خاص", "جاء في التفسير النبوي تعيين الصلاة الوسطى بأنها صلاة العصر.", "صحيح البخاري (6396)، صحيح مسلم (629)"),
                LearningEntry("أربع ركعات قبل العصر", "نافلة مستحبة وليست من الرواتب المؤكدة", "ورد الدعاء بالرحمة لمن صلى قبل العصر أربعًا؛ وهي نافلة مستحبة.", "سنن أبي داود (1271)، جامع الترمذي (430)")
            )
        ),
        "المغرب" to PrayerPage(
            "فضل صلاة المغرب وهديها",
            "صلاة مفروضة يدخل وقتها بغروب الشمس.",
            listOf(
                LearningEntry("تعجيل المغرب", "سنة في وقتها", "لا تزال الأمة بخير ما عجلوا المغرب.", "سنن أبي داود (418)، جامع الترمذي (172)"),
                LearningEntry("القراءة في المغرب", "هدي متنوع", "ثبتت قراءات مختلفة في المغرب؛ فلا يلزم المصلي سورة واحدة في كل مرة.", "صحيح البخاري (763)، صحيح مسلم (462)"),
                LearningEntry("ركعتا ما بعد المغرب", "نافلة راتبة", "ثبتت ركعتا سنة المغرب بعد الفريضة.", "صحيح البخاري (1180)، صحيح مسلم (729)")
            )
        ),
        "العشاء" to PrayerPage(
            "فضل صلاة العشاء",
            "صلاة مفروضة، ويُستحب عدم تأخيرها بما يشق على الناس.",
            listOf(
                LearningEntry("ثقلها على المنافقين", "فضل المحافظة عليها", "بيّن النبي ﷺ عظم شأن العشاء والفجر وأنهما أثقل الصلوات على المنافقين.", "صحيح البخاري (657)، صحيح مسلم (651)"),
                LearningEntry("تأخير العشاء أحيانًا", "سنة مع مراعاة الجماعة والمشقة", "كان النبي ﷺ يستحب تأخير العشاء لولا المشقة على أمته.", "صحيح البخاري (566)، صحيح مسلم (638)"),
                LearningEntry("ركعتا ما بعد العشاء", "نافلة راتبة", "ثبتت ركعتا سنة العشاء بعد الفريضة.", "صحيح البخاري (1180)، صحيح مسلم (729)")
            )
        ),
        "الضحى" to PrayerPage(
            "صلاة الضحى",
            "نافلة وليست واحدة من الصلوات الخمس، ولا يرتبط وقتها بأذان مفروض.",
            listOf(
                LearningEntry("وقتها", "من بعد ارتفاع الشمس إلى قبيل الزوال", "يبدأ وقتها بعد ارتفاع الشمس، وينتهي قبل دخول وقت الظهر.", "شرح أحاديث صلاة الضحى في صحيح مسلم"),
                LearningEntry("عدد ركعاتها", "أقلها ركعتان", "ثبتت ركعتا الضحى، وجاءت أحاديث في الزيادة؛ صلِّ ما تيسر دون اعتقاد عدد لازم.", "صحيح مسلم (720)، صحيح البخاري (1178)"),
                LearningEntry("وصية أبي هريرة", "المداومة على النافلة", "أوصى النبي ﷺ أبا هريرة بثلاث، منها ركعتي الضحى.", "صحيح البخاري (1981)، صحيح مسلم (721)")
            )
        ),
        "قيام الليل" to PrayerPage(
            "قيام الليل",
            "نافلة من صلاة التطوع، ووقتها من بعد العشاء إلى طلوع الفجر، وأفضله الثلث الأخير.",
            listOf(
                LearningEntry("أفضل صلاة التطوع", "فضل قيام الليل", "أفضل الصلاة بعد الصلاة المكتوبة صلاة الليل.", "صحيح مسلم (1163)"),
                LearningEntry("صفة الصلاة", "مثنى مثنى", "صلاة الليل ركعتين ركعتين، ثم يختم المصلي بوتر.", "صحيح البخاري (990)، صحيح مسلم (749)"),
                LearningEntry("القراءة والتدبر", "ليس المقصود مجرد العدد", "اقرأ ما تيسر، وتدبر المعنى، وخذ من القيام ما تقدر على المداومة عليه.", "المعنى مستفاد من عموم الأمر بترتيل القرآن والتدبر؛ المزمل (4)، ص (29)")
            )
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = LearningPale)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("سنن وفضائل الصلوات", color = LearningGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("اختر الصلاة لقراءة فضلها والهدي النبوي المتعلق بها. الفريضة والنافلة والراتبة أبواب مختلفة.")
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            prayers.forEach { prayer ->
                FilterChip(selected = selected == prayer, onClick = { selected = prayer }, label = { Text(prayer) })
            }
        }
        val page = content[selected]!!
        LearningCard(page.title, page.subtitle, page.entries)
    }
}

@Composable
fun NawafilLearningScreen() {
    var selected by remember { mutableStateOf("سنة الفجر") }
    val topics = listOf("سنة الفجر", "رواتب الظهر", "قبل العصر", "بعد المغرب", "بعد العشاء", "الضحى")
    val content = mapOf(
        "سنة الفجر" to LearningEntry("ركعتا الفجر", "ركعتان قبل فرض الفجر", "كان النبي ﷺ لا يدع ركعتي الفجر، وورد في فضلهما أنهما خير من الدنيا وما فيها. تُصليهما خفيفتين قبل الفريضة.", "صحيح مسلم (725)، صحيح البخاري (1169)"),
        "رواتب الظهر" to LearningEntry("السنن الراتبة للظهر", "أربع قبل الظهر وركعتان بعدها", "وردت أربع ركعات قبل الظهر وركعتان بعدها. وهناك روايات في أربع بعدها أيضًا؛ وتفاصيل الرواتب وعددها تُعرض بحسب الروايات.", "صحيح مسلم (728)، سنن أبي داود (1250)"),
        "قبل العصر" to LearningEntry("أربع ركعات قبل العصر", "نافلة مستحبة وليست من الرواتب المؤكدة", "ورد: رحم الله امرأً صلى قبل العصر أربعًا. تُصلى ركعتين ركعتين، ولا تُعامل على أنها فرض أو سنة راتبة مؤكدة.", "سنن أبي داود (1271)، جامع الترمذي (430)"),
        "بعد المغرب" to LearningEntry("ركعتا سنة المغرب", "بعد فرض المغرب", "ثبتت ركعتا سنة المغرب بعد الفريضة، ويُستحب أداؤهما في البيت إن تيسر.", "صحيح البخاري (1180)، صحيح مسلم (729)"),
        "بعد العشاء" to LearningEntry("ركعتا سنة العشاء", "بعد فرض العشاء", "ثبتت ركعتا سنة العشاء بعد الفريضة، وهما من الرواتب.", "صحيح البخاري (1180)، صحيح مسلم (729)"),
        "الضحى" to LearningEntry("صلاة الضحى", "نافلة مستقلة عن الرواتب", "وقتها بعد ارتفاع الشمس إلى قبيل الزوال، وأقلها ركعتان. لا تُحسب من الرواتب التابعة للصلوات الخمس.", "صحيح مسلم (720)، صحيح البخاري (1178)")
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = LearningPale)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("تعلم سنن النوافل", color = LearningGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("صفحات قصيرة لكل نافلة: وقتها وعدد ركعاتها وكيفيتها ودليلها. النوافل مستحبة وليست فرضًا.")
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            topics.forEach { topic ->
                FilterChip(selected = selected == topic, onClick = { selected = topic }, label = { Text(topic) })
            }
        }
        LearningCard(content[selected]!!.title, content[selected]!!.label, listOf(content[selected]!!))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("التدرج في العمل", color = LearningGreen, fontWeight = FontWeight.Bold)
                Text("ابدأ بما تستطيع المداومة عليه، ولا تجعل متابعة النوافل ضغطًا أو بديلًا عن الفرائض.")
                Text("قال رسول الله ﷺ: «أحب الأعمال إلى الله أدومها وإن قل».", style = MaterialTheme.typography.bodySmall)
                Text("صحيح البخاري (6464)، وصحيح مسلم (783).", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
