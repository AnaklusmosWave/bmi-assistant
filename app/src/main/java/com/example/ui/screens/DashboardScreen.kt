package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.UserProfile
import com.example.data.database.WeightLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiSuggestionState
import com.example.ui.viewmodel.WeightViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    viewModel: WeightViewModel,
    profile: UserProfile,
    logs: List<WeightLog>,
    onNavigateToHistory: () -> Unit
) {
    val aiState by viewModel.aiSuggestionState
    val latestLog = logs.firstOrNull()
    val isMetric = profile.isMetricUnit
    val t = { key: String -> com.example.ui.components.Localization.get(key, profile.language) }

    var showLogDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Welcome Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = t("welcome_back"),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = t("today_health_status"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Quick Add button
                Button(
                    onClick = { showLogDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = t("add_record"),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = t("add_record"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Circular Gauge Card
        item {
            BmiGaugeCard(latestLog = latestLog, profile = profile)
        }

        // Weight Goal Tracker Progress Bar
        item {
            GoalTrackerCard(latestLog = latestLog, profile = profile)
        }
    }

    // Modal dialogue for custom log inputs (with smart pre-filling!)
    if (showLogDialog) {
        LogWeightDialog(
            latestLog = latestLog,
            profile = profile,
            onDismiss = { showLogDialog = false },
            onSave = { weight, note, timestamp ->
                viewModel.logWeight(weight, note, timestamp)
                showLogDialog = false
            }
        )
    }
}

@Composable
fun BmiGaugeCard(latestLog: WeightLog?, profile: UserProfile) {
    val isMetric = profile.isMetricUnit
    val weightUnit = if (isMetric) "kg" else "lb"
    val heightUnit = if (isMetric) "cm" else "in"
    val t = { key: String -> com.example.ui.components.Localization.get(key, profile.language) }

    val currentWeightDisplay = if (latestLog != null) {
        if (isMetric) latestLog.weightKg else latestLog.weightKg * 2.20462
    } else 0.0

    val currentBmi = latestLog?.bmi ?: 0.0

    // Determine status text & colors based on Taiwan Health promotion guidelines
    val (statusLabel, statusColor) = when {
        currentBmi == 0.0 -> t("unrecorded") to MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        currentBmi < 18.5 -> t("bmi_underweight") to CreamAccentBlue
        currentBmi >= 18.5 && currentBmi < 24.0 -> t("bmi_normal") to CreamAccentGreen
        currentBmi >= 24.0 && currentBmi < 27.0 -> t("bmi_overweight") to CreamAccentGold
        else -> t("bmi_obese") to CreamAccentRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = t("body_status_gauge"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Large Circular Gauge mockup in Compose
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .border(
                        width = 8.dp,
                        color = statusColor.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .padding(8.dp)
                    .border(
                        width = 4.dp,
                        color = statusColor,
                        shape = CircleShape
                    )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (latestLog == null) {
                        Text(
                            text = t("unrecorded"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", currentWeightDisplay),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = weightUnit,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "BMI ${String.format(Locale.getDefault(), "%.1f", currentBmi)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status chip badge
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detail Row: Height info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val heightDisplay = if (isMetric) profile.heightCm else profile.heightCm / 2.54
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = t("current_height"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", heightDisplay)} $heightUnit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = t("update_time"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    val formattedDate = latestLog?.let {
                        val langLocale = if (profile.language == "en-US") Locale.ENGLISH else Locale.TRADITIONAL_CHINESE
                        val basePattern = if (profile.dateFormatPattern == "DEFAULT") {
                            if (profile.language == "en-US") "MMM d" else "M/dd"
                        } else {
                            profile.dateFormatPattern.replace("/yyyy", "").replace("yyyy/", "").replace("yyyy年", "")
                        }
                        val pattern = "$basePattern HH:mm"
                        SimpleDateFormat(pattern, langLocale).format(Date(it.timestamp))
                    } ?: "--/--"
                    Text(
                        text = formattedDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun GoalTrackerCard(latestLog: WeightLog?, profile: UserProfile) {
    val isMetric = profile.isMetricUnit
    val weightUnit = if (isMetric) "kg" else "lb"
    val t = { key: String -> com.example.ui.components.Localization.get(key, profile.language) }

    val currentWeight = latestLog?.weightKg ?: 0.0
    val targetWeight = profile.targetWeightKg

    val displayWeight = if (isMetric) currentWeight else currentWeight * 2.20462
    val displayTarget = if (isMetric) targetWeight else targetWeight * 2.20462

    val difference = if (latestLog != null) displayWeight - displayTarget else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = t("health_goal_progress"),
                        tint = CreamAccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = t("health_goal_progress"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${t("target_weight_label")}: ${String.format(Locale.getDefault(), "%.1f", displayTarget)} $weightUnit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (latestLog == null) {
                Text(
                    text = if (profile.language == "en-US") "Set your weight target and log to begin your amazing journey!" else "設定目標並開始記錄，分析今日體重與奮鬥方向！",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                // Display custom motivating subtitle
                val motivationText = when {
                    difference > 0 -> {
                        val diffStr = String.format(Locale.getDefault(), "%.1f", difference)
                        String.format(t("difference_above_target"), diffStr, weightUnit)
                    }
                    difference < 0 -> {
                        val diffStr = String.format(Locale.getDefault(), "%.1f", -difference)
                        String.format(t("difference_below_target"), diffStr, weightUnit)
                    }
                    else -> t("difference_equal_target")
                }

                Text(
                    text = HtmlTextParser(motivationText),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Simulated progress bar: normalize base progress starting from 100kg down, or standard ratio
                // Let's assume progress is (currentWeight / targetWeight) ratio
                // Ideally, progress bar fills closest to target. Let's do a beautiful bar.
                val progress = remember(currentWeight, targetWeight) {
                    val maxPossibleDiff = 20.0
                    val currentDiff = Math.abs(difference)
                    if (currentDiff >= maxPossibleDiff) 0.1f
                    else ((maxPossibleDiff - currentDiff) / maxPossibleDiff).toFloat().coerceIn(0.1f, 1.0f)
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = CreamAccentGold,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// Custom simple parser to italicize or bold items with markdown double asterisks in Text
@Composable
fun HtmlTextParser(text: String): String {
    // Basic clean-up for the model representation
    return text.replace("**", "")
}

@Composable
fun FastLogShortcutBar(
    latestLog: WeightLog?,
    profile: UserProfile,
    onLogInstant: (Double) -> Unit,
    onManualLogClicked: () -> Unit
) {
    val isMetric = profile.isMetricUnit
    val weightUnit = if (isMetric) "kg" else "lb"
    val t = { key: String -> com.example.ui.components.Localization.get(key, profile.language) }

    // Automated default value logic: grab previous weight, or default to 65.0 or raw target weight
    val baseWeightInKg = latestLog?.weightKg ?: profile.targetWeightKg

    val displayBaseWeight = if (isMetric) baseWeightInKg else baseWeightInKg * 2.20462

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = t("one_sec_shortcut"),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val baseWStr = String.format(Locale.getDefault(), "%.1f", displayBaseWeight)
            Text(
                text = String.format(t("shortcut_desc"), baseWStr, weightUnit),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Faster adjustment increments
                listOf(-0.5, -0.1, 0.1, 0.5).forEach { offset ->
                    val sign = if (offset > 0) "+" else ""
                    val targetWeight = displayBaseWeight + offset
                    val weightInKgToLog = if (isMetric) targetWeight else targetWeight / 2.20462

                    Button(
                        onClick = { onLogInstant(weightInKgToLog) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text(
                            text = "$sign$offset",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pre-fill log equal to base
            Button(
                onClick = { 
                    val baseKg = if (isMetric) displayBaseWeight else displayBaseWeight / 2.20462
                    onLogInstant(baseKg)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary)
            ) {
                val yesterStr = String.format(Locale.getDefault(), "%.1f", displayBaseWeight)
                Text(
                    text = String.format(t("same_as_yesterday"), yesterStr, weightUnit),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AiCoachCard(
    aiState: AiSuggestionState,
    profile: UserProfile,
    onTriggerAi: () -> Unit
) {
    val t = { key: String -> com.example.ui.components.Localization.get(key, profile.language) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = t("ai_coach_title"),
                        tint = CreamAccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = t("ai_coach_title"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (aiState is AiSuggestionState.Success) {
                    IconButton(onClick = onTriggerAi, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = t("retry"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Crossfade(targetState = aiState) { state ->
                when (state) {
                    is AiSuggestionState.Idle -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = t("ai_coach_tip"),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onTriggerAi,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(t("btn_generate_ai"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    is AiSuggestionState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = t("ai_loading_message"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    is AiSuggestionState.Success -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = state.advice,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is AiSuggestionState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = t("ai_error_prefix") + state.error,
                                fontSize = 13.sp,
                                color = CreamAccentRed
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onTriggerAi, shape = RoundedCornerShape(12.dp)) {
                                Text(t("retry"), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWeightDialog(
    latestLog: WeightLog?,
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (Double, String, Long) -> Unit
) {
    val isMetric = profile.isMetricUnit
    val weightUnit = if (isMetric) "kg" else "lb"
    val t = { key: String -> com.example.ui.components.Localization.get(key, profile.language) }

    // Default pre-fill values based on guidelines
    val baseWeightInKg = latestLog?.weightKg ?: profile.targetWeightKg
    val baseWeightDisplay = if (isMetric) baseWeightInKg else baseWeightInKg * 2.20462

    var weightInput by remember { mutableStateOf(String.format(Locale.getDefault(), "%.1f", baseWeightDisplay)) }
    var noteInput by remember { mutableStateOf("") }
    var dateInMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    var errorText by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

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
                    text = t("dialog_record_title"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val heightUnitStr = if (isMetric) "cm" else "in"
                val heightValStr = String.format(Locale.getDefault(), "%.1f", if (isMetric) profile.heightCm else profile.heightCm / 2.54)
                Text(
                    text = String.format(t("dialog_prefilled_desc"), heightValStr, heightUnitStr),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(text = errorText!!, color = CreamAccentRed, fontSize = 11.sp)
                }

                // Increments panel inside dialog to satisfy tap-reduction
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

                // Note text
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text(t("weight_note_field")) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Dynamic Date label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "日期",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val formattedDateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(dateInMillis))
                    Text(
                        text = String.format(t("dialog_date_label"), formattedDateStr),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

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

                            // Convert weight back to KG if input was in LBS
                            val weightInKgToSave = if (isMetric) parsedWeight else parsedWeight / 2.20462
                            onSave(weightInKgToSave, noteInput, dateInMillis)
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
