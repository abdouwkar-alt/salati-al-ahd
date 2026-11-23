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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val ShellGreen = Color(0xFF164B3A)
private val ShellCream = Color(0xFFF6F2E9)
private val ShellLine = Color(0xFFD8D2C4)

@Composable
fun HomeDashboard(
    dateText: String,
    onOpen: (section: String, destination: String, subTab: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = ShellGreen), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("أشهد أن لا إله إلا الله، وأشهد أن محمدًا رسول الله", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(5.dp))
                Surface(color = ShellCream, shape = RoundedCornerShape(24.dp)) {
                    Text("رضيت بالله ربًا، وبالإسلام دينًا، وبمحمدٍ رسولًا", color = ShellGreen, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(3.dp))
                Text(dateText, color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.labelSmall)
            }
        }
        JourneyHome(onOpen = onOpen, modifier = Modifier.weight(1f).fillMaxWidth())
    }
}

private data class JourneyDoor(
    val title: String,
    val section: String,
    val destination: String,
    val x: Float,
    val y: Float
)

@Composable
private fun JourneyHome(onOpen: (String, String, String) -> Unit, modifier: Modifier = Modifier) {
    val doors = listOf(
        JourneyDoor("الإنفاق", "الإنفاق", "الإنفاق", .76f, .14f),
        JourneyDoor("الصدقات", "الإنفاق", "الإنفاق", .24f, .23f),
        JourneyDoor("الصوم", "الصوم", "الصوم", .76f, .32f),
        JourneyDoor("التفقه", "التفقه", "فقه الصلاة", .24f, .41f),
        JourneyDoor("الأذكار", "الأذكار", "الأذكار", .76f, .50f),
        JourneyDoor("سنن الرسول ﷺ", "سنن الرسول ﷺ", "السنن", .24f, .59f),
        JourneyDoor("القرآن", "تدبر القرآن", "تدبر القرآن", .76f, .68f),
        JourneyDoor("الدعاء", "الدعاء", "الأدعية", .24f, .77f)
    )
    val prayerX = .5f
    val prayerY = .89f
    val selected = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    val opened = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    val progress = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    val prayerProgress = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (opened.intValue == 8) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(850, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "prayerDoor"
    )
    val verses = listOf(
        "وَاللَّهُ يَهْدِي مَن يَشَاءُ إِلَىٰ صِرَاطٍ مُّسْتَقِيمٍ",
        "وَأَنْ لَيْسَ لِلْإِنْسَانِ إِلَّا مَا سَعَىٰ",
        "وَأَنَّ سَعْيَهُ سَوْفَ يُرَىٰ",
        "فَإِذَا فَرَغْتَ فَانْصَبْ",
        "وَأَنَّ إِلَىٰ رَبِّكَ الْمُنْتَهَىٰ",
        "إِنَّ سَعْيَكُمْ لَشَتَّىٰ",
        "فَاسْتَبِقُوا الْخَيْرَاتِ",
        "وَسَارِعُوا إِلَىٰ مَغْفِرَةٍ مِنْ رَبِّكُمْ"
    )
    val currentVerse = if (selected.intValue < 0) verses[0] else verses[(selected.intValue + 1) % verses.size]
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val doorHeight = (maxHeight * .085f).coerceIn(42.dp, 50.dp)
        val doorWidth = (maxWidth * .205f).coerceIn(64.dp, 76.dp)
        val points = buildList {
            add(androidx.compose.ui.geometry.Offset(widthPx * .5f, heightPx * .105f))
            doors.forEach { add(androidx.compose.ui.geometry.Offset(widthPx * it.x, heightPx * it.y)) }
            add(androidx.compose.ui.geometry.Offset(widthPx * prayerX, heightPx * prayerY))
        }
        val route = androidx.compose.runtime.remember(widthPx, heightPx) { buildJourneyRoute(points) }
        val routeLength = route.lastOrNull()?.distance ?: 1f
        val targetDistance = when {
            selected.intValue == 8 -> routeLength
            selected.intValue >= 0 -> route.firstOrNull { it.nodeIndex == selected.intValue + 1 }?.distance ?: 0f
            else -> 0f
        }
        val targetProgress = (targetDistance / routeLength).coerceIn(0f, 1f)
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val path = androidx.compose.ui.graphics.Path()
            route.forEachIndexed { index, sample ->
                if (index == 0) path.moveTo(sample.point.x, sample.point.y) else path.lineTo(sample.point.x, sample.point.y)
            }
            drawPath(path, Color(0xFFD8C9A9).copy(alpha = .36f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 13.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawPath(path, Color(0xFFB9A77F).copy(alpha = .82f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 7.dp.toPx())), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Text(
            "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 0.dp),
            color = ShellGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge
        )
        doors.forEachIndexed { index, door ->
            val doorOpen = opened.intValue == index
            Column(
                modifier = Modifier.offset(
                    x = maxWidth * door.x - doorWidth / 2,
                    y = maxHeight * door.y - doorHeight * .40f
                ).width(doorWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(door.title, color = ShellGreen, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                JourneyDoorGraphic(open = doorOpen, onOpen = { selected.intValue = index; opened.intValue = -1 }, compactHeight = doorHeight)
            }
        }
        Column(
            modifier = Modifier.offset(x = maxWidth * prayerX - maxWidth * .18f, y = maxHeight * prayerY - 34.dp).width(maxWidth * .36f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("الصلاة جامعة", color = ShellGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
            PrayerDoorGraphic(openProgress = prayerProgress.value, onOpen = { selected.intValue = 8; opened.intValue = 8 }, compact = true)
            Text("عماد الدين", color = ShellGreen.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
        }
        val walkerPoint = routePointAt(route, routeLength * progress.value)
        if (selected.intValue >= 0) {
            Text(
                currentVerse,
                modifier = Modifier.offset {
                    androidx.compose.ui.unit.IntOffset(
                        (walkerPoint.x - widthPx * .40f).roundToInt(),
                        (walkerPoint.y - with(density) { 48.dp.toPx() }).coerceAtLeast(0f).roundToInt()
                    )
                }.width(maxWidth * .80f),
                color = ShellGreen, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelSmall
            )
        }
        androidx.compose.foundation.Canvas(
            Modifier.offset {
                val p = if (selected.intValue >= 0) walkerPoint else androidx.compose.ui.geometry.Offset(widthPx * .5f, heightPx * .105f)
                androidx.compose.ui.unit.IntOffset((p.x - with(density) { 12.dp.toPx() }).roundToInt(), (p.y - with(density) { 22.dp.toPx() }).roundToInt())
            }.size(24.dp, 44.dp)
        ) {
            val c = Color(0xFF59675D)
            drawCircle(c, 5.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width / 2, 6.dp.toPx()))
            drawRoundRect(c, androidx.compose.ui.geometry.Offset(size.width * .31f, 13.dp.toPx()), androidx.compose.ui.geometry.Size(size.width * .38f, 17.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()))
            drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .34f, 17.dp.toPx()), androidx.compose.ui.geometry.Offset(size.width * .12f, 28.dp.toPx()), 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .66f, 17.dp.toPx()), androidx.compose.ui.geometry.Offset(size.width * .88f, 28.dp.toPx()), 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            val swing = if (progress.value % .08f < .04f) 1f else -1f
            drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .43f, 29.dp.toPx()), androidx.compose.ui.geometry.Offset(size.width * .28f, (40f + swing * 2f).dp.toPx()), 3.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(c, androidx.compose.ui.geometry.Offset(size.width * .57f, 29.dp.toPx()), androidx.compose.ui.geometry.Offset(size.width * .72f, (40f - swing * 2f).dp.toPx()), 3.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        androidx.compose.runtime.LaunchedEffect(selected.intValue, targetProgress) {
            if (selected.intValue >= 0) {
                progress.snapTo(0f)
                progress.animateTo(targetProgress, animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = (3200 + (selected.intValue + 1) * 800).coerceAtMost(9600),
                    easing = androidx.compose.animation.core.LinearEasing
                ))
                opened.intValue = selected.intValue
                kotlinx.coroutines.delay(900)
                val door = if (selected.intValue == 8) null else doors[selected.intValue]
                if (door != null) onOpen(door.section, door.destination, "") else onOpen("الصلاة", "الصلاة", "")
                selected.intValue = -1
            }
        }
    }
}

private data class JourneySample(val point: androidx.compose.ui.geometry.Offset, val distance: Float, val nodeIndex: Int)

private fun buildJourneyRoute(points: List<androidx.compose.ui.geometry.Offset>): List<JourneySample> {
    if (points.size < 2) return emptyList()
    val result = mutableListOf<JourneySample>()
    var total = 0f
    var previous: androidx.compose.ui.geometry.Offset? = null
    points.zipWithNext().forEachIndexed { segment, pair ->
        val p0 = pair.first
        val p1 = pair.second
        val midY = (p0.y + p1.y) / 2f
        val bend = if (segment % 2 == 0) 0.10f else -0.10f
        val c1 = androidx.compose.ui.geometry.Offset(p0.x + (p1.x - p0.x) * bend + (p1.y - p0.y) * .18f, midY)
        val c2 = androidx.compose.ui.geometry.Offset(p1.x - (p1.x - p0.x) * bend - (p1.y - p0.y) * .18f, midY)
        for (step in 0..28) {
            if (segment > 0 && step == 0) continue
            val t = step / 28f
            val u = 1f - t
            val point = androidx.compose.ui.geometry.Offset(
                u*u*u*p0.x + 3*u*u*t*c1.x + 3*u*t*t*c2.x + t*t*t*p1.x,
                u*u*u*p0.y + 3*u*u*t*c1.y + 3*u*t*t*c2.y + t*t*t*p1.y
            )
            if (previous != null) total += (point - previous).getDistance()
            result += JourneySample(point, total, if (step == 28) segment + 1 else -1)
            previous = point
        }
    }
    return result
}

private fun routePointAt(route: List<JourneySample>, distance: Float): androidx.compose.ui.geometry.Offset {
    if (route.isEmpty()) return androidx.compose.ui.geometry.Offset.Zero
    val index = route.binarySearch { it.distance.compareTo(distance) }.let { if (it >= 0) it else (-it - 1).coerceIn(0, route.lastIndex) }
    if (index == 0) return route.first().point
    val a = route[index - 1]
    val b = route[index]
    val f = ((distance - a.distance) / (b.distance - a.distance).coerceAtLeast(.001f)).coerceIn(0f, 1f)
    return androidx.compose.ui.geometry.Offset(
        a.point.x + (b.point.x - a.point.x) * f,
        a.point.y + (b.point.y - a.point.y) * f
    )
}

@Composable
private fun JourneyDoorGraphic(open: Boolean, onOpen: () -> Unit, compactHeight: androidx.compose.ui.unit.Dp = 112.dp) {
    val angleState = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (open) 68f else 0f,
        animationSpec = androidx.compose.animation.core.tween(850, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "doorLeaves"
    )
    JourneyDoorGraphic(openProgress = angleState.value / 68f, onOpen = onOpen, compactHeight = compactHeight)
}

@Composable
private fun JourneyDoorGraphic(openProgress: Float, onOpen: () -> Unit, compactHeight: androidx.compose.ui.unit.Dp = 112.dp) {
    Row(
        Modifier.widthIn(min = 70.dp, max = 92.dp).height(compactHeight).clickable(onClick = onOpen),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(Modifier.weight(1f).fillMaxHeight().graphicsLayer {
            rotationY = -68f * openProgress
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, .5f)
            cameraDistance = 18 * density
        }) {
            DoorLeaf()
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).size(5.dp).background(Color(0xFFE0C18A), RoundedCornerShape(50)))
        }
        Box(Modifier.weight(1f).fillMaxHeight().graphicsLayer {
            rotationY = 68f * openProgress
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, .5f)
            cameraDistance = 18 * density
        }) {
            DoorLeaf()
            Box(Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(5.dp).background(Color(0xFFE0C18A), RoundedCornerShape(50)))
        }
    }
}

