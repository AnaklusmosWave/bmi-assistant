package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.UserProfile
import com.example.ui.components.Localization
import com.example.ui.theme.*
import com.example.ui.viewmodel.WeightViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WeightViewModel,
    profile: UserProfile,
    isDevModeUnlockedThisSession: Boolean = false
) {
    val context = LocalContext.current

    // State to track if user actually touched any setting, to prevent side save button from appearing on first screen enter
    var userMadeChanges by remember { mutableStateOf(false) }

    // Local states initialized with DB values of user profile
    var selectedLanguage by remember(profile) { mutableStateOf(profile.language) }
    var weekStartDay by remember(profile) { mutableStateOf(profile.weekStartDay) }
    var dateFormatPattern by remember(profile) { 
        mutableStateOf(if (profile.dateFormatPattern == "DEFAULT") "yyyy/MM/dd" else profile.dateFormatPattern) 
    }
    var itemsPerPage by remember(profile) { mutableStateOf(profile.itemsPerPage) }
    
    // Localization helper
    val t = { key: String -> Localization.get(key, selectedLanguage) }

    var heightInput by remember(profile) { 
        val h = if (profile.isMetricUnit) profile.heightCm else profile.heightCm / 2.54
        mutableStateOf(String.format(Locale.getDefault(), "%.1f", h)) 
    }
    
    var targetWeightInput by remember(profile) { 
        val w = if (profile.isMetricUnit) profile.targetWeightKg else profile.targetWeightKg * 2.20462
        mutableStateOf(String.format(Locale.getDefault(), "%.1f", w)) 
    }

    var isMetric by remember(profile) { mutableStateOf(profile.isMetricUnit) }
    var ageInput by remember(profile) { mutableStateOf(profile.age.toString()) }
    var selectedGender by remember(profile) { mutableStateOf(profile.gender) }
    
    var reminderEnabled by remember(profile) { mutableStateOf(profile.reminderEnabled) }
    var reminderHour by remember(profile) { mutableStateOf(profile.reminderHour) }
    var reminderMinute by remember(profile) { mutableStateOf(profile.reminderMinute) }

    // Parse the legacy/comma-separated days from profile.reminderFrequency
    val initialDays = remember(profile.reminderFrequency) {
        val freq = profile.reminderFrequency
        val set = mutableSetOf<String>()
        when (freq) {
            "每日", "Daily" -> {
                set.addAll(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
            }
            "週一至週五", "Mon to Fri" -> {
                set.addAll(listOf("Mon", "Tue", "Wed", "Thu", "Fri"))
            }
            "每週", "Weekly" -> {
                set.add("Mon")
            }
            else -> {
                if (freq.isNotEmpty()) {
                    freq.split(",").map { it.trim() }.forEach {
                        if (it.isNotEmpty()) set.add(it)
                    }
                }
            }
        }
        if (set.isEmpty()) {
            set.addAll(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) // default fallback
        }
        set
    }

    // 7 checkboxes states (stored in a snapshotStateList)
    val selectedDays = remember(initialDays) { 
        androidx.compose.runtime.mutableStateListOf<String>().apply { 
            addAll(initialDays) 
        } 
    }

    val daysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    
    // Derived current reminderFrequency string
    val currentReminderFrequency = remember(selectedDays.toList()) {
        daysList.filter { selectedDays.contains(it) }.joinToString(",")
    }

    // Developer Mode Switch State
    var isDeveloperMode by remember(profile) { mutableStateOf(profile.isDeveloperMode) }

    // Dropdown selectors states
    var showHourDropdown by remember { mutableStateOf(false) }
    var showMinDropdown by remember { mutableStateOf(false) }

    val heightUnit = if (isMetric) "cm" else t("imperial_opt").substringAfter("(").substringBefore("/").trim()
    val weightUnit = if (isMetric) "kg" else t("imperial_opt").substringAfter("/").substringBefore(")").trim()

    // Determine if setting form has un-saved changed state against db entity
    val hasChanges = remember(
        selectedLanguage, isMetric, ageInput, selectedGender,
        reminderEnabled, reminderHour, reminderMinute, currentReminderFrequency,
        heightInput, targetWeightInput, isDeveloperMode, weekStartDay, dateFormatPattern, itemsPerPage, profile
    ) {
        val originalH = if (profile.isMetricUnit) profile.heightCm else profile.heightCm / 2.54
        val originalW = if (profile.isMetricUnit) profile.targetWeightKg else profile.targetWeightKg * 2.20462
        
        val currentH = heightInput.toDoubleOrNull() ?: 0.0
        val currentW = targetWeightInput.toDoubleOrNull() ?: 0.0

        val hChanged = Math.abs(currentH - originalH) > 0.05
        val wChanged = Math.abs(currentW - originalW) > 0.05

        selectedLanguage != profile.language ||
        isMetric != profile.isMetricUnit ||
        ageInput != profile.age.toString() ||
        selectedGender != profile.gender ||
        reminderEnabled != profile.reminderEnabled ||
        reminderHour != profile.reminderHour ||
        reminderMinute != profile.reminderMinute ||
        currentReminderFrequency != profile.reminderFrequency ||
        isDeveloperMode != profile.isDeveloperMode ||
        weekStartDay != profile.weekStartDay ||
        dateFormatPattern != profile.dateFormatPattern ||
        itemsPerPage != profile.itemsPerPage ||
        hChanged || wChanged
    }

    // Consolidated save settings routine
    val saveSettings = {
        val parsedH = heightInput.toDoubleOrNull()
        val parsedW = targetWeightInput.toDoubleOrNull()
        val parsedAge = ageInput.toIntOrNull()

        if (parsedH == null || parsedH <= 0) {
            Toast.makeText(context, t("enter_valid_height"), Toast.LENGTH_SHORT).show()
        } else if (parsedW == null || parsedW <= 0) {
            Toast.makeText(context, t("enter_valid_target_weight"), Toast.LENGTH_SHORT).show()
        } else if (parsedAge == null || parsedAge <= 0) {
            val ageErr = if (selectedLanguage == "en-US") "Please enter a valid age!" else "請填寫合規真實年齡！"
            Toast.makeText(context, ageErr, Toast.LENGTH_SHORT).show()
        } else {
            // Convert Imperial inputs BACK to Standard KG/CM internally for storage
            val heightInCmToSave = if (isMetric) parsedH else parsedH * 2.54
            val weightInKgToSave = if (isMetric) parsedW else parsedW / 2.20462

            viewModel.updateProfile(
                heightCm = heightInCmToSave,
                targetWeightKg = weightInKgToSave,
                isMetric = isMetric,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                reminderFrequency = currentReminderFrequency,
                language = selectedLanguage,
                age = parsedAge,
                gender = selectedGender,
                isOnboarded = profile.isOnboarded,
                isDeveloperMode = isDeveloperMode,
                weekStartDay = weekStartDay,
                dateFormatPattern = dateFormatPattern,
                itemsPerPage = itemsPerPage
            )

            userMadeChanges = false
            Toast.makeText(context, t("save_success_toast"), Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Settings Header
        item {
            Column {
                Text(
                    text = t("app_title_settings"),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = t("settings"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 1. Language Select setting card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = t("lang_setting_title"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = t("lang_setting_desc"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(4.dp)
                    ) {
                        listOf("zh-TW", "en-US").forEach { langOpt ->
                            val label = if (langOpt == "zh-TW") "繁體中文" else "English (USA)"
                            val isSelected = selectedLanguage == langOpt
                            Button(
                                onClick = { selectedLanguage = langOpt; userMadeChanges = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = if (selectedLanguage == "en-US") 10.5.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Metric / Imperial Unit settings selector card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = t("metric_system"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = t("metric_system_desc"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(4.dp)
                    ) {
                        listOf(true, false).forEach { metricOption ->
                            val label = if (metricOption) t("metric_opt") else t("imperial_opt")
                            val isSelected = isMetric == metricOption
                            Button(
                                onClick = { 
                                    val currentH = heightInput.toDoubleOrNull() ?: profile.heightCm
                                    val currentW = targetWeightInput.toDoubleOrNull() ?: profile.targetWeightKg

                                    if (metricOption && !isMetric) {
                                        // Imperial -> Metric conversion
                                        heightInput = String.format(Locale.getDefault(), "%.1f", currentH * 2.54)
                                        targetWeightInput = String.format(Locale.getDefault(), "%.1f", currentW / 2.20462)
                                    } else if (!metricOption && isMetric) {
                                        // Metric -> Imperial conversion
                                        heightInput = String.format(Locale.getDefault(), "%.1f", currentH / 2.54)
                                        targetWeightInput = String.format(Locale.getDefault(), "%.1f", currentW * 2.20462)
                                    }
                                    isMetric = metricOption 
                                    userMadeChanges = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = if (selectedLanguage == "en-US") 9.5.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Physical Metrics (Height, Target Weight, Age, Gender)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = t("physical_metrics"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Height Input field
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it; userMadeChanges = true },
                        label = { Text("${t("height_label")} ($heightUnit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        ),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Age Input
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it; userMadeChanges = true },
                        label = { Text(t("onboarding_age")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        ),
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Gender Selector Row
                    Column {
                        Text(
                            text = t("onboarding_gender"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(4.dp)
                        ) {
                            listOf("男", "女", "其他").forEach { genderOpt ->
                                val label = when (genderOpt) {
                                    "男" -> t("gender_male")
                                    "女" -> t("gender_female")
                                    else -> t("gender_other")
                                }
                                val isSelected = selectedGender == genderOpt
                                Button(
                                    onClick = { selectedGender = genderOpt; userMadeChanges = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3.5 Goal Settings Card (目標設定)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = t("target_goals_title"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = t("target_goals_desc"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    // Target Weight Input
                    OutlinedTextField(
                        value = targetWeightInput,
                        onValueChange = { targetWeightInput = it; userMadeChanges = true },
                        label = { Text("${t("target_weight_label")} ($weightUnit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        ),
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = CreamAccentGold) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dynamic standard target recommendation (BMI of 22)
                    val parsedCurrentH = heightInput.toDoubleOrNull() ?: profile.heightCm
                    val standardHeightInM = (if (isMetric) parsedCurrentH else parsedCurrentH * 2.54) / 100.0
                    val recommendedStandardKg = 22.0 * (standardHeightInM * standardHeightInM)
                    val recommendedStandardDisplay = if (isMetric) recommendedStandardKg else recommendedStandardKg * 2.20462
                    val recommendedStandardDisplayString = String.format(Locale.getDefault(), "%.1f", recommendedStandardDisplay)

                    // Live target BMI calculation
                    val inputTargetW = targetWeightInput.toDoubleOrNull() ?: 60.0
                    val targetWInKg = if (isMetric) inputTargetW else inputTargetW / 2.22462
                    val targetBmiResult = if (standardHeightInM > 0) targetWInKg / (standardHeightInM * standardHeightInM) else 0.0

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("target_bmi_label"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", targetBmiResult),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (targetBmiResult >= 18.5 && targetBmiResult < 24.0) CreamAccentGreen else CreamAccentGold
                        )
                    }
                }
            }
        }

        // 3.8 Chart View Settings Card (圖表檢視設定)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = t("chart_view_settings"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (selectedLanguage == "en-US") "Configure chart start day of week and regional date presentation." else "調整歷史圖表中的每週首日，以及清單、細節之年月日日期顯示方法。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    // Week start day selector (1 = Sunday, 2 = Monday)
                    Column {
                        Text(
                            text = t("week_start_label"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(4.dp)
                        ) {
                            listOf(2, 1).forEach { dayOpt ->
                                val label = if (dayOpt == 2) t("week_monday") else t("week_sunday")
                                val isSelected = weekStartDay == dayOpt
                                Button(
                                    onClick = { weekStartDay = dayOpt; userMadeChanges = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Date format pattern selector
                    Column {
                        Text(
                            text = t("date_format_label"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(4.dp)
                        ) {
                            listOf("yyyy/MM/dd", "MM/dd/yyyy", "dd/MM/yyyy").forEach { patternOpt ->
                                val label = when (patternOpt) {
                                    "yyyy/MM/dd" -> "Y/M/D"
                                    "MM/dd/yyyy" -> "M/D/Y"
                                    else -> "D/M/Y"
                                }
                                val isSelected = dateFormatPattern == patternOpt
                                Button(
                                    onClick = { dateFormatPattern = patternOpt; userMadeChanges = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = label, 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Items per page selector (歷史紀錄每頁筆數)
                    Column {
                        Text(
                            text = if (selectedLanguage == "en-US") "History Logs Per Page" else "歷史紀錄每頁顯示筆數",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(4.dp)
                        ) {
                            listOf(5, 10, 15, 20).forEach { countOpt ->
                                val label = if (selectedLanguage == "en-US") "$countOpt logs" else "${countOpt} 筆"
                                val isSelected = itemsPerPage == countOpt
                                Button(
                                    onClick = { itemsPerPage = countOpt; userMadeChanges = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = label, 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Daily Reminders Settings (Frequency and Time selectors)
        item {
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("reminder_title"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = t("reminder_desc"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it; userMadeChanges = true },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (reminderEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Hour Selector
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = String.format(Locale.getDefault(), "%02d ${if (selectedLanguage == "en-US") "h" else "點"}", reminderHour),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(t("reminder_hour")) },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showHourDropdown = true },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showHourDropdown = true }
                                )
                                DropdownMenu(
                                    expanded = showHourDropdown,
                                    onDismissRequest = { showHourDropdown = false }
                                ) {
                                    (0..23).forEach { hour ->
                                        DropdownMenuItem(
                                            text = { Text(String.format(Locale.getDefault(), "%02d ${if (selectedLanguage == "en-US") "h" else "點"}", hour), fontSize = 13.sp) },
                                            onClick = {
                                                reminderHour = hour
                                                showHourDropdown = false
                                                userMadeChanges = true
                                            }
                                        )
                                    }
                                }
                            }

                            // Minute Selector
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = String.format(Locale.getDefault(), "%02d ${if (selectedLanguage == "en-US") "m" else "分"}", reminderMinute),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(t("reminder_min")) },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showMinDropdown = true },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showMinDropdown = true }
                                )
                                DropdownMenu(
                                    expanded = showMinDropdown,
                                    onDismissRequest = { showMinDropdown = false }
                                ) {
                                    (0..55 step 5).forEach { minute ->
                                        DropdownMenuItem(
                                            text = { Text(String.format(Locale.getDefault(), "%02d ${if (selectedLanguage == "en-US") "m" else "分"}", minute), fontSize = 13.sp) },
                                            onClick = {
                                                reminderMinute = minute
                                                showMinDropdown = false
                                                userMadeChanges = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 7 checkboxes for selecting the reminder days
                        Text(
                            text = t("reminder_freq"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            daysOfWeek.forEach { day ->
                                val isDaySelected = selectedDays.contains(day)
                                val dayLabel = when (day) {
                                    "Mon" -> t("day_mon")
                                    "Tue" -> t("day_tue")
                                    "Wed" -> t("day_wed")
                                    "Thu" -> t("day_thu")
                                    "Fri" -> t("day_fri")
                                    "Sat" -> t("day_sat")
                                    else -> t("day_sun")
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            userMadeChanges = true
                                            if (isDaySelected) {
                                                if (selectedDays.size > 1) {
                                                    selectedDays.remove(day)
                                                } else {
                                                    Toast.makeText(context, if (selectedLanguage == "en-US") "Please select at least one day!" else "請至少選擇一天！", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                selectedDays.add(day)
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = dayLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Checkbox(
                                        checked = isDaySelected,
                                        onCheckedChange = { checked ->
                                            userMadeChanges = true
                                            if (checked) {
                                                selectedDays.add(day)
                                            } else {
                                                if (selectedDays.size > 1) {
                                                    selectedDays.remove(day)
                                                } else {
                                                    Toast.makeText(context, if (selectedLanguage == "en-US") "Please select at least one day!" else "請至少選擇一天！", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = t("reminder_inactive_desc"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // 4.5 Developer Mode Settings Card
        if (isDevModeUnlockedThisSession) {
            item {
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t("developer_mode_title"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = t("developer_mode_desc"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            Switch(
                                checked = isDeveloperMode,
                                onCheckedChange = { isDeveloperMode = it; userMadeChanges = true },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                    // Onboarding link & Random data button are shown if developer mode is enabled
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isDeveloperMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    // Instantly update database and let the app stream Onboarding Screen
                                    viewModel.updateProfile(
                                        heightCm = if (isMetric) (heightInput.toDoubleOrNull() ?: profile.heightCm) else (heightInput.toDoubleOrNull() ?: (profile.heightCm / 2.54)) * 2.54,
                                        targetWeightKg = if (isMetric) (targetWeightInput.toDoubleOrNull() ?: profile.targetWeightKg) else (targetWeightInput.toDoubleOrNull() ?: (profile.targetWeightKg * 2.20462)) / 2.20462,
                                        isMetric = isMetric,
                                        reminderEnabled = reminderEnabled,
                                        reminderHour = reminderHour,
                                        reminderMinute = reminderMinute,
                                        reminderFrequency = currentReminderFrequency,
                                        language = selectedLanguage,
                                        age = ageInput.toIntOrNull() ?: profile.age,
                                        gender = selectedGender,
                                        isOnboarded = false, // TRIGGER ONBOARDED TO FALSE TO RESET IT FOR TESTING
                                        isDeveloperMode = true
                                    )
                                    Toast.makeText(context, if (selectedLanguage == "en-US") "Launching initial Onboarding introduction..." else "正在啟動初始功能導航體驗...", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = t("initial_intro_btn"),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = t("initial_intro_btn"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.generateRandomTestData {
                                        Toast.makeText(context, t("random_data_success"), Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = t("random_data_btn"),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = t("random_data_btn"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.deleteAllData {
                                        Toast.makeText(context, if (selectedLanguage == "en-US") "Cleared all weight records!" else "已成功刪除所有量測歷史紀錄！", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = if (selectedLanguage == "en-US") "Delete All Data" else "刪除所有資料",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (selectedLanguage == "en-US") "Delete All Data" else "刪除所有資料",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.resetProfileAndRestart {
                                        Toast.makeText(context, if (selectedLanguage == "en-US") "Wiped all profile and weight records! Starting onboarding fresh..." else "已完全自資料庫清除所有量歷程與設定！重啟新手功能導流體驗...", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = if (selectedLanguage == "en-US") "Start From Scratch Test" else "完全從頭開始測試",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (selectedLanguage == "en-US") "Start From Scratch Test" else "完全從頭開始測試",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // 5. Submission save button
        item {
            Button(
                onClick = { saveSettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "儲存")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(t("save_settings_btn"), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Animated Vertical Save Button centered vertically on the extreme right
    androidx.compose.animation.AnimatedVisibility(
        visible = hasChanges && userMadeChanges,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 4.dp)
    ) {
        Surface(
            onClick = { saveSettings() },
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(42.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = t("save_settings_btn"),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                val vertText = if (selectedLanguage == "en-US") "SAVE" else "儲存設定"
                vertText.forEach { char ->
                    Text(
                        text = char.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}
}
