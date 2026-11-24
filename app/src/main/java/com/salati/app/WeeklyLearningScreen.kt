package com.salati.app

import android.content.SharedPreferences
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val LearningGreen = Color(0xFF164B3A)
private val LearningPale = Color(0xFFE5EBDD)
private val LearningCream = Color(0xFFF6F2E9)

private data class WeeklyLesson(
    val id: String,
    val title: String,
    val kind: String,
    val text: String,
    val attribution: String,
    val source: String,
    val explanation: String,
    val guardrail: String,
    val action: String,
    val question: String,
    val answer: String
)

private val weeklyLessons = listOf(
    WeeklyLesson(
        id = "ease",
        title = "اليسر والاستقامة بلا تكلف",
        kind = "حديث نبوي صحيح",
        text = "«إن الدين يسر، ولن يشاد الدين أحد إلا غلبه، فسددوا وقاربوا وأبشروا»",
        attribution = "أبو هريرة رضي الله عنه",
        source = "صحيح البخاري (39)، وصحيح مسلم (2816) بمعناه",
        explanation = "الدين قائم على اليسر، والنبي ﷺ وجّه إلى الاستقامة والمقاربة وترك التكلف الذي يفضي إلى الانقطاع.",
        guardrail = "اليسر لا يعني إسقاط الفرائض أو استباحة المحرمات؛ بل يكون في حدود الشرع والاستطاعة.",
        action = "اختر عملًا صالحًا صغيرًا تستطيع المداومة عليه دون أن يزاحم واجبًا.",
        question = "هل اليسر يعني أن أترك ما أوجبه الله إذا وجدته صعبًا؟",
        answer = "لا. المقصود الرفق وترك التكلف مع أداء ما أوجبه الله بحسب الاستطاعة والضوابط الشرعية."
    ),
    WeeklyLesson(
        id = "gentleness",
        title = "الرفق في التعليم والتعامل",
        kind = "حديث نبوي صحيح",
        text = "«يسروا ولا تعسروا، وبشروا ولا تنفروا»",
        attribution = "أنس بن مالك رضي الله عنه",
        source = "صحيح البخاري (69)، وصحيح مسلم (1734)",
        explanation = "يوجه الحديث إلى الرفق والتيسير والتبشير، والابتعاد عن الأسلوب الذي ينفّر الناس.",
        guardrail = "التيسير في الأسلوب لا يبيح تغيير حكم شرعي أو إخفاء الحقيقة.",
        action = "اختر موقفًا واحدًا هذا الأسبوع تتعامل فيه برفق وتشجيع.",
        question = "كيف أكون ميسّرًا دون أن أغيّر الحق؟",
        answer = "أحسن الأسلوب وأراعي حال الشخص، مع بيان الحكم كما هو وعدم تحويل الرفق إلى تساهل بلا ضابط."
    ),
    WeeklyLesson(
        id = "continuity",
        title = "المداومة أحب من الاندفاع المنقطع",
        kind = "حديث نبوي صحيح",
        text = "«أحب الأعمال إلى الله تعالى أدومها وإن قل»",
        attribution = "عائشة أم المؤمنين رضي الله عنها",
        source = "صحيح مسلم (783)، وصحيح البخاري (6465) بلفظ أطول",
        explanation = "يبين الحديث فضل العمل المشروع الذي يثبت عليه صاحبه، ولو كان قليلًا.",
        guardrail = "لا يعني ذلك أن القليل يغني عن الفرائض أو أن مقدار العمل هو معيار قبول الله.",
        action = "اختر عادة نافلة أو درسًا قصيرًا يناسب قدرتك، وعدّل حجمه عند الحاجة.",
        question = "ما الذي يميز المداومة عن كثرة البداية؟",
        answer = "أن العمل يستمر بقدر مستطاع، بدل الحماس المؤقت الذي يتبعه انقطاع."
    ),
    WeeklyLesson(
        id = "intention",
        title = "النية ومعنى العمل",
        kind = "حديث نبوي صحيح",
        text = "«إنما الأعمال بالنيات، وإنما لكل امرئ ما نوى»",
        attribution = "عمر بن الخطاب رضي الله عنه",
        source = "صحيح البخاري (1)، وصحيح مسلم (1907)",
        explanation = "للنية أثر عظيم في العمل، وبها يتميز المقصود من الفعل.",
        guardrail = "النية الحسنة لا تجعل العمل المحرم مشروعًا، ولا تغني عن صحة العمل.",
        action = "قبل عمل صالح أو مباح، خذ لحظة لاستحضار نيتك.",
        question = "هل النية الحسنة تجعل أي فعل صحيحًا؟",
        answer = "لا؛ النية مهمة، لكن العمل نفسه لا بد أن يكون مشروعًا وصحيحًا."
    ),
    WeeklyLesson(
        id = "teaching-gently",
        title = "التعليم دون إملال",
        kind = "حديث نبوي صحيح",
        text = "«كان النبي ﷺ يتخولنا بالموعظة في الأيام كراهة السآمة علينا»",
        attribution = "عبد الله بن مسعود رضي الله عنه",
        source = "صحيح البخاري (68)، وصحيح مسلم (2821) باختلاف يسير",
        explanation = "كان النبي ﷺ يراعي حال أصحابه، وينوّع أوقات الموعظة حتى لا يملّوا.",
        guardrail = "هذا توجيه في مراعاة المتعلم، وليس تحديدًا لعدد الدروس أو وجوب جدول أسبوعي.",
        action = "اجعل جلسة التعلم قصيرة وفي وقت مناسب لك، وافتح التفصيل عندما تكون مستعدًا.",
        question = "لماذا لا نملأ كل يوم بعشرات الرسائل التعليمية؟",
        answer = "لأن مراعاة حال المتعلم والرفق به يعينان على الانتفاع والاستمرار، كما دل الحديث."
    ),
    WeeklyLesson(
        id = "understand-and-act",
        title = "افهم ما تتعلمه واعمل به",
        kind = "أثر عن صحابي — ليس حديثًا مرفوعًا",
        text = "«كان الرجل منا إذا تعلم عشر آيات لم يجاوزهن حتى يعرف معانيهن والعمل بهن»",
        attribution = "أثر عن عبد الله بن مسعود رضي الله عنه",
        source = "تفسير الطبري (1/44)، وورد الحكم عليه بالصحة في الموسوعة الحديثية",
        explanation = "ينقل الأثر طريقة في التدرج: معرفة المعنى وربط التعلم بالعمل قبل الانتقال إلى مزيد من المادة.",
        guardrail = "هذا أثر موقوف على ابن مسعود، وليس حديثًا نبويًا. والعدد المذكور ليس حدًا ملزمًا لكل متعلم.",
        action = "اختر آية، وافهم معناها من تفسير موثوق، ثم فكر في عمل مشروع يرتبط بها.",
        question = "ما الذي كان يجمعه الصحابة في هذا الأثر؟",
        answer = "جمعوا بين تعلم الآيات وفهم معانيها والعمل بها."
    ),
    WeeklyLesson(
        id = "istikhara",
        title = "الاستخارة عند الهمّ بالأمر",
        kind = "حديث نبوي صحيح",
        text = "«كان رسول الله ﷺ يعلمنا الاستخارة في الأمور كلها كما يعلمنا السورة من القرآن»",
        attribution = "جابر بن عبد الله رضي الله عنهما",
        source = "صحيح البخاري (1162)",
        explanation = "يدل الحديث على عناية النبي ﷺ بتعليم الاستخارة، ودعاء الله أن يقدر الخير ويصرف الشر.",
        guardrail = "الاستخارة ليست تنبؤًا بالغيب، ولا يشترط بعدها أن يرى الإنسان حلمًا. ولا يغني ذلك عن التفكير والاستشارة.",
        action = "تعلّم دعاء الاستخارة، وارجع إليه عندما تهمّ بأمر يحتاج إلى استخارة.",
        question = "ما الذي يسأله المسلم ربه في دعاء الاستخارة؟",
        answer = "يسأله أن يقدّر له الخير وييسره ويبارك فيه، وأن يصرف عنه الشر ويصرفه عنه ويرضيه بالخير."
    ),
    WeeklyLesson(
        id = "fiqh-difference",
        title = "كيف نفهم الاختلاف الفقهي؟",
        kind = "درس منهجي — ليس حديثًا",
        text = "الخلاف الفقهي المعتبر في بعض الفروع قد يكون سعة، لكن ليس كل قول منقول خلافًا معتبرًا.",
        attribution = "مادة تحريرية تعليمية تحتاج مراجعة فقهية",
        source = "دار الإفتاء المصرية: الاختلاف بين المذاهب الفقهية؛ ومجمع الفقه الإسلامي الدولي",
        explanation = "نتعلم احترام المذاهب وعرض أقوالها بأمانة، مع معرفة محل الاتفاق ومحل الاجتهاد.",
        guardrail = "عبارة «اختلاف أمتي رحمة» لا تُعرض على أنها حديث ثابت عن النبي ﷺ، ولا تُسوّغ الأقوال الشاذة.",
        action = "عند سماع مسألة خلافية، اسأل: ما مصدر القول؟ وهل الخلاف فيها معتبر؟",
        question = "هل كل رأي فقهي منشور على الإنترنت يصح اختياره؟",
        answer = "لا. يلزم التحقق من نسبة القول ومن كونه قولًا معتبرًا له مساغ فقهي."
    )
)

