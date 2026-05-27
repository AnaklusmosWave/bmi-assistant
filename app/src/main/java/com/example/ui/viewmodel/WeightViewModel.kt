package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.AppDatabase
import com.example.data.database.UserProfile
import com.example.data.database.WeightLog
import com.example.data.repository.WeightRepository
import com.example.receiver.ReminderNotificationReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

sealed interface AiSuggestionState {
    object Idle : AiSuggestionState
    object Loading : AiSuggestionState
    data class Success(val advice: String) : AiSuggestionState
    data class Error(val error: String) : AiSuggestionState
}

class WeightViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WeightRepository
    val context = application.applicationContext

    init {
        val db = AppDatabase.getDatabase(application)
        repository = WeightRepository(db.weightDao(), db.userProfileDao())

        // Initial launch settings: register/schedule reminder if enabled
        viewModelScope.launch {
            val profile = repository.getUserProfileDirect()
            if (profile.reminderEnabled) {
                ReminderNotificationReceiver.scheduleFromProfile(context, profile)
            }
        }
    }

    // List of Logs & User Profile reactive flows
    val allLogs: StateFlow<List<WeightLog>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allLogsAscending: StateFlow<List<WeightLog>> = repository.allLogsAscending.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val userProfile: StateFlow<UserProfile?> = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // AI coaching Advice State
    private val _aiSuggestionState = mutableStateOf<AiSuggestionState>(AiSuggestionState.Idle)
    val aiSuggestionState: State<AiSuggestionState> = _aiSuggestionState

    // Log a new weight
    fun logWeight(weight: Double, note: String, timestamp: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentProfile = repository.getUserProfileDirect()
            val newLog = WeightLog(
                weightKg = weight,
                heightCm = currentProfile.heightCm,
                timestamp = timestamp,
                note = note
            )
            repository.insertLog(newLog)
        }
    }

    // Delete a logged weight
    fun deleteLog(log: WeightLog) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLog(log)
        }
    }

    // Update user profile settings
    fun updateProfile(
        heightCm: Double,
        targetWeightKg: Double,
        isMetric: Boolean,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        reminderFrequency: String,
        language: String,
        age: Int,
        gender: String,
        isOnboarded: Boolean,
        isDeveloperMode: Boolean = false,
        weekStartDay: Int = 2,
        dateFormatPattern: String = "DEFAULT",
        itemsPerPage: Int = 10
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = UserProfile(
                id = 1,
                heightCm = heightCm,
                targetWeightKg = targetWeightKg,
                isMetricUnit = isMetric,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                reminderFrequency = reminderFrequency,
                language = language,
                age = age,
                gender = gender,
                isOnboarded = isOnboarded,
                isDeveloperMode = isDeveloperMode,
                weekStartDay = weekStartDay,
                dateFormatPattern = dateFormatPattern,
                itemsPerPage = itemsPerPage
            )
            repository.saveUserProfile(profile)
 
            // Dynamic schedule logic based on updated settings
            withContext(Dispatchers.Main) {
                if (reminderEnabled) {
                    ReminderNotificationReceiver.scheduleFromProfile(context, profile)
                } else {
                    ReminderNotificationReceiver.cancelAlarm(context)
                }
            }
        }
    }

    // Simulation logic to build 15 reasonable, realistic historical weights over 30 days
    fun generateRandomTestData(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = repository.getUserProfileDirect()
            repository.deleteAllLogs()

            val target = profile.targetWeightKg
            val isEn = profile.language == "en-US"

            // List of beautiful, contextual notes corresponding to index 0 to 14
            val notesCh = listOf(
                "🎯 完美持平！目標就在眼前",
                "🔋 水分與睡眠充足，感覺很有體力",
                "🏃 慢跑半小時，爆汗十分舒服",
                "🥗 地中海沙拉飲食，輕盈無負擔",
                "💧 今日水分充足，新陳代謝良好",
                "💪 持續記錄中，感覺肌肉變緊實了",
                "🍵 昨晚減少精緻澱粉，體重有感下降",
                "🏃 快走 10000 步，通勤當作運動",
                "🍉 晚餐吃水果與優格，清空腸胃",
                "🏋️ 阻力重訓 45 分鐘，提升基礎代謝",
                "🍳 執行一日低碳飲食，精神非常好",
                "🔋 昨晚有睡飽 8 小時，精力充沛",
                "🍵 晚餐少油鹽原型食物，不吃消夜",
                "💧 養成每日多喝水好習慣",
                "🏃 起步慢跑 3 公里，呼吸稍喘"
            )

            val notesEn = listOf(
                "🎯 Almost hit my target! Perfectly on track",
                "🔋 Feeling energized & rested, fully active",
                "🏃 Jogged for 30 mins, sweating a lot",
                "🥗 Delicious healthy Mediterranean salad day",
                "💧 Daily hydration targets met successfully",
                "💪 Keep going! Core feels firmer already",
                "🍵 Cut refined carbs yesterday, feeling lighter",
                "🏃 Walked 10,000 steps during commute",
                "🍉 Clean supper with fruits and Greek yogurt",
                "🏋️ Heavy progressive strength training session",
                "🍳 Low carb meal prep worked perfectly",
                "🔋 Great deep sleep for over 8 hours",
                "🍵 Prepared clean healthy protein dinner",
                "💧 Built dynamic daily hydration habit",
                "🏃 Started journey with a fresh 3km run!"
            )

            // We generate 30 records from 29 down to 0 (where 29 means 29 days ago, 0 means today)
            for (i in 29 downTo 0) {
                val dayOffset = i * 1L
                val timeMillis = System.currentTimeMillis() - (dayOffset * 24 * 60 * 60 * 1000L)

                // Decaying weight curve: starts slightly higher, drops gracefully
                val progressRatio = (29 - i).toDouble() / 29.0 // 0.0 initially, 1.0 today
                val startWeight = target + 5.5
                val currentBase = startWeight - (progressRatio * 4.4) // drops towards target + 1.1

                // Cyclical water weight/sodium fluctuation to look authentic
                val fluctuation = Math.sin(i.toDouble() * 1.5) * 0.4 + ((i % 3 - 1) * 0.15)
                val calculatedWeight = currentBase + fluctuation
                val roundedWeight = Math.round(calculatedWeight * 10.0) / 10.0

                val note = if (isEn) notesEn[i % 15] else notesCh[i % 15]

                val log = WeightLog(
                    weightKg = roundedWeight,
                    heightCm = profile.heightCm,
                    timestamp = timeMillis,
                    note = note
                )
                repository.insertLog(log)
            }

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // Deletes all logged measurements from the database
    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllLogs()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // Completely wipes all data and resets the profile onboarding status
    fun resetProfileAndRestart(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllLogs()
            repository.saveUserProfile(UserProfile(id = 1, isOnboarded = false))
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // Dynamic prompt and invoke direct Gemini REST API with local rule-based safety fallback
    fun generateAiAdvice() {
        _aiSuggestionState.value = AiSuggestionState.Loading

        viewModelScope.launch {
            val currentProfile = repository.getUserProfileDirect()
            val logsList = allLogs.value.take(15) // Recent 15 logs for analysis

            val currentHeight = currentProfile.heightCm
            val targetWeight = currentProfile.targetWeightKg
            val isMetric = currentProfile.isMetricUnit
            val isEnglish = currentProfile.language == "en-US"

            if (logsList.isEmpty()) {
                val emptyMsg = if (isEnglish) {
                    "💡 Direct logged weight values to begin! Tap 'Generate AI Advice' once entered and Gemini will synthesize highly professional tips just for you."
                } else {
                    "💡 開始新增您的第一個體重記錄後，點選『生成 AI 分析』，Gemini 將為您提供量身打造的飲食與運動指南喔！"
                }
                _aiSuggestionState.value = AiSuggestionState.Success(emptyMsg)
                return@launch
            }

            // Read API key
            val apiKey = BuildConfig.GEMINI_API_KEY
            val isApiKeyMissing = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY"

            if (isApiKeyMissing) {
                // Return immediate beautifully-crafted local expert advice fallback to minimize empty setups
                val localAdvice = generateLocalHealthAdvice(logsList, currentProfile)
                _aiSuggestionState.value = AiSuggestionState.Success(localAdvice)
                return@launch
            }

            // Build detailed analysis prompt
            val lastLog = logsList.first() // latest log
            val bmiString = String.format(Locale.getDefault(), "%.1f", lastLog.bmi)
            val formatStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

            val historyRecordsString = logsList.joinToString("\n") { log ->
                "- ${formatStr.format(Date(log.timestamp))}: ${if (isEnglish) "Weight" else "體重"} ${log.weightKg}kg, BMI ${String.format(Locale.getDefault(), "%.1f", log.bmi)}"
            }

            val systemInstruction = if (isEnglish) {
                "You are a warm, professional AI weight loss and health coaching expert. You excel at combining medical common sense with weight history analysis to provide specific nutrition, diet, and exercise guidelines in an encouraging, friendly tone."
            } else {
                "你是一個溫暖、專業的 AI 減重與健康諮詢教練。你擅長結合醫學常識與體重紀錄分析，以鼓勵、親切的口吻提供具體的營養、飲食及運動方案指南。"
            }

            val prompt = if (isEnglish) {
                """
                Please act as my health coach and analyze the following health and weight logs:
                
                Basic Info:
                - Height: $currentHeight cm
                - Age: ${currentProfile.age}
                - Gender: ${currentProfile.gender}
                - Current Weight: ${lastLog.weightKg} kg
                - Target Weight: $targetWeight kg
                - Current BMI: $bmiString
                
                Recent measurement history:
                $historyRecordsString
                
                Please provide me with:
                1. A warm encouragement and status feedback regarding my current BMI class and progress.
                2. Three concrete, actionable lifestyle guidelines (such as daily water intake, diet, or exercise frequency).
                3. Keep the total response within 300 words. Use bullet points and clean readability. Write in English.
                """.trimIndent()
            } else {
                """
                請扮演我的健康教練，幫我分析以下的健康與體重紀錄：
                
                基本狀態：
                - 身高：$currentHeight 公分
                - 年齡：${currentProfile.age} 歲
                - 性別：${currentProfile.gender}
                - 目前體重：${lastLog.weightKg} 公斤
                - 目標體重：$targetWeight 公斤
                - 目前 BMI：$bmiString
                
                近期測量歷史：
                $historyRecordsString
                
                請為我提供：
                1. 針對目前的 BMI 分級與目標（在 ${if (isMetric) "kg" else "lbs"} 單位下），給予溫馨的鼓勵與狀態回饋。
                2. 提供 3 項具體、切實可行的生活指引（例如每日水分攝取、飲食建議或運動頻率）。
                3. 整篇回答不超過 300 字，必須使用繁體中文、排版整齊、使用條列式呈現，保持溫暖與親切，不需要任何工程程式碼。
                """.trimIndent()
            }

            try {
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
                )
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!aiText.isNullOrEmpty()) {
                    _aiSuggestionState.value = AiSuggestionState.Success(aiText)
                } else {
                    val errMsg = if (isEnglish) "Unable to fetch AI suggestions, please try again later." else "無法獲取 AI 分析，請稍後再試。"
                    _aiSuggestionState.value = AiSuggestionState.Error(errMsg)
                }
            } catch (e: Exception) {
                Log.e("WeightVM", "Gemini API failed", e)
                val fallbackErrorAdvice = generateLocalHealthAdvice(logsList, currentProfile) + 
                    if (isEnglish) "\n\n*(Note: Gemini cloud connection issue; automatically switched to local coach mode)*" else "\n\n*(注意：Gemini 雲端連線異常，已自動切換至本機智慧教練模式)*"
                _aiSuggestionState.value = AiSuggestionState.Success(fallbackErrorAdvice)
            }
        }
    }

    // Highly comprehensive, medical-inspired local expert logic
    private fun generateLocalHealthAdvice(logs: List<WeightLog>, profile: UserProfile): String {
        val lastLog = logs.first()
        val bmi = lastLog.bmi
        val isMetric = profile.isMetricUnit
        val targetWeight = profile.targetWeightKg
        val currentWeight = lastLog.weightKg
        val isEnglish = profile.language == "en-US"

        val unit = if (isMetric) "kg" else "lb"
        val displayWeight = if (isMetric) currentWeight else currentWeight * 2.20462
        val displayTarget = if (isMetric) targetWeight else targetWeight * 2.20462
        val goalDiff = displayWeight - displayTarget

        if (isEnglish) {
            val bmiCategory = when {
                bmi < 18.5 -> "Underweight"
                bmi >= 18.5 && bmi < 24.0 -> "Healthy/Normal Weight"
                bmi >= 24.0 && bmi < 27.0 -> "Overweight"
                else -> "Obesity range"
            }

            val welcome = "✨ **Local Smart Coach Health Report (API key not set)** ✨\n\n" +
                    "Your current weight is **${String.format(Locale.getDefault(), "%.1f", displayWeight)} $unit**, " +
                    "BMI index is **${String.format(Locale.getDefault(), "%.1f", bmi)}**, falling within the [**$bmiCategory**] range.\n"

            val goalAdvice = if (goalDiff > 0) {
                "You are **${String.format(Locale.getDefault(), "%.1f", goalDiff)} $unit** away from your ideal goal of **${String.format(Locale.getDefault(), "%.1f", displayTarget)} $unit**. Real progress takes patience, stay committed!\n\n"
            } else {
                "Congratulations! You've matched or exceeded your ideal target goal of **${String.format(Locale.getDefault(), "%.1f", displayTarget)} $unit**! Maintain this exceptional rhythm! 👏\n\n"
            }

            val tips = when {
                bmi < 18.5 -> """
                    🎯 **Coach's 3 Nutrition & Strength Recommendations**:
                    1. 🍱 **Macronutrient Balance**: Increase intake of lean proteins (chicken, salmon, tofu) and complex carbohydrates (sweet potatoes, oats) to build lean mass.
                    2. 🥛 **Frequent Small Meals**: Introduce nutrient-dense snacks like assorted nuts, high-protein Greek yogurt, or oat milk between main meals.
                    3. 🏋️ **Progressive Resistance Training**: Exercise with resistance or weights 2-3 times per week to prompt functional muscle volume growth.
                """.trimIndent()

                bmi >= 18.5 && bmi < 24.0 -> """
                    🎯 **Coach's 3 Lifestyle Maintenance Habits**:
                    1. 🥗 **Balanced Mediterranean Food**: Embrace green leaf vegetables, extra virgin olive oil, and clean proteins while avoiding deep fried processes.
                    2. 💧 **Proper Hydration**: Target daily water intake of about 35ml per kg of your weight (e.g., 60kg requires 2100ml) to energize metabolism.
                    3. 🏃 **Hybrid Exercise**: Keep a target of 150 minutes of moderate aerobic workouts weekly (like swimming, logging runs, or Pilates).
                """.trimIndent()

                bmi >= 24.0 && bmi < 27.0 -> """
                    🎯 **Coach's 3 Body Recomposition Guidelines**:
                    1. 🍎 **Optimize Meal Order**: Eat cooked vegetables first, followed by proteins, and utilize carbohydrate starches at the very last to keep blood sugar stable.
                    2. 🚶 **Steady-State Cardio**: Commit to daily 30-minute power walking or jogging which serves as an outstanding fat oxidizing regimen.
                    3. 😴 **Quality Sleep Patterns**: Rest 7-8 hours firmly. Chronic sleep deprivation spikes ghrelin levels, amplifying unnecessary cravings.
                """.trimIndent()

                else -> """
                    🎯 **Coach's 3 Vital Healthy Weight-loss Directives**:
                    1. 🍽️ **Reduce Refined Sugars**: Limit intake of sweeteners, heavily processed flour, and pastries. Anchor hunger using high-fiber organic foods.
                    2. 💧 **Sip Water Warm before Meals**: Sip 300ml of warm water 10 minutes before principal meals. Take 20 small chew cycles to trigger satiety.
                    3. 🚶 **Low Impact Exercises**: Safeguard joints initially by doing standard cycling, brisk flat walks, or light water aerobics.
                """.trimIndent()
            }

            val hint = "\n\n💡 **Tip**: Enter your `GEMINI_API_KEY` in the AI Studio Secrets panel to activate live generative analyses based on your weight history!"
            return welcome + goalAdvice + tips + hint
        }

        val bmiCategory = when {
            bmi < 18.5 -> "體重過輕"
            bmi >= 18.5 && bmi < 24.0 -> "健康正常體態"
            bmi >= 24.0 && bmi < 27.0 -> "過重體態"
            else -> "肥胖體態"
        }

        val welcome = "✨ **本機智慧教練分析健康報表 (API未設定)** ✨\n\n" +
                "您目前的體重為 **${String.format(Locale.getDefault(), "%.1f", displayWeight)} $unit**，" +
                "BMI 指數為 **${String.format(Locale.getDefault(), "%.1f", bmi)}**，落於【**$bmiCategory**】分區。\n"

        val goalAdvice = if (goalDiff > 0) {
            "距離您的理想目標 **${String.format(Locale.getDefault(), "%.1f", displayTarget)} $unit** 還差 **${String.format(Locale.getDefault(), "%.1f", goalDiff)} $unit**。請保持信心，讓我們一起踏實前進！\n\n"
        } else {
            "您已超越或持平您的理想目標！這是一個巨大的成就，請保持目前的精采節奏。👏\n\n"
        }

        val tips = when {
            bmi < 18.5 -> """
                🎯 **教練給您的 3 個營養增補建議**：
                1. 🍱 **營養素比例**：增加高蛋白食物（如雞肉、鮭魚、豆腐）和健康的複雜碳水（地瓜、糙米），支持健康增肌。
                2. 🥛 **少量多餐**：在主餐之間增加堅果、優格或燕麥牛奶等富含能量的健康點心。
                3. 🏋️ **漸進式阻力訓練**：每週進行 2-3 次核心與力量訓練，能幫助您長出漂亮的健康肌肉，而非僅僅增加體脂肪。
            """.trimIndent()

            bmi >= 18.5 && bmi < 24.0 -> """
                🎯 **教練給您的 3 個體態維護建議**：
                1. 🥗 **地中海飲食**：多攝取大量深綠色蔬菜、優質橄欖油與水果，配合原型食物。
                2. 💧 **足量飲水**：將每日飲水量定在「體重 × 35 毫升」（例如 60kg 需 2100 毫升），能大幅促進新陳代謝。
                3. 🏃 **心肺與肌力並重**：維持每週 150 分鐘的中等強度運動，比如快走、皮拉提斯或慢跑，有助於保持優良心血管功能。
            """.trimIndent()

            bmi >= 24.0 && bmi < 27.0 -> """
                🎯 **教練給您的 3 個減脂起步建議**：
                1. 🍎 **控糖減脂**：每餐先喝湯或水、接著吃蔬菜與蛋白質，最後吃澱粉類。這能穩定血糖，避免脂肪生成。
                2. 🚶 **低強度長心肺**：每天下班提早一站下車，進行 30 分鐘微喘快走。低強度有氧是燃燒多氧脂肪最好的工具。
                3. 😴 **規律充足睡眠**：每天睡足 7-8 小時，睡眠不足會導致飢餓素大幅上升，增加對甜點的渴望喔。
            """.trimIndent()

            else -> """
                🎯 **教練給您的 3 個重點減重建議**：
                1. 🍽️ **減糖與原型食物**：大幅減少加工食品、精緻麵包與含糖手搖飲，多選擇少油鹽的原型菜葉與優質瘦肉。
                2. 💧 **溫水飽足感**：飯前 10 分鐘規律飲用 300 毫升溫開水，並拉長每一口咀嚼次數至 20 次，大腦會加速產生飽足感。
                3. 🚶 **安全不傷膝運動**：先從健走、游泳或原地踏步等不傷膝關節的運動開始。切記循序漸進，保護好足踝與關節。
            """.trimIndent()
        }

        val hint = "\n\n💡 **限時提示**：在 AI Studio 的 Secrets 面板設定您的 `GEMINI_API_KEY`，便可召喚真實 Gemini AI 提供融合歷史趨勢的自訂分析喔！"

        return welcome + goalAdvice + tips + hint
    }
}
