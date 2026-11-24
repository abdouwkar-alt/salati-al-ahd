package com.salati.app

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val CareGreen = Color(0xFF164B3A)
private val CarePale = Color(0xFFE5EBDD)
private val CareCream = Color(0xFFF6F2E9)

@Composable
fun RemembranceCareScreen(prefs: SharedPreferences) {
    var selected by remember { mutableStateOf("أشخاص في دعائك") }
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var people by remember { mutableStateOf(prefs.getStringSet("care_remembrance_people", emptySet())?.toList()?.sorted().orEmpty()) }
    var family by remember { mutableStateOf(prefs.getStringSet("care_kinship_people", emptySet())?.toList()?.sorted().orEmpty()) }
    var feedback by remember { mutableStateOf("") }
    fun persist(key: String, values: List<String>) { prefs.edit().putStringSet(key, values.toSet()).apply() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = CarePale)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("الإنفاق والوفاء للناس", color = CareGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("مساحة شخصية للتذكير بمن تحب، وبأبواب الخير التي تريد أن تتعلمها أو تتذكرها.")
                Text("التطبيق لا يجمع تبرعات ولا يحوّل أموالًا ولا يتواصل مع أي جهة نيابةً عنك.", color = Color(0xFF765B38), style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selected == "أشخاص في دعائك", onClick = { selected = "أشخاص في دعائك"; name = ""; note = ""; feedback = "" }, label = { Text("أشخاص في دعائك") })
            FilterChip(selected = selected == "صلة الرحم", onClick = { selected = "صلة الرحم"; name = ""; note = ""; feedback = "" }, label = { Text("صلة الرحم") })
        }
        if (selected == "أشخاص في دعائك") {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("لا تدع من تحب يغيب عن دعائك", color = CareGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("سجّل اسم شخص تحب أن تتذكره في الدعاء، حيًا كان أو متوفى. واكتب تذكيرًا شخصيًا مثل الدعاء له أو عمل خير تنوي فعله.")
                    OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("الاسم أو الوصف") })
                    OutlinedTextField(value = note, onValueChange = { note = it.take(120) }, modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("ملاحظة اختيارية") })
                    Button(onClick = {
                        val clean = name.trim()
                        if (clean.isNotEmpty()) {
                            val entry = if (note.isBlank()) clean else clean + " — " + note.trim()
                            people = (people + entry).distinct().sorted()
                            persist("care_remembrance_people", people)
                            name = ""; note = ""; feedback = "تم حفظ التذكير على هذا الجهاز."
                        } else feedback = "اكتب اسمًا أو وصفًا أولًا."
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CareGreen)) { Text("حفظ في قائمة التذكير") }
                    if (feedback.isNotBlank()) Text(feedback, color = CareGreen, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("قائمة من تحب أن تتذكرهم", color = CareGreen, fontWeight = FontWeight.Bold)
            if (people.isEmpty()) Text("لم تضف أسماء بعد.")
            people.forEach { entry -> ListItem(headlineContent = { Text(entry) }, trailingContent = {
                TextButton(onClick = { people = people - entry; persist("care_remembrance_people", people) }) { Text("حذف") }
            }, colors = ListItemDefaults.colors(containerColor = Color.White)) }
            Card(colors = CardDefaults.cardColors(containerColor = CareCream)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("من أبواب الإنفاق", color = CareGreen, fontWeight = FontWeight.Bold)
                    Text("الصدقة على المحتاج، وإطعام الطعام، وسقي الماء، وكفالة اليتيم، ومساعدة الغارم، والإنفاق على الأهل، وبناء المساجد والمرافق النافعة، ودعم التعليم والعلاج، وإغاثة الملهوف.")
                    Text("هذه أمثلة عامة؛ تختلف الأولوية باختلاف الحاجة والبلد والجهة الموثوقة. تحرّ التثبت من المستفيد والجهة قبل التبرع.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF765B38))
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("صلة الرحم تبدأ بخطوة صغيرة", color = CareGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("سجّل قريبًا أو صديقًا تريد الاطمئنان عليه، واكتب وسيلة التذكير المناسبة لك. لا يراقب التطبيق مكالماتك أو رسائلك؛ أنت من يؤكد إنك تواصلت.")
                    OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("اسم القريب أو الصديق") })
                    OutlinedTextField(value = note, onValueChange = { note = it.take(120) }, modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("ملاحظة: اتصال أسبوعي، زيارة، تهنئة عيد...") })
                    Button(onClick = {
                        val clean = name.trim()
                        if (clean.isNotEmpty()) {
                            val entry = if (note.isBlank()) clean else clean + " — " + note.trim()
                            family = (family + entry).distinct().sorted()
                            persist("care_kinship_people", family)
                            name = ""; note = ""; feedback = "تم حفظ الاسم. جدولة التنبيهات الدورية لم تُفعّل بعد."
                        } else feedback = "اكتب اسمًا أولًا."
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CareGreen)) { Text("إضافة إلى قائمة صلة الرحم") }
                    if (feedback.isNotBlank()) Text(feedback, color = CareGreen, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("قائمة المتابعة", color = CareGreen, fontWeight = FontWeight.Bold)
            if (family.isEmpty()) Text("لم تضف أسماء بعد.")
            family.forEach { entry -> ListItem(headlineContent = { Text(entry) }, trailingContent = {
                TextButton(onClick = { family = family - entry; persist("care_kinship_people", family) }) { Text("حذف") }
            }, colors = ListItemDefaults.colors(containerColor = Color.White)) }
        }
        Text("البيانات محفوظة محليًا على جهازك. لا تُرسل الأسماء إلى خادم.", color = Color(0xFF77736B), style = MaterialTheme.typography.bodySmall)
    }
}