@Composable
private fun DoorLeaf() {
    Box(
        Modifier.fillMaxSize()
            .background(Color(0xFF956B45), RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
            .border(2.dp, Color(0xFF68472F), RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(7.dp)) {
            drawRoundRect(Color(0xFFB58A5D), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()))
            drawLine(Color(0xFF765237), androidx.compose.ui.geometry.Offset(size.width * .5f, 8.dp.toPx()), androidx.compose.ui.geometry.Offset(size.width * .5f, size.height - 8.dp.toPx()), 1.dp.toPx())
        }
    }
}

@Composable
private fun PrayerDoorGraphic(openProgress: Float, onOpen: () -> Unit, compact: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().height(if (compact) 52.dp else 142.dp).clickable(onClick = onOpen),
        horizontalArrangement = Arrangement.Center
    ) {
        listOf(false, true).forEach { right ->
            Box(Modifier.weight(1f).fillMaxHeight().graphicsLayer {
                rotationY = if (right) 72f * openProgress else -72f * openProgress
                transformOrigin = if (right) androidx.compose.ui.graphics.TransformOrigin(1f, .5f) else androidx.compose.ui.graphics.TransformOrigin(0f, .5f)
                cameraDistance = 18 * density
            }.background(Color(0xFF205541), RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp, bottomStart = 4.dp, bottomEnd = 4.dp)).border(2.dp, Color(0xFFD8C49C), RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp, bottomStart = 4.dp, bottomEnd = 4.dp))) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                    drawRoundRect(Color(0xFF9DB6A2), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()))
                }
                Box(Modifier.align(if (right) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 5.dp).size(6.dp).background(Color(0xFFE0C18A), RoundedCornerShape(50)))
            }
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
        "الصلاة" -> listOf("الصلاة المفروضة" to "الصلاة", "النوافل" to "النوافل", "قيام الليل" to "قيام الليل", "القضاء" to "القضاء"),
        "النوافل" -> listOf("تسجيل النوافل" to "النوافل", "سنة الفجر" to "نافلة_الفجر", "سنة الظهر" to "نافلة_الظهر", "سنة العصر" to "نافلة_العصر", "سنة المغرب" to "نافلة_المغرب", "سنة العشاء" to "نافلة_العشاء", "صلاة الضحى" to "نافلة_الضحى"),
        "قيام الليل" -> listOf("سنن قيام الليل" to "قيام_السنن", "جلسة قيام الليل" to "قيام_الجلسة"),
        "فرض" -> listOf("تسجيل الصلاة" to "الصلاة", "سنة الفجر" to "فرض_الفجر", "سنة الظهر" to "فرض_الظهر", "سنة العصر" to "فرض_العصر", "سنة المغرب" to "فرض_المغرب", "سنة العشاء" to "فرض_العشاء"),
        "القضاء" -> listOf("الحاسبة" to "القضاء_الحاسبة", "القضاء والمتابعة" to "القضاء")
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
    if (section == "الصلاة") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = ShellGreen), shape = RoundedCornerShape(18.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("الصلاة جامعة", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("﴿إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَوْقُوتًا﴾", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("البقرة: 45", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall)
                }
            }
            options.chunked(2).forEach { rowOptions ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowOptions.forEach { (label, destination) ->
                        val active = selected == label || (selected.isBlank() && options.firstOrNull()?.first == label)
                        Surface(
                            modifier = Modifier.weight(1f).heightIn(min = 64.dp).clickable { onSelect(label, destination) },
                            color = if (active) ShellGreen else Color(0xFFEAE5D9),
                            shape = RoundedCornerShape(28.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (active) ShellGreen else ShellLine)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp)) {
                                Text(label, color = if (active) Color.White else ShellGreen, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    } else {
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
