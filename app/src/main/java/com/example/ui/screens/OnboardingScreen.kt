package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: WeightViewModel,
    profile: UserProfile
) {
    val context = LocalContext.current

    // Internal onboarding state variables
    var currentStep by remember { mutableIntStateOf(0) }
    var selectedLanguage by remember { mutableStateOf(profile.language) }
    var isMetric by remember { mutableStateOf(profile.isMetricUnit) }
    var heightInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("其他") }
    var targetWeightInput by remember { mutableStateOf("") }

    val t = { key: String -> Localization.get(key, selectedLanguage) }

    val heightUnit = if (isMetric) "cm" else t("unit_inch") ?: "in"
    val weightUnit = if (isMetric) "kg" else "lb"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = t("app_title"), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Horizontal Step DOT Indicator
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..4).forEach { stepIndex ->
                    val isActive = stepIndex == currentStep
                    val isCompleted = stepIndex < currentStep
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 12.dp else 8.dp)
                            .background(
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else if (isCompleted) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }



            // Body content according to the selected page code
            when (currentStep) {
                0 -> {
                    // Page 1: Welcome to BMI Assistant with Units settings
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = CreamAccentRed,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = t("onboarding_title"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (selectedLanguage == "en-US") {
                            "This is your personal health compass and weight log tracker. Let's calibrate your localized environment and get started!"
                        } else {
                            "這是您的專屬體型管理與 BMI 指標紀錄儀。在我們攜手開啟旅程前，請先微調符合您習慣的使用環境！"
                        },
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 1. Language Toggle
                            Column {
                                Text(
                                    text = t("lang_setting_title"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(4.dp)
                                ) {
                                    listOf("zh-TW" to "繁體中文", "en-US" to "English (USA)").forEach { (langKey, label) ->
                                        val isSelected = selectedLanguage == langKey
                                        Button(
                                            onClick = { selectedLanguage = langKey },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // 2. Unit System Toggle
                            Column {
                                Text(
                                    text = t("metric_system"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(4.dp)
                                ) {
                                    listOf(true to t("metric_opt"), false to t("imperial_opt")).forEach { (metricKey, label) ->
                                        val isSelected = isMetric == metricKey
                                        Button(
                                            onClick = { isMetric = metricKey },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = if (selectedLanguage == "en-US") 9.5.sp else 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { currentStep = 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (selectedLanguage == "en-US") "Next Step" else "閱讀 BMI 基本介紹 ➡️", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                1 -> {
                    // Page 2: BMI reference values instruction
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = if (selectedLanguage == "en-US") "What is BMI?" else "📊 什麼是 BMI？",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (selectedLanguage == "en-US") {
                            "Body Mass Index (BMI) is calculated from height and weight. It is a simple, globally accepted measure of whether your physique belongs to standard ranges."
                        } else {
                            "身體質量指數 (BMI) 是以身高和體重配合公式計算而出的數值，用以評定個人的身體密度。它是全球公認用來快速篩檢健康與體態肥胖程度的主流規格指標！"
                        },
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Reference Range Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (selectedLanguage == "en-US") "Understood BMI Benchmark Scales" else "💡 BMI 健康與肥胖指標參考範圍",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Underweight
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (selectedLanguage == "en-US") "Underweight" else "體重過輕 (< 18.5)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .background(CreamAccentBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(if (selectedLanguage == "en-US") "Nutrition Focus" else "注意補給", fontSize = 10.sp, color = CreamAccentBlue, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Normal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (selectedLanguage == "en-US") "Normal (18.5 - 24.0)" else "健康範圍 (18.5 ~ 24.0)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CreamAccentGreen)
                                Box(
                                    modifier = Modifier
                                        .background(CreamAccentGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(if (selectedLanguage == "en-US") "Excellent" else "完美標準", fontSize = 10.sp, color = CreamAccentGreen, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Overweight
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (selectedLanguage == "en-US") "Overweight (24.0 - 27.0)" else "體重過重 (24.0 ~ 27.0)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .background(CreamAccentGold.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(if (selectedLanguage == "en-US") "Monitor Watch" else "精準管理", fontSize = 10.sp, color = CreamAccentGold, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Obese
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (selectedLanguage == "en-US") "Obese (>= 27.0)" else "體重肥胖 (>= 27.0)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CreamAccentRed)
                                Box(
                                    modifier = Modifier
                                        .background(CreamAccentRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(if (selectedLanguage == "en-US") "Urgent Action" else "高度警惕", fontSize = 10.sp, color = CreamAccentRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentStep = 0 },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (selectedLanguage == "en-US") "Back" else "⬅️ 上一步", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { currentStep = 2 },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (selectedLanguage == "en-US") "Next" else "填寫基本資料 ➡️", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                2 -> {
                    // Page 3: Body metrics inputs
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = if (selectedLanguage == "en-US") "Your Body Profiles" else "👤 填寫基本健康資料",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (selectedLanguage == "en-US") {
                            "Please enter height, initial weight, age, and biological gender so we can analyze."
                        } else {
                            "請填入您當前最精準的身體數值，我們將依據此初始對位計算，提供全天候的 BMI 狀態監測與健康建議！"
                        },
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Real Body Metrics Entry Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Height field
                            OutlinedTextField(
                                value = heightInput,
                                onValueChange = { heightInput = it },
                                label = { Text("${t("height_label")} ($heightUnit)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Weight field
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { weightInput = it },
                                label = { Text("${t("onboarding_weight")} ($weightUnit)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = CreamAccentGold) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Age field
                            OutlinedTextField(
                                value = ageInput,
                                onValueChange = { ageInput = it },
                                label = { Text(t("onboarding_age")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Gender toggle row
                            Column {
                                Text(
                                    text = t("onboarding_gender"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(4.dp)
                                ) {
                                    listOf("男", "女", "其他").forEach { genderKey ->
                                        val label = when (genderKey) {
                                            "男" -> t("gender_male")
                                            "女" -> t("gender_female")
                                            else -> t("gender_other")
                                        }
                                        val isSelected = selectedGender == genderKey
                                        Button(
                                            onClick = { selectedGender = genderKey },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentStep = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (selectedLanguage == "en-US") "Back" else "⬅️ 上一步", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val parsedH = heightInput.toDoubleOrNull()
                                val parsedW = weightInput.toDoubleOrNull()
                                val parsedAge = ageInput.toIntOrNull()

                                if (parsedH == null || parsedH <= 0) {
                                    Toast.makeText(context, t("enter_valid_height"), Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (parsedW == null || parsedW <= 0) {
                                    Toast.makeText(context, t("please_enter_weight"), Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (parsedAge == null || parsedAge <= 0) {
                                    val err = if (selectedLanguage == "en-US") "Please enter valid age!" else "請輸入合法的年齡！"
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                currentStep = 3
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (selectedLanguage == "en-US") "Analyze BMI 🔍" else "計算 BMI 狀態 🔍", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                3 -> {
                    // Page 4: Diagnosis Result based on height and weight inputs
                    val parsedH = heightInput.toDoubleOrNull() ?: 170.0
                    val parsedW = weightInput.toDoubleOrNull() ?: 60.0
                    val heightInM = (if (isMetric) parsedH else parsedH * 2.54) / 100.0
                    val weightInKg = if (isMetric) parsedW else parsedW / 2.20462
                    val finalBmi = if (heightInM > 0) weightInKg / (heightInM * heightInM) else 0.0

                    val (bmiCategory, categoryColor, categoryAdvice) = when {
                        finalBmi < 18.5 -> {
                            val cat = if (selectedLanguage == "en-US") "Underweight" else "體重過輕"
                            val advice = if (selectedLanguage == "en-US") {
                                "Nurture nutritious high-density dietary patterns and practice moderate resistance drills to sculpt muscles safely! 💪"
                            } else {
                                "建議攝取均衡高熱量營養，並搭配規律肌力阻力訓練增強抵抗力以建立結實完美體格！💪"
                            }
                            Triple(cat, CreamAccentBlue, advice)
                        }
                        finalBmi < 24.0 -> {
                            val cat = if (selectedLanguage == "en-US") "Normal" else "體態正常"
                            val advice = if (selectedLanguage == "en-US") {
                                "Sensational! Your body compositions lean beautifully on premium scales. Persist consistent status logging! 🌟"
                            } else {
                                "太棒了！您的體態在完美健康範圍（黃金標準）！請繼續保持均衡輕量化飲食與適度有氧，持之以恆！🌟"
                            }
                            Triple(cat, CreamAccentGreen, advice)
                        }
                        finalBmi < 27.0 -> {
                            val cat = if (selectedLanguage == "en-US") "Overweight" else "體重過重"
                            val advice = if (selectedLanguage == "en-US") {
                                "Gently adjust daily caloric inputs and engage in fun cardio sequences. We support you all the way! 🥗"
                            } else {
                                "您的數據目前有些微超重。建議微調日常精緻甜食熱量，並循序漸進培養慢跑等有氧愛好，向完美健美挺進！🥗"
                            }
                            Triple(cat, CreamAccentGold, advice)
                        }
                        else -> {
                            val cat = if (selectedLanguage == "en-US") "Obese" else "體重肥胖"
                            val advice = if (selectedLanguage == "en-US") {
                                "Breathe and relax! Health is a marathon. Transitioning slowly to high-fiber meals protects joints and heart metrics safely. ❤️"
                            } else {
                                "肥胖在長期容易加重關節負荷與血管壓。建議多吃高纖全穀飲食，控制油脂分母，讓我們陪伴您循序漸進安全瘦身！❤️"
                            }
                            Triple(cat, CreamAccentRed, advice)
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = if (selectedLanguage == "en-US") "Your BMI Diagnosis" else "🎯 您的 BMI 解算報告",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Diagnostic Card showing customized BMI value & recommendation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (selectedLanguage == "en-US") "Primary Indices" else "當前測量數值",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )

                            // Weight & Height Display
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = heightInput, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "${t("height_label")} ($heightUnit)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = weightInput, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "${t("onboarding_weight")} ($weightUnit)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Large colored BMI gauge
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f", finalBmi),
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = categoryColor
                                )
                                Text(
                                    text = "BMI INDEX",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            // Result card
                            Box(
                                modifier = Modifier
                                    .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (selectedLanguage == "en-US") "Physique State: $bmiCategory" else "目前診斷體態：$bmiCategory",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = categoryColor,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Personalized advice
                            Text(
                                text = categoryAdvice,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentStep = 2 },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (selectedLanguage == "en-US") "Edit Info" else "⬅️ 修改資料", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val recHInM = (if (isMetric) parsedH else parsedH * 2.54) / 100.0
                                val recKg = 22.0 * (recHInM * recHInM)
                                val recDisplay = if (isMetric) recKg else recKg * 2.20462
                                if (targetWeightInput.isEmpty()) {
                                    targetWeightInput = String.format(Locale.getDefault(), "%.1f", recDisplay)
                                }
                                currentStep = 4
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = if (selectedLanguage == "en-US") "Next: Set Target" else "下一步：設定目標 ➡️",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                else -> {
                    // Page 5: Custom target setting
                    val parsedH = heightInput.toDoubleOrNull() ?: 170.0
                    val heightInM = (if (isMetric) parsedH else parsedH * 2.54) / 100.0
                    val parsedW = weightInput.toDoubleOrNull() ?: 60.0

                    // Recommended weight with BMI of 22
                    val recKg = 22.0 * (heightInM * heightInM)
                    val recDisplayValue = if (isMetric) recKg else recKg * 2.20462
                    val recDisplayString = String.format(Locale.getDefault(), "%.1f", recDisplayValue)

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = CreamAccentGold,
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        text = if (selectedLanguage == "en-US") "🎯 Set Ideal Health & Weight Goal" else "🎯 設定理想健康目標",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (selectedLanguage == "en-US") {
                            "Setting a clear weight target keeps you focused! Based on your height, we recommend standard BMI 22 at $recDisplayString $weightUnit."
                        } else {
                            "設定明確的體重目標，是健康管理的起點！依據您的身高，系統推薦 BMI 22 的健康體重為：$recDisplayString $weightUnit。"
                        },
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Target input card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (selectedLanguage == "en-US") {
                                    "Customize Your Target"
                                } else {
                                    "🖊️ 支援微調與自定義"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Target weight field
                            OutlinedTextField(
                                value = targetWeightInput,
                                onValueChange = { targetWeightInput = it },
                                label = { Text(if (selectedLanguage == "en-US") "Target Weight ($weightUnit)" else "目標體重 ($weightUnit)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = CreamAccentGold
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                             )

                             // Quick adjustment buttons row
                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.spacedBy(8.dp)
                             ) {
                                 listOf(-1.0, -0.5, 0.5, 1.0).forEach { offset ->
                                     val prefix = if (offset > 0) "+$offset" else offset.toString()
                                     Button(
                                         onClick = {
                                             val currentVal = targetWeightInput.toDoubleOrNull() ?: recDisplayValue
                                             val nextVal = (currentVal + offset).coerceAtLeast(10.0)
                                             targetWeightInput = String.format(Locale.getDefault(), "%.1f", nextVal)
                                         },
                                         colors = ButtonDefaults.buttonColors(
                                             containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                             contentColor = MaterialTheme.colorScheme.primary
                                         ),
                                         modifier = Modifier.weight(1f).height(36.dp),
                                         contentPadding = PaddingValues(0.dp)
                                     ) {
                                         Text(text = prefix, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                     }
                                 }
                             }

                             // Dynamic target BMI indicator
                             val customTargetWeight = targetWeightInput.toDoubleOrNull() ?: recDisplayValue
                             val customTargetWeightInKg = if (isMetric) customTargetWeight else customTargetWeight / 2.20462
                             val customTargetBmi = if (heightInM > 0) customTargetWeightInKg / (heightInM * heightInM) else 0.0

                             Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.SpaceBetween,
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Text(
                                     text = if (selectedLanguage == "en-US") "Projected Target BMI:" else "預估目標 BMI：",
                                     fontSize = 12.sp,
                                     fontWeight = FontWeight.Medium,
                                     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                 )
                                 Text(
                                     text = String.format(Locale.getDefault(), "%.1f", customTargetBmi),
                                     fontSize = 16.sp,
                                     fontWeight = FontWeight.ExtraBold,
                                     color = if (customTargetBmi >= 18.5 && customTargetBmi < 24.0) CreamAccentGreen else CreamAccentGold
                                 )
                             }
                         }
                     }

                     Spacer(modifier = Modifier.height(10.dp))

                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.spacedBy(12.dp)
                     ) {
                         OutlinedButton(
                             onClick = { currentStep = 3 },
                             modifier = Modifier
                                 .weight(1f)
                                 .height(52.dp),
                             shape = RoundedCornerShape(16.dp)
                         ) {
                             Text(if (selectedLanguage == "en-US") "Back" else "⬅️ 回報告", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                         }

                         Button(
                             onClick = {
                                 val inputTargetW = targetWeightInput.toDoubleOrNull()
                                 if (inputTargetW == null || inputTargetW <= 0.0) {
                                     val err = if (selectedLanguage == "en-US") "Please enter a valid target weight!" else "請輸入有效的目標體重！"
                                     Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                     return@Button
                                 }

                                 val heightInCm = if (isMetric) parsedH else parsedH * 2.54
                                 val weightInKg = if (isMetric) parsedW else parsedW / 2.20462
                                 val targetWeightInKg = if (isMetric) inputTargetW else inputTargetW / 2.22462
                                 val ageVal = ageInput.toIntOrNull() ?: 25

                                 // 1. Record initial weigh log
                                 viewModel.logWeight(
                                     weight = weightInKg,
                                     note = if (selectedLanguage == "en-US") "Initial Setup Weight" else "初始建檔體重紀錄",
                                     timestamp = System.currentTimeMillis()
                                 )

                                 // 2. Save profile settings with target weight
                                 viewModel.updateProfile(
                                     heightCm = heightInCm,
                                     targetWeightKg = targetWeightInKg,
                                     isMetric = isMetric,
                                     reminderEnabled = profile.reminderEnabled,
                                     reminderHour = profile.reminderHour,
                                     reminderMinute = profile.reminderMinute,
                                     reminderFrequency = profile.reminderFrequency,
                                     language = selectedLanguage,
                                     age = ageVal,
                                     gender = selectedGender,
                                     isOnboarded = true
                                 )

                                 val successToast = if (selectedLanguage == "en-US") "Welcome to your health and BMI tracking journey!" else "成功建檔！開啟您的健康與體態管理之旅！"
                                 Toast.makeText(context, successToast, Toast.LENGTH_LONG).show()
                             },
                             modifier = Modifier
                                 .weight(1.3f)
                                 .height(52.dp),
                             shape = RoundedCornerShape(16.dp),
                             colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                         ) {
                             Text(text = t("onboarding_submit"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                         }
                     }
                 }
             }
         }
     }
 }
