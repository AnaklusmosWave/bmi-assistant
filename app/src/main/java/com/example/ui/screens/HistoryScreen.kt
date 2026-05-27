package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.UserProfile
import com.example.data.database.WeightLog
import kotlinx.coroutines.launch
import com.example.ui.components.BmiChart
import com.example.ui.theme.*
import com.example.ui.viewmodel.WeightViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: WeightViewModel,
    profile: UserProfile,
    logs: List<WeightLog>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val targetWeight = profile.targetWeightKg
    val isMetric = profile.isMetricUnit
    val t = { key: String -> com.example.ui.components.Localization.get(key, profile.language) }

    var showDeleteConfirmationLog by remember { mutableStateOf<WeightLog?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showOverwriteWarningByLog by remember { mutableStateOf<WeightLog?>(null) }
    var pendingWeight by remember { mutableStateOf(0.0) }
    var pendingNote by remember { mutableStateOf("") }
    var pendingTimestamp by remember { mutableStateOf(0L) }
    var highlightedLogId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(highlightedLogId) {
        if (highlightedLogId != null) {
            kotlinx.coroutines.delay(3500)
            highlightedLogId = null
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Track Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = t("trending_and_stats"),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = t("history_track"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (logs.isNotEmpty()) {
                    Button(
                        onClick = { exportAndShareReport(context, profile, logs) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = t("export_report"),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(t("export_report"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Bmi Chart Container
        item {
            BmiChart(
                logs = logs,
                targetWeight = targetWeight,
                isMetric = isMetric,
                language = profile.language,
                weekStartDay = profile.weekStartDay,
                onLogDoubleClicked = { targetLog ->
                    highlightedLogId = targetLog.id
                    val idx = logs.indexOfFirst { it.id == targetLog.id }
                    if (idx != -1) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(idx + 3)
                        }
                    }
                }
            )
        }

        // Timeline separator
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (profile.language == "en-US") "${t("all_history")} (${logs.size})" else "${t("all_history")} (${logs.size} 筆)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = t("add_record"),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(t("add_record"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Logs items
        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = t("no_history"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = t("no_history_desc"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        } else {
            items(logs) { log ->
                val displayWeight = if (isMetric) log.weightKg else log.weightKg * 2.20462
                val weightUnit = if (isMetric) "kg" else "lb"

                val (bmiStatusLabel, bmiColor) = when {
                    log.bmi < 18.5 -> t("bmi_underweight") to CreamAccentBlue
                    log.bmi >= 18.5 && log.bmi < 24.0 -> t("bmi_normal") to CreamAccentGreen
                    log.bmi >= 24.0 && log.bmi < 27.0 -> t("bmi_overweight") to CreamAccentGold
                    else -> t("bmi_obese") to CreamAccentRed
                }

                val isHighlighted = log.id == highlightedLogId
                val borderStroke = if (isHighlighted) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                }
                val cardBgColor = if (isHighlighted) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    border = borderStroke,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            // Date
                            val langLocale = if (profile.language == "en-US") Locale.ENGLISH else Locale.TRADITIONAL_CHINESE
                            val basePattern = if (profile.dateFormatPattern == "DEFAULT") {
                                if (profile.language == "en-US") "MMM d, yyyy" else "yyyy年M月d日"
                            } else {
                                profile.dateFormatPattern
                            }
                            val datePattern = "$basePattern HH:mm"
                            val logDate = SimpleDateFormat(datePattern, langLocale).format(Date(log.timestamp))
                            Text(
                                text = logDate,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Weight Display
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f", displayWeight) + " $weightUnit",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                               )

                                Spacer(modifier = Modifier.width(12.dp))

                                // BMI status indicator
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bmiColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "BMI ${String.format(Locale.getDefault(), "%.1f", log.bmi)} ($bmiStatusLabel)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = bmiColor
                                    )
                                }
                            }

                            if (log.note.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "📝 " + log.note,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Trash Deletion Button
                        IconButton(onClick = { showDeleteConfirmationLog = log }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = t("delete_record_tooltip"),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Deletion confirmation dialog
    if (showDeleteConfirmationLog != null) {
        val logToDelete = showDeleteConfirmationLog!!
        val isMetricType = profile.isMetricUnit
        val displayWeight = if (isMetricType) logToDelete.weightKg else logToDelete.weightKg * 2.20462
        val weightUnitStr = if (isMetricType) "kg" else "lb"

        AlertDialog(
            onDismissRequest = { showDeleteConfirmationLog = null },
            title = { Text(t("delete_confirm_title")) },
            text = {
                val formattedTime = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(logToDelete.timestamp))
                val weightValStr = String.format(Locale.getDefault(), "%.1f", displayWeight)
                val detailDesc = if (profile.language == "en-US") {
                    String.format(t("delete_confirm_detail"), weightValStr, weightUnitStr, formattedTime)
                } else {
                    String.format(t("delete_confirm_detail"), formattedTime, weightValStr, weightUnitStr)
                }
                Text(detailDesc)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLog(logToDelete)
                        showDeleteConfirmationLog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CreamAccentRed)
                ) {
                    Text(t("delete_confirm_btn"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationLog = null }) {
                    Text(t("cancel"), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // Add Record dialog
    if (showAddDialog) {
        HistoryAddWeightDialog(
            profile = profile,
            logs = logs,
            onDismiss = { showAddDialog = false },
            onSaveRequested = { weight, note, timestamp, duplicateLog ->
                if (duplicateLog != null) {
                    pendingWeight = weight
                    pendingNote = note
                    pendingTimestamp = timestamp
                    showOverwriteWarningByLog = duplicateLog
                } else {
                    viewModel.logWeight(weight, note, timestamp)
                    showAddDialog = false
                }
            }
        )
    }

    // Overwrite Dialog warning confirmation
    if (showOverwriteWarningByLog != null) {
        val dupLog = showOverwriteWarningByLog!!
        AlertDialog(
            onDismissRequest = { showOverwriteWarningByLog = null },
            title = { Text(t("duplicate_record_title")) },
            text = { Text(t("duplicate_record_desc")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLog(dupLog)
                        viewModel.logWeight(pendingWeight, pendingNote, pendingTimestamp)
                        showOverwriteWarningByLog = null
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(t("overwrite_btn"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverwriteWarningByLog = null }) {
                    Text(t("cancel"), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryAddWeightDialog(
    profile: UserProfile,
    logs: List<WeightLog>,
    onDismiss: () -> Unit,
    onSaveRequested: (Double, String, Long, WeightLog?) -> Unit
) {
    val isMetric = profile.isMetricUnit
    val weightUnit = if (isMetric) "kg" else "lb"
    val t = { key: String -> com.example.ui.components.Localization.get(key, profile.language) }
    val context = LocalContext.current

    val baseWeightInKg = logs.firstOrNull()?.weightKg ?: profile.targetWeightKg
    val baseWeightDisplay = if (isMetric) baseWeightInKg else baseWeightInKg * 2.20462

    var weightInput by remember { mutableStateOf(String.format(Locale.getDefault(), "%.1f", baseWeightDisplay)) }
    var noteInput by remember { mutableStateOf("") }
    var dateInMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = t("add_record"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Weight Input Field
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = {
                        weightInput = it
                        errorText = null
                    },
                    label = { Text("${t("dialog_weight_label")} ($weightUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorText != null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(text = errorText!!, color = CreamAccentRed, fontSize = 11.sp)
                }

                // Quick buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-0.5, -0.1, 0.1, 0.5).forEach { offset ->
                        val sign = if (offset > 0) "+" else ""
                        TextButton(
                            onClick = {
                                val curVal = weightInput.toDoubleOrNull() ?: baseWeightDisplay
                                weightInput = String.format(Locale.getDefault(), "%.1f", curVal + offset)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("$sign$offset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Note field
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text(t("weight_note_field")) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Date selection field triggered by clicking a button
                OutlinedCard(
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = dateInMillis }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val updatedCal = Calendar.getInstance().apply {
                                    timeInMillis = dateInMillis
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    set(Calendar.HOUR_OF_DAY, 12)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                }
                                dateInMillis = updatedCal.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "選擇日期",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val formattedDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(dateInMillis))
                            Text(
                                text = if (profile.language == "en-US") "Date: $formattedDate" else "量測日期: $formattedDate",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(t("cancel"), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsedWeight = weightInput.toDoubleOrNull()
                            if (parsedWeight == null || parsedWeight <= 0) {
                                errorText = t("please_enter_weight")
                                return@Button
                            }

                            val weightInKgToSave = if (isMetric) parsedWeight else parsedWeight / 2.20462

                            // Check duplicate entries on the same day
                            val cal1 = Calendar.getInstance().apply { timeInMillis = dateInMillis }
                            val cal2 = Calendar.getInstance()
                            val duplicateLog = logs.find { log2 ->
                                cal2.timeInMillis = log2.timestamp
                                cal2.get(Calendar.YEAR) == cal1.get(Calendar.YEAR) &&
                                cal2.get(Calendar.MONTH) == cal1.get(Calendar.MONTH) &&
                                cal2.get(Calendar.DAY_OF_MONTH) == cal1.get(Calendar.DAY_OF_MONTH)
                            }

                            onSaveRequested(weightInKgToSave, noteInput, dateInMillis, duplicateLog)
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(t("dialog_save_btn"))
                    }
                }
            }
        }
    }
}

// Share Intent Report Exporter
fun exportAndShareReport(context: Context, profile: UserProfile, logs: List<WeightLog>) {
    val isMetric = profile.isMetricUnit
    val weightUnit = if (isMetric) "kg" else "lb"
    val heightUnit = if (isMetric) "cm" else (if (profile.language == "en-US") "in" else "吋")

    val displayHeight = if (isMetric) profile.heightCm else profile.heightCm / 2.54
    val displayTarget = if (isMetric) profile.targetWeightKg else profile.targetWeightKg * 2.20462

    val latestLog = logs.firstOrNull()
    val currentWeightDisplay = if (latestLog != null) {
        if (isMetric) latestLog.weightKg else latestLog.weightKg * 2.20462
    } else 0.0

    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    val isEn = profile.language == "en-US"
    val reportText = StringBuilder().apply {
        if (isEn) {
            append("✦ BMI Assistant Health Report ✦\n")
            append("Export Date: ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())}\n")
            append("═══════════════════════════\n")
            append("[Key Health Metrics]\n")
            append("- Current Height: ${String.format(Locale.getDefault(), "%.1f", displayHeight)} $heightUnit\n")
            append("- Target Weight: ${String.format(Locale.getDefault(), "%.1f", displayTarget)} $weightUnit\n")
            if (latestLog != null) {
                append("- Latest Weight: ${String.format(Locale.getDefault(), "%.1f", currentWeightDisplay)} $weightUnit\n")
                append("- Latest BMI: ${String.format(Locale.getDefault(), "%.1f", latestLog.bmi)}\n")
            }
            append("═══════════════════════════\n")
            append("[Historical Track Logs]\n")
            logs.take(30).forEach { log ->
                val w = if (isMetric) log.weightKg else log.weightKg * 2.20462
                val dateStr = dateFormat.format(Date(log.timestamp))
                val noteStr = if (log.note.isNotEmpty()) " (Notes: ${log.note})" else ""
                append("- $dateStr -> ${String.format(Locale.getDefault(), "%.1f", w)} $weightUnit | BMI: ${String.format(Locale.getDefault(), "%.1f", log.bmi)}$noteStr\n")
            }
            if (logs.size > 30) {
                append("- (Only showing recent 30 records)\n")
            }
            append("═══════════════════════════\n")
            append("Health Tip: Regular weight tracking helps support dynamic metabolic awareness. Sleep well and eat clean-rich dietary fiber!\n")
            append("Report generated by BMI Assistant.")
        } else {
            append("✦ BMI 助理體重健康報告 ✦\n")
            append("產出日期: ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())}\n")
            append("═══════════════════════════\n")
            append("【基本健康數據指標】\n")
            append("- 目前身高: ${String.format(Locale.getDefault(), "%.1f", displayHeight)} $heightUnit\n")
            append("- 目標體重: ${String.format(Locale.getDefault(), "%.1f", displayTarget)} $weightUnit\n")
            if (latestLog != null) {
                append("- 最新體重: ${String.format(Locale.getDefault(), "%.1f", currentWeightDisplay)} $weightUnit\n")
                append("- 最新 BMI: ${String.format(Locale.getDefault(), "%.1f", latestLog.bmi)}\n")
            }
            append("═══════════════════════════\n")
            append("【歷史量測軌跡】\n")
            logs.take(30).forEach { log ->
                val w = if (isMetric) log.weightKg else log.weightKg * 2.20462
                val dateStr = dateFormat.format(Date(log.timestamp))
                val noteStr = if (log.note.isNotEmpty()) " (備註: ${log.note})" else ""
                append("- $dateStr -> ${String.format(Locale.getDefault(), "%.1f", w)} $weightUnit | BMI: ${String.format(Locale.getDefault(), "%.1f", log.bmi)}$noteStr\n")
            }
            if (logs.size > 30) {
                append("- (系統僅列出最近 30 筆紀錄)\n")
            }
            append("═══════════════════════════\n")
            append("健康提醒：定時定分記錄體重有助於掌控新陳代謝狀態，配合睡眠與膳食纖維更棒唷！\n")
            append("報告由《BMI 助理》生成。")
        }
    }.toString()

    val shareSubject = if (isEn) "BMI Assistant Health Report" else "BMI 助理體重健康報告"
    val chooserTitle = if (isEn) "Share health report to..." else "分享您的體重報告到..."

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, shareSubject)
        putExtra(Intent.EXTRA_TEXT, reportText)
    }

    context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
}
