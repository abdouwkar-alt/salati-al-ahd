package com.salati.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val ShellGreen = Color(0xFF164B3A)
private val ShellCream = Color(0xFFF6F2E9)
private val ShellLine = Color(0xFFD8D2C4)

@Composable
fun HomeDashboard(
    dateText: String,
    onOpen: (section: String, destination: String, subTab: String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = ShellGreen), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("أشهد أن لا إله إلا الله، وأشهد أن محمدًا رسول الله", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Surface(color = ShellCream, shape = RoundedCornerShape(24.dp)) {
                    Text("رضيت بالله ربًا، وبالإسلام دينًا، وبمحمدٍ رسولًا", color = ShellGreen, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(dateText, color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.labelSmall)
            }
        }
        JourneySketch(Modifier.fillMaxWidth().height(118.dp))
        Text(
            "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp),
            color = ShellGreen,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeTile("التفكر", "التفكر", "التفكر", onOpen)
                HomeTile("سنن الرسول ﷺ", "سنن الرسول ﷺ", "السنن", onOpen)
                HomeTile("تدبر القرآن", "تدبر القرآن", "تدبر القرآن", onOpen)
                HomeTile("التفقه", "التفقه", "فقه الصلاة", onOpen)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeTile("الإنفاق", "الإنفاق", "الإنفاق", onOpen)
                HomeTile("الأذكار", "الأذكار", "الأذكار", onOpen)
                HomeTile("الصوم", "الصوم", "الصوم", onOpen)
                HomeTile("الدعاء", "الدعاء", "الأدعية", onOpen)
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HomeTile(
                title = "الصلاة",
                section = "الصلاة",
                destination = "الصلاة",
                onOpen = onOpen,
                subtitle = "عماد الدين",
                emphasized = true,
                modifier = Modifier.fillMaxWidth(.62f)
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Surface(color = ShellGreen, shape = RoundedCornerShape(14.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun HomeTile(
    title: String,
    section: String,
    destination: String,
    onOpen: (String, String, String) -> Unit,
    subtitle: String? = null,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().heightIn(min = 62.dp).clickable { onOpen(section, destination, "") },
        colors = CardDefaults.cardColors(containerColor = if (emphasized) ShellGreen else Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (emphasized) ShellGreen else ShellLine),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(title, color = if (emphasized) Color.White else ShellGreen, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            if (subtitle != null) Text(subtitle, color = if (emphasized) ShellCream else ShellGreen.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SectionNavigation(
    section: String,
    selected: String,
    onSelect: (label: String, destination: String) -> Unit
) {
    val options = when (section) {
        "الصلاة" -> listOf("الصلاة المفروضة" to "الصلاة", "السنن والرواتب" to "السنن", "قيام الليل" to "قيام الليل", "القضاء" to "القضاء")
        "الصوم" -> listOf("الصيام" to "الصوم", "قضاء الصيام" to "القضاء")
        "الأذكار" -> listOf("مكتبة الأذكار" to "الأذكار")
        "الدعاء" -> listOf("مكتبة الأدعية" to "الأدعية")
        "الإنفاق" -> listOf("الزكاة" to "الإنفاق", "الصدقة" to "الإنفاق", "الإنفاق" to "الإنفاق")
        "سنن الرسول ﷺ" -> listOf("سنن الصلاة" to "السنن", "سنن اليوم والليلة" to "السنن", "سنن البيت والمسجد" to "السنن")
        "تدبر القرآن" -> listOf("المصحف والتدبر" to "تدبر القرآن")
        "التفكر" -> listOf("التفكر" to "التفكر", "محاسبة النفس" to "التفكر")
        "التفقه" -> listOf("فقه الوضوء" to "فقه الوضوء", "فقه الصلاة" to "فقه الصلاة")
        else -> emptyList()
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(section, color = ShellGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (label, destination) ->
                FilterChip(
                    selected = selected == label || (selected.isBlank() && options.firstOrNull()?.first == label),
                    onClick = { onSelect(label, destination) },
                    label = { Text(label, maxLines = 1) }
                )
            }
        }
    }
}

@Composable
fun SectionSketch(section: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAE5D9)), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Canvas(Modifier.size(width = 92.dp, height = 58.dp)) {
                val c = ShellGreen
                if (section == "الصلاة") {
                    val xs = listOf(.14f, .38f, .62f, .86f)
                    xs.forEachIndexed { index, x ->
                        val base = size.height * .78f
                        val headY = if (index == 2) size.height * .55f else size.height * .22f
                        drawCircle(c, size.minDimension * .065f, Offset(size.width*x, headY))
                        if (index == 0) drawLine(c, Offset(size.width*x, headY+8), Offset(size.width*x, base), 3.dp.toPx())
                        else if (index == 1) drawLine(c, Offset(size.width*x-5, headY+8), Offset(size.width*x+8, base-6), 3.dp.toPx())
                        else if (index == 2) drawLine(c, Offset(size.width*x-8, base-10), Offset(size.width*x+8, base-10), 3.dp.toPx())
                        else drawLine(c, Offset(size.width*x-8, base-8), Offset(size.width*x+8, base-8), 3.dp.toPx())
                    }
                } else {
                    drawLine(c, Offset(4f, size.height*.75f), Offset(size.width-4f, size.height*.75f), 2.dp.toPx())
                    drawCircle(c, 8.dp.toPx(), Offset(size.width*.48f, size.height*.24f))
                    drawLine(c, Offset(size.width*.48f, size.height*.34f), Offset(size.width*.48f, size.height*.68f), 3.dp.toPx())
                    drawLine(c, Offset(size.width*.48f, size.height*.45f), Offset(size.width*.30f, size.height*.58f), 3.dp.toPx())
                    drawLine(c, Offset(size.width*.48f, size.height*.45f), Offset(size.width*.66f, size.height*.58f), 3.dp.toPx())
                    drawLine(c, Offset(size.width*.48f, size.height*.68f), Offset(size.width*.36f, size.height*.88f), 3.dp.toPx())
                    drawLine(c, Offset(size.width*.48f, size.height*.68f), Offset(size.width*.60f, size.height*.88f), 3.dp.toPx())
                }
            }
            Text(
                when (section) {
                    "الصلاة" -> "سكتش مبسط لهيئات الصلاة"
                    "الصوم" -> "متابعة الصيام وأيامه"
                    "الأذكار" -> "أذكار ثابتة وأوقات الذكر"
                    "الدعاء" -> "أدعية من القرآن والسنة"
                    "الإنفاق" -> "الزكاة والصدقة والإنفاق"
                    "سنن الرسول ﷺ" -> "التعرّف على هدي الرسول ﷺ"
                    "تدبر القرآن" -> "قراءة وتدبر وتدوين"
                    "التفكر" -> "مساحة للتأمل ومراجعة النفس"
                    else -> "التعلم والتفقه"
                },
                color = ShellGreen,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun SectionPlaceholder(title: String, subtitle: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, ShellLine)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = ShellGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, color = ShellGreen, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Color(0xFF5F625D))
            Text("قسم أولي — المحتوى التفصيلي قيد البناء والمراجعة.", color = Color(0xFF7A715F), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun BottomNavigationBar(selected: String, onSelect: (String) -> Unit) {
    Surface(color = ShellGreen, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("الإعدادات", "الإحصائيات", "البداية").forEach { item ->
                val active = selected == item
                Surface(
                    modifier = Modifier.weight(1f).clickable { onSelect(item) },
                    color = if (active) ShellCream else ShellGreen,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(item, modifier = Modifier.padding(vertical = 13.dp, horizontal = 4.dp), textAlign = TextAlign.Center, color = if (active) ShellGreen else Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun JourneySketch(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val line = Path().apply {
            moveTo(size.width*.06f, size.height*.82f)
            cubicTo(size.width*.25f, size.height*.28f, size.width*.53f, size.height*1.05f, size.width*.72f, size.height*.42f)
            cubicTo(size.width*.83f, size.height*.10f, size.width*.91f, size.height*.22f, size.width*.98f, size.height*.08f)
        }
        drawPath(line, Color(0xFFB9B09E), style = Stroke(3.dp.toPx()))
        val x=size.width*.48f; val y=size.height*.42f
        drawCircle(ShellGreen, 7.dp.toPx(), Offset(x,y-15.dp.toPx()))
        drawLine(ShellGreen, Offset(x,y-7.dp.toPx()), Offset(x,y+16.dp.toPx()), 4.dp.toPx())
        drawLine(ShellGreen, Offset(x,y), Offset(x-10.dp.toPx(),y+9.dp.toPx()), 3.dp.toPx())
        drawLine(ShellGreen, Offset(x,y), Offset(x+11.dp.toPx(),y+5.dp.toPx()), 3.dp.toPx())
        drawLine(ShellGreen, Offset(x,y+16.dp.toPx()), Offset(x-8.dp.toPx(),y+30.dp.toPx()), 3.dp.toPx())
        drawLine(ShellGreen, Offset(x,y+16.dp.toPx()), Offset(x+9.dp.toPx(),y+30.dp.toPx()), 3.dp.toPx())
        drawCircle(Color(0xFF8E9A78), 4.dp.toPx(), Offset(size.width*.15f,size.height*.74f))
        drawCircle(Color(0xFF8E9A78), 4.dp.toPx(), Offset(size.width*.84f,size.height*.30f))
    }
}

data class DhikrBurst(val id: Int, val position: Offset)

@Composable
fun DhikrBurstLabel(burst: DhikrBurst, onFinished: () -> Unit) {
    val progress = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(burst.id) {
        progress.animateTo(1f, androidx.compose.animation.core.tween(620))
        onFinished()
    }
    Text(
        "أستغفر الله",
        color = ShellGreen,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .offset {
                androidx.compose.ui.unit.IntOffset(
                    burst.position.x.toInt(),
                    burst.position.y.toInt() - (progress.value * 56f).toInt()
                )
            }
            .graphicsLayer { alpha = 1f - progress.value }
    )
}
