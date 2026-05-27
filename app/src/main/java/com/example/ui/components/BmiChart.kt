package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import com.example.data.database.WeightLog
import com.example.ui.theme.CreamAccentGold
import com.example.ui.theme.CreamSecondary
import java.text.SimpleDateFormat
import java.util.*

enum class ChartPeriod {
    WEEK, MONTH, YEAR
}

data class DrawnPoint(
    val offset: Offset,
    val log: WeightLog
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun BmiChart(
    logs: List<WeightLog>,
    targetWeight: Double,
    isMetric: Boolean,
    language: String,
    weekStartDay: Int, // 1 = Sunday, 2 = Monday
    modifier: Modifier = Modifier,
    onLogDoubleClicked: ((WeightLog) -> Unit)? = null
) {
    var selectedPeriodStr by rememberSaveable { mutableStateOf(ChartPeriod.WEEK.name) }
    val selectedPeriod = remember(selectedPeriodStr) { ChartPeriod.valueOf(selectedPeriodStr) }
    var viewingDate by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }

    val t = { key: String -> com.example.ui.components.Localization.get(key, language) }
    val isEn = language.startsWith("en", ignoreCase = true)
    val density = LocalDensity.current

    // Convert logs' weights inside chart context
    val displayLogs = remember(logs, isMetric) {
        logs.map { log ->
            val displayWeight = if (isMetric) log.weightKg else log.weightKg * 2.20462
            log.copy(weightKg = displayWeight)
        }
    }
    // Convert target weight
    val displayTarget = if (isMetric) targetWeight else targetWeight * 2.20462
    val weightUnit = if (isMetric) "kg" else "lb"

    // 1. Calculate historical range boundaries
    val (startBound, endBound) = remember(viewingDate, selectedPeriod, weekStartDay) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = viewingDate

        // Normalize time to start of day
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        when (selectedPeriod) {
            ChartPeriod.WEEK -> {
                val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday=1, Monday=2
                val diff = if (currentDayOfWeek >= weekStartDay) {
                    currentDayOfWeek - weekStartDay
                } else {
                    7 - (weekStartDay - currentDayOfWeek)
                }
                cal.add(Calendar.DAY_OF_YEAR, -diff)
                val start = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            ChartPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            ChartPeriod.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
        }
    }

    // Filter logs inside current active range
    val filteredLogs = remember(displayLogs, startBound, endBound) {
        displayLogs.filter { it.timestamp in startBound..endBound }.sortedBy { it.timestamp }
    }

    // Keep track of point locations to support double-click callback detection
    val drawnPoints = remember { mutableListOf<DrawnPoint>() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // A. Segmented Period Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChartPeriod.values().forEach { period ->
                    val label = when (period) {
                        ChartPeriod.WEEK -> t("chart_week")
                        ChartPeriod.MONTH -> t("chart_month")
                        ChartPeriod.YEAR -> t("chart_year")
                    }
                    val isSelected = selectedPeriod == period
                    Button(
                        onClick = { selectedPeriodStr = period.name },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Text(text = label, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // B. Range Switch Navigation Panel (Time Travel Row)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                // Arrow Left (Previous Range)
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = viewingDate
                        when (selectedPeriod) {
                            ChartPeriod.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
                            ChartPeriod.MONTH -> cal.add(Calendar.MONTH, -1)
                            ChartPeriod.YEAR -> cal.add(Calendar.YEAR, -1)
                        }
                        viewingDate = cal.timeInMillis
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous Range",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Range Label (Middle Text Label)
                val periodLabel = remember(startBound, endBound, selectedPeriod, language) {
                    val startCal = Calendar.getInstance().apply { timeInMillis = startBound }
                    val endCal = Calendar.getInstance().apply { timeInMillis = endBound }

                    val dfMd = SimpleDateFormat("MM/dd", Locale.getDefault())
                    val dfYm = SimpleDateFormat("yyyy/MM", Locale.getDefault())
                    val dfY = SimpleDateFormat("yyyy", Locale.getDefault())

                    when (selectedPeriod) {
                        ChartPeriod.WEEK -> {
                            val weekOfYear = startCal.get(Calendar.WEEK_OF_YEAR)
                            val startStr = dfMd.format(startCal.time)
                            val endStr = dfMd.format(endCal.time)
                            if (isEn) "Week $weekOfYear: $startStr - $endStr" else "第${weekOfYear}週 ${startStr}到${endStr}"
                        }
                        ChartPeriod.MONTH -> {
                            dfYm.format(startCal.time)
                        }
                        ChartPeriod.YEAR -> {
                            dfY.format(startCal.time)
                        }
                    }
                }

                Text(
                    text = periodLabel,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                // Arrow Right (Next Range)
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = viewingDate
                        when (selectedPeriod) {
                            ChartPeriod.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                            ChartPeriod.MONTH -> cal.add(Calendar.MONTH, 1)
                            ChartPeriod.YEAR -> cal.add(Calendar.YEAR, 1)
                        }
                        viewingDate = cal.timeInMillis
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next Range",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Return to current Period Button
                Button(
                    onClick = { viewingDate = System.currentTimeMillis() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (isEn) "Today" else "返回當前",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredLogs.isEmpty() && selectedPeriod != ChartPeriod.WEEK && selectedPeriod != ChartPeriod.MONTH) {
                // Return empty state inside chart for YEAR if empty
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = t("unrecorded"),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = t("chart_empty_title"),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = t("chart_empty_desc"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Render custom chart Canvas
                val primaryColor = MaterialTheme.colorScheme.primary
                val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                val targetLineColor = CreamAccentGold
                val textColor = MaterialTheme.colorScheme.onSurface
                val textMeasurer = rememberTextMeasurer()

                // Calculate weights for Y-scaling
                // For Year, we compute weekly averages first to determine limits
                val yearCal = Calendar.getInstance()
                val weekAveragesList = remember(filteredLogs, selectedPeriod) {
                    if (selectedPeriod != ChartPeriod.YEAR) emptyList() else {
                        val weekSum = DoubleArray(54)
                        val weekCount = IntArray(54)
                        filteredLogs.forEach { log ->
                            yearCal.timeInMillis = log.timestamp
                            val wk = yearCal.get(Calendar.WEEK_OF_YEAR)
                            if (wk in 1..53) {
                                weekSum[wk] += log.weightKg
                                weekCount[wk] += 1
                            }
                        }
                        val avgs = mutableListOf<Double>()
                        for (w in 1..53) {
                            if (weekCount[w] > 0) avgs.add(weekSum[w] / weekCount[w])
                        }
                        avgs
                    }
                }

                val scale = remember(filteredLogs, selectedPeriod, weekAveragesList, displayTarget) {
                    val weights = when (selectedPeriod) {
                        ChartPeriod.WEEK, ChartPeriod.MONTH -> filteredLogs.map { it.weightKg }
                        ChartPeriod.YEAR -> weekAveragesList
                    }
                    val maxW = (weights + displayTarget).maxOrNull() ?: 100.0
                    val minW = (weights + displayTarget).minOrNull() ?: 50.0

                    val rangeBuffer = if (maxW == minW) 10.0 else (maxW - minW) * 0.15
                    val minYVal = maxOf(0.0, minW - rangeBuffer)
                    val maxYVal = maxW + rangeBuffer
                    val range = if (maxYVal == minYVal) 1.0 else (maxYVal - minYVal)
                    Triple(minYVal, maxYVal, range)
                }
                val minY = scale.first
                val maxY = scale.second
                val valueRange = scale.third

                // Define gesture modifier
                val hitModifier = if (selectedPeriod == ChartPeriod.YEAR || onLogDoubleClicked == null) {
                    Modifier
                } else {
                    Modifier.pointerInput(selectedPeriod, logs) {
                        detectTapGestures(
                            onDoubleTap = { pressOffset ->
                                val thresholdPx = with(density) { 28.dp.toPx() }
                                val hit = drawnPoints.minByOrNull { point ->
                                    val dx = point.offset.x - pressOffset.x
                                    val dy = point.offset.y - pressOffset.y
                                    dx * dx + dy * dy
                                }
                                if (hit != null) {
                                    val dist = Math.sqrt(((hit.offset.x - pressOffset.x) * (hit.offset.x - pressOffset.x) + (hit.offset.y - pressOffset.y) * (hit.offset.y - pressOffset.y)).toDouble())
                                    if (dist <= thresholdPx) {
                                        // Find corresponding original WeightLog in original logs list
                                        // Translate copy weight back to original
                                        val originalLog = logs.find { it.timestamp == hit.log.timestamp }
                                        if (originalLog != null) {
                                            onLogDoubleClicked(originalLog)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp)
                        .then(hitModifier)
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingLeft = 75f
                    val paddingRight = 30f
                    val paddingTop = 40f
                    val paddingBottom = 55f

                    val graphWidth = width - paddingLeft - paddingRight
                    val graphHeight = height - paddingTop - paddingBottom

                    // Clear drawn points helper map inside canvas calculation
                    drawnPoints.clear()

                    fun getY(weight: Double): Float {
                        val ratio = (weight - minY) / valueRange
                        return (paddingTop + graphHeight - (ratio * graphHeight)).toFloat()
                    }

                    // 1. Draw 3 horizontal helper grid lines
                    val gridLinesCount = 3
                    for (i in 0..gridLinesCount) {
                        val gridValue = minY + (valueRange * i / gridLinesCount)
                        val gridY = getY(gridValue)

                        drawLine(
                            color = gridColor,
                            start = Offset(paddingLeft, gridY),
                            end = Offset(width - paddingRight, gridY),
                            strokeWidth = 2f
                        )

                        val textString = String.format(Locale.getDefault(), "%.1f", gridValue) + weightUnit
                        drawText(
                            textMeasurer = textMeasurer,
                            text = textString,
                            topLeft = Offset(5f, gridY - 18f),
                            style = TextStyle(
                                color = textColor.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        )
                    }

                    // 2. Draw Dash Target weight Line
                    if (displayTarget > 0) {
                        val targetY = getY(displayTarget)
                        drawLine(
                            color = targetLineColor,
                            start = Offset(paddingLeft, targetY),
                            end = Offset(width - paddingRight, targetY),
                            strokeWidth = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "${t("chart_target")} $weightUnit",
                            topLeft = Offset(width - paddingRight - 80f, targetY - 22f),
                            style = TextStyle(
                                color = targetLineColor,
                                fontSize = 9.sp
                            )
                        )
                    }

                    // 3. Draw depending on chart period
                    val textStyleAxis = TextStyle(color = textColor.copy(alpha = 0.5f), fontSize = 10.sp)

                    when (selectedPeriod) {
                        ChartPeriod.WEEK -> {
                            // Week view: Lists Monday to Sunday (or Sunday to Saturday)
                            val dayLabels = if (weekStartDay == 1) { // Sunday start
                                listOf(t("day_sun"), t("day_mon"), t("day_tue"), t("day_wed"), t("day_thu"), t("day_fri"), t("day_sat"))
                            } else { // Monday start
                                listOf(t("day_mon"), t("day_tue"), t("day_wed"), t("day_thu"), t("day_fri"), t("day_sat"), t("day_sun"))
                            }

                            val daysX = FloatArray(7) { i ->
                                paddingLeft + (i.toFloat() / 6) * graphWidth
                            }

                            // Draw X axis day labels at ticks
                            dayLabels.forEachIndexed { i, labelStr ->
                                val x = daysX[i]
                                val textResult = textMeasurer.measure(labelStr, textStyleAxis)
                                val textWidth = textResult.size.width
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = labelStr,
                                    topLeft = Offset(x - textWidth / 2f, paddingTop + graphHeight + 10f),
                                    style = textStyleAxis
                                )
                            }

                            // Plot data points
                            val weekPoints = mutableListOf<Offset>()
                            val weekLogsMapped = mutableListOf<WeightLog>()

                            filteredLogs.forEach { log ->
                                val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                                val dayOfWeek = logCal.get(Calendar.DAY_OF_WEEK)
                                val dayIndex = if (weekStartDay == 1) {
                                    dayOfWeek - 1
                                } else {
                                    if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                                }

                                if (dayIndex in 0..6) {
                                    val x = daysX[dayIndex]
                                    val y = getY(log.weightKg)
                                    val pt = Offset(x, y)
                                    weekPoints.add(pt)
                                    weekLogsMapped.add(log)
                                    drawnPoints.add(DrawnPoint(pt, log))
                                }
                            }

                            // Sort mapping points by horizontal X axis to trace lines properly
                            val sortedIndices = weekPoints.indices.sortedBy { weekPoints[it].x }
                            val sortedPoints = sortedIndices.map { weekPoints[it] }
                            val sortedLogsMapped = sortedIndices.map { weekLogsMapped[it] }

                            if (sortedPoints.isNotEmpty()) {
                                val strokePath = Path().apply {
                                    moveTo(sortedPoints[0].x, sortedPoints[0].y)
                                    for (i in 1 until sortedPoints.size) {
                                        val prev = sortedPoints[i - 1]
                                        val current = sortedPoints[i]
                                        val cpX1 = prev.x + (current.x - prev.x) / 2
                                        val cpY1 = prev.y
                                        val cpX2 = prev.x + (current.x - prev.x) / 2
                                        val cpY2 = current.y
                                        cubicTo(cpX1, cpY1, cpX2, cpY2, current.x, current.y)
                                    }
                                }

                                // Fill gracefully
                                val fillPath = Path().apply {
                                    addPath(strokePath)
                                    lineTo(sortedPoints.last().x, paddingTop + graphHeight)
                                    lineTo(sortedPoints.first().x, paddingTop + graphHeight)
                                    close()
                                }

                                drawPath(path = fillPath, color = primaryColor.copy(alpha = 0.08f))
                                drawPath(path = strokePath, color = primaryColor, style = Stroke(width = 5f))

                                // Draw circles and text on active points
                                sortedPoints.forEachIndexed { idx, point ->
                                    drawCircle(color = primaryColor, radius = 9f, center = point)
                                    drawCircle(color = CreamSecondary, radius = 4f, center = point)

                                    // Display weight metrics directly above the points
                                    val log = sortedLogsMapped[idx]
                                    val valStr = String.format(Locale.getDefault(), "%.1f", log.weightKg)
                                    val measRes = textMeasurer.measure(valStr, TextStyle(color = primaryColor, fontSize = 9.sp))
                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = valStr,
                                        topLeft = Offset(point.x - measRes.size.width / 2f, point.y - 28f),
                                        style = TextStyle(color = primaryColor, fontSize = 9.sp)
                                    )
                                }
                            }
                        }

                        ChartPeriod.MONTH -> {
                            // Month view: Fixed range spacing ticks based on days in active viewing month
                            val testCal = Calendar.getInstance().apply { timeInMillis = startBound }
                            val daysInMonth = testCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                            val tickDays = when (daysInMonth) {
                                28 -> listOf(1, 8, 15, 21, 28)
                                29 -> listOf(1, 8, 15, 22, 29)
                                30 -> listOf(1, 8, 16, 23, 30)
                                else -> listOf(1, 9, 16, 24, 31) // 31 days
                            }

                            val ticksX = FloatArray(5) { i ->
                                paddingLeft + (i.toFloat() / 4) * graphWidth
                            }

                            // Draw X ticks of specific days
                            tickDays.forEachIndexed { i, dayNum ->
                                val labelStr = "$dayNum"
                                val measText = textMeasurer.measure(labelStr, textStyleAxis)
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = labelStr,
                                    topLeft = Offset(ticksX[i] - measText.size.width / 2f, paddingTop + graphHeight + 10f),
                                    style = textStyleAxis
                                )
                            }

                            // Helper function to map days segmentally
                            fun getMonthXCoord(day: Int): Float {
                                if (day <= tickDays.first()) return paddingLeft
                                if (day >= tickDays.last()) return paddingLeft + graphWidth
                                for (i in 0 until tickDays.size - 1) {
                                    val d1 = tickDays[i]
                                    val d2 = tickDays[i + 1]
                                    if (day in d1..d2) {
                                        val x1 = ticksX[i]
                                        val x2 = ticksX[i + 1]
                                        val ratio = (day - d1).toFloat() / (d2 - d1).toFloat()
                                        return x1 + ratio * (x2 - x1)
                                    }
                                }
                                return paddingLeft
                            }

                            // Plot month points
                            val monthPoints = mutableListOf<Offset>()
                            val monthLogsMapped = mutableListOf<WeightLog>()

                            filteredLogs.forEach { log ->
                                val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                                val day = logCal.get(Calendar.DAY_OF_MONTH)
                                val x = getMonthXCoord(day)
                                val y = getY(log.weightKg)
                                val pt = Offset(x, y)
                                monthPoints.add(pt)
                                monthLogsMapped.add(log)
                                drawnPoints.add(DrawnPoint(pt, log))
                            }

                            if (monthPoints.isNotEmpty()) {
                                val strokePath = Path().apply {
                                    moveTo(monthPoints[0].x, monthPoints[0].y)
                                    for (i in 1 until monthPoints.size) {
                                        val prev = monthPoints[i - 1]
                                        val current = monthPoints[i]
                                        val cpX1 = prev.x + (current.x - prev.x) / 2
                                        val cpY1 = prev.y
                                        val cpX2 = prev.x + (current.x - prev.x) / 2
                                        val cpY2 = current.y
                                        cubicTo(cpX1, cpY1, cpX2, cpY2, current.x, current.y)
                                    }
                                }

                                val fillPath = Path().apply {
                                    addPath(strokePath)
                                    lineTo(monthPoints.last().x, paddingTop + graphHeight)
                                    lineTo(monthPoints.first().x, paddingTop + graphHeight)
                                    close()
                                }

                                drawPath(path = fillPath, color = primaryColor.copy(alpha = 0.08f))
                                drawPath(path = strokePath, color = primaryColor, style = Stroke(width = 5f))

                                monthPoints.forEachIndexed { idx, point ->
                                    drawCircle(color = primaryColor, radius = 8f, center = point)
                                    drawCircle(color = CreamSecondary, radius = 3.5f, center = point)

                                    // Display weight metrics directly above month points
                                    val log = monthLogsMapped[idx]
                                    val valStr = String.format(Locale.getDefault(), "%.1f", log.weightKg)
                                    val measRes = textMeasurer.measure(valStr, TextStyle(color = primaryColor, fontSize = 8.5.sp))
                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = valStr,
                                        topLeft = Offset(point.x - measRes.size.width / 2f, point.y - 25f),
                                        style = TextStyle(color = primaryColor, fontSize = 8.5.sp)
                                    )
                                }
                            }
                        }

                        ChartPeriod.YEAR -> {
                            // Year view: Ticks are 1 to 12 months (Jan to Dec)
                            val monthLabels = if (isEn) {
                                listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                            } else {
                                listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
                            }

                            val monthX = FloatArray(12) { i ->
                                paddingLeft + (i.toFloat() / 11) * graphWidth
                            }

                            // Draw X month axis ticks
                            monthLabels.forEachIndexed { i, labelStr ->
                                val textResult = textMeasurer.measure(labelStr, textStyleAxis)
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = labelStr,
                                    topLeft = Offset(monthX[i] - textResult.size.width / 2f, paddingTop + graphHeight + 10f),
                                    style = textStyleAxis
                                )
                            }

                            // Year view: Points are calculated as weekly averages!
                            // "年檢視1至12月每月標在橫軸，資料點取每週平均，不滿一周取有資料的平均"
                            val weekSum = DoubleArray(54)
                            val weekCount = IntArray(54)
                            val weekCentralTimestamp = LongArray(54)

                            filteredLogs.forEach { log ->
                                yearCal.timeInMillis = log.timestamp
                                val wk = yearCal.get(Calendar.WEEK_OF_YEAR)
                                if (wk in 1..53) {
                                    weekSum[wk] += log.weightKg
                                    weekCount[wk] += 1
                                    weekCentralTimestamp[wk] = log.timestamp
                                }
                            }

                            val yearPoints = mutableListOf<Offset>()
                            val yearWeeklyWeightsMapped = mutableListOf<Double>()

                            for (w in 1..53) {
                                if (weekCount[w] > 0) {
                                    val avgWeight = weekSum[w] / weekCount[w]
                                    val timestamp = weekCentralTimestamp[w]

                                    // Compute continuous position relative to Jan 1st - Dec 31st
                                    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                    val month = cal.get(Calendar.MONTH) // 0..11
                                    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                                    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                                    val monthPos = month.toFloat() + (dayOfMonth - 1).toFloat() / maxDays.toFloat()
                                    val x = paddingLeft + (monthPos / 11f) * graphWidth
                                    val y = getY(avgWeight)
                                    yearPoints.add(Offset(x, y))
                                    yearWeeklyWeightsMapped.add(avgWeight)
                                }
                            }

                            if (yearPoints.isNotEmpty()) {
                                val strokePath = Path().apply {
                                    moveTo(yearPoints[0].x, yearPoints[0].y)
                                    for (i in 1 until yearPoints.size) {
                                        val prev = yearPoints[i - 1]
                                        val current = yearPoints[i]
                                        val cpX1 = prev.x + (current.x - prev.x) / 2
                                        val cpY1 = prev.y
                                        val cpX2 = prev.x + (current.x - prev.x) / 2
                                        val cpY2 = current.y
                                        cubicTo(cpX1, cpY1, cpX2, cpY2, current.x, current.y)
                                    }
                                }

                                val fillPath = Path().apply {
                                    addPath(strokePath)
                                    lineTo(yearPoints.last().x, paddingTop + graphHeight)
                                    lineTo(yearPoints.first().x, paddingTop + graphHeight)
                                    close()
                                }

                                drawPath(path = fillPath, color = primaryColor.copy(alpha = 0.08f))
                                drawPath(path = strokePath, color = primaryColor, style = Stroke(width = 5f))

                                yearPoints.forEachIndexed { idx, point ->
                                    drawCircle(color = primaryColor, radius = 7f, center = point)
                                    drawCircle(color = CreamSecondary, radius = 3f, center = point)

                                    // Weight indicators above points
                                    val avgWeight = yearWeeklyWeightsMapped[idx]
                                    val valStr = String.format(Locale.getDefault(), "%.1f", avgWeight)
                                    val measRes = textMeasurer.measure(valStr, TextStyle(color = primaryColor, fontSize = 8.sp))
                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = valStr,
                                        topLeft = Offset(point.x - measRes.size.width / 2f, point.y - 22f),
                                        style = TextStyle(color = primaryColor, fontSize = 8.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
