package com.salati.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val AreaGreen = Color(0xFF164B3A)
private val AreaPale = Color(0xFFE5EBDD)
private val AreaCream = Color(0xFFF6F2E9)

private data class AreaAction(
    val title: String,
    val description: String,
    val section: String,
    val destination: String,
    val subTab: String
)

@Composable
private fun AreaIntro(title: String, description: String, note: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AreaGreen),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(description, color = Color.White.copy(alpha = .94f), style = MaterialTheme.typography.bodyMedium)
            Text(note, color = Color(0xFFE8D7A9), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AreaActionList(
    actions: List<AreaAction>,
    onOpen: (String, String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        actions.forEach { action ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E0D5))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 11.dp, bottom = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(action.title, color = AreaGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(action.description, color = Color(0xFF66665F), style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { onOpen(action.section, action.destination, action.subTab) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AreaGreen),
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp)
                    ) {
                        Text("افتح")
                    }
                }
            }
        }
    }
}

@Composable
fun WorshipAreaHubScreen(onOpen: (String, String, String) -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AreaIntro(
            title = "العبادة",
            description = "مساحة تجمع أعمال العبادة ومتابعتها، مع توضيح ما هو فرض وما هو نافلة، ومصادر المسائل التعليمية.",
            note = "اختر العمل الذي تريد البدء به. لا توجد درجات أو مقارنة بين المستخدمين."
        )
        Text("ابدأ من هنا", color = AreaGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        AreaActionList(
            listOf(
                AreaAction("الصلاة المفروضة", "مواقيت اليوم وتسجيل الصلاة ومتابعة حالتها.", "الصلاة", "الصلاة", "تسجيل الصلاة"),
                AreaAction("قيام الليل", "جلسة قراءة متدرجة، مع إمكانية الاستكمال وسجل للجلسات.", "الصلاة", "قيام الليل", "جلسة قيام الليل"),
                AreaAction("القرآن والتدبر", "افتح المصحف وتابع القراءة والتدبر.", "تدبر القرآن", "تدبر القرآن", "المصحف والتدبر"),
                AreaAction("السنن والنوافل", "تعرف على السنن الرواتب والنوافل وسجل ما تريد متابعته.", "النوافل", "النوافل", "تسجيل النوافل"),
                AreaAction("الصيام", "متابعة صيام رمضان والتطوع.", "الصوم", "الصوم", "الصيام"),
                AreaAction("قضاء الفوائت", "تنظيم تقديري لما تريد متابعته، مع بيان حدود الحاسبة والخلاف الفقهي.", "الصلاة", "القضاء", "القضاء"),
                AreaAction("فقه الوضوء والصلاة", "مادة تعليمية منظمة بحسب الموضوع والمذهب المختار.", "التفقه", "فقه الوضوء", "فقه الوضوء")
            ),
            onOpen
        )
        Text(
            "المحتوى التعليمي للتعريف والمراجعة، وليس فتوى شخصية. عند وجود حالة خاصة اسأل أهل العلم.",
            color = Color(0xFF765B38),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun GoodPeopleAreaHubScreen(onOpen: (String, String, String) -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AreaIntro(
            title = "الخير والناس",
            description = "أعمال خير وعلاقات إنسانية تختار أنت ما تريد تذكره أو متابعته، دون مراقبة اتصالاتك أو التواصل مع الآخرين نيابةً عنك.",
            note = "كل التذكيرات اختيارية، ويمكنك تعديلها أو إيقافها."
        )
        Text("مساحاتك الشخصية", color = AreaGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        AreaActionList(
            listOf(
                AreaAction("أشخاص في دعائك", "قائمة شخصية لتذكر من تحب الدعاء لهم، دون مشاركة الأسماء مع أي جهة.", "الخير والناس", "الإنفاق", "أشخاص في دعائك"),
                AreaAction("صلة الرحم", "سجّل من تريد الاطمئنان عليهم، واختر بنفسك طريقة التواصل المناسبة.", "الخير والناس", "الإنفاق", "صلة الرحم"),
                AreaAction("روتين يومك", "أنشئ تذكيرات شخصية بوقت وأيام تختارها، مع إمكانية الإيقاف والحذف.", "روتين يومك", "روتين_اليوم", "التذكيرات")
            ),
            onOpen
        )
        Card(colors = CardDefaults.cardColors(containerColor = AreaCream)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("الخصوصية", color = AreaGreen, fontWeight = FontWeight.Bold)
                Text("الأسماء والملاحظات التي تضيفها محفوظة محليًا على جهازك. التطبيق لا يجمع تبرعات ولا يقرأ جهات الاتصال أو الرسائل.")
            }
        }
    }
}