@Composable
fun WeeklyLearningScreen(prefs: SharedPreferences) {
    val today = LocalDate.now()
    val startDate = remember {
        prefs.getString("weekly_learning_started_on", null) ?: today.toString().also {
            prefs.edit().putString("weekly_learning_started_on", it).apply()
        }
    }
    val elapsedWeeks = remember(startDate, today) {
        runCatching {
            (ChronoUnit.DAYS.between(LocalDate.parse(startDate), today).coerceAtLeast(0L) / 7L).toInt()
        }.getOrDefault(0)
    }
    val currentIndex = elapsedWeeks.coerceIn(0, weeklyLessons.lastIndex)
    var selectedIndex by remember { mutableIntStateOf(currentIndex) }
    LaunchedEffect(currentIndex) { selectedIndex = currentIndex }

    val lesson = weeklyLessons[selectedIndex]
    var answerVisible by remember(lesson.id) { mutableStateOf(false) }
    var istikharaDetailsVisible by remember { mutableStateOf(false) }
    var lessonDone by remember(lesson.id) {
        mutableStateOf(prefs.getBoolean("weekly_lesson_done_" + lesson.id, false))
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = LearningGreen)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("رحلة تعلم هادئة", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.labelMedium)
                Text("حديث أو معنى واحد كل أسبوع", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "نتعلم قليلًا، نفهم، ثم نختار خطوة عملية. يمكنك الرجوع للدروس السابقة في أي وقت.",
                    color = Color.White.copy(alpha = .92f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("الدرس ${selectedIndex + 1} من ${weeklyLessons.size}", color = Color(0xFFE8D7A9), fontWeight = FontWeight.SemiBold)
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weeklyLessons.forEachIndexed { index, _ ->
                if (index <= currentIndex) {
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        label = { Text("درس ${index + 1}") }
                    )
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(lesson.kind, color = LearningGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(lesson.title, color = LearningGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(lesson.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(lesson.attribution, color = Color(0xFF6D716A), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(color = LearningPale)
                Text("المصدر", color = LearningGreen, fontWeight = FontWeight.Bold)
                Text(lesson.source, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(color = LearningPale)
                Text("المعنى", color = LearningGreen, fontWeight = FontWeight.Bold)
                Text(lesson.explanation)
                Text("تنبيه مهم", color = LearningGreen, fontWeight = FontWeight.Bold)
                Text(lesson.guardrail, color = Color(0xFF765B38))
                Surface(color = LearningCream, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("خطوة عملية اختيارية", color = LearningGreen, fontWeight = FontWeight.Bold)
                        Text(lesson.action)
                    }
                }
                Text("اختبر فهمك", color = LearningGreen, fontWeight = FontWeight.Bold)
                Text(lesson.question)
                OutlinedButton(onClick = { answerVisible = !answerVisible }) {
                    Text(if (answerVisible) "إخفاء الإجابة" else "إظهار الإجابة")
                }
                if (answerVisible) Text(lesson.answer, color = Color(0xFF4E6254))
                HorizontalDivider(color = LearningPale)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = lessonDone,
                        onCheckedChange = {
                            lessonDone = it
                            prefs.edit().putBoolean("weekly_lesson_done_" + lesson.id, it).apply()
                        }
                    )
                    Column(Modifier.weight(1f)) {
                        Text("سجلت أنني قرأت هذا الدرس", fontWeight = FontWeight.SemiBold)
                        Text("هذا تذكير شخصي فقط، وليس تقييمًا للعمل أو القبول.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF77736B))
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = LearningPale)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تذكرة عند الحيرة: الاستخارة", color = LearningGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("علّم النبي ﷺ أصحابه الاستخارة في الأمور كما يعلّمهم السورة من القرآن — صحيح البخاري (1162).")
                Text("إذا هممت بأمر، تعلّم صلاة الاستخارة ودعاءها، وخذ بالأسباب واستشر من تثق بعلمه.")
                TextButton(onClick = { istikharaDetailsVisible = !istikharaDetailsVisible }) {
                    Text(if (istikharaDetailsVisible) "إخفاء التذكرة" else "اعرف طريقة الاستخارة")
                }
                if (istikharaDetailsVisible) {
                    Text("ورد في الحديث صلاة ركعتين من غير الفريضة ثم دعاء الاستخارة مع تسمية الحاجة. راجع نص الدعاء كاملًا في صحيح البخاري (1162).")
                    Text("لا يشترط أن ترى منامًا بعد الاستخارة؛ انظر فتوى دار الإفتاء المصرية: «معرفة نتيجة صلاة الاستخارة» (فتوى 349).", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text(
            "المحتوى التعليمي هنا مختصر. ارجع إلى المصدر المذكور، ولا يُغني الشرح عن سؤال أهل العلم في المسائل الخاصة.",
            color = Color(0xFF77736B),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
