# 📚 PrivacyShield - Code Examples & Best Practices
## December 2025 | YukiHookAPI + Material 3 Expressive

---

## 🎯 BEST PRACTICES OVERVIEW

| **Area** | **Key Principles** |
|---|---|
| **YukiHookAPI** | Use Kotlin DSL, `YukiBaseHooker`, optional hooks, proper error handling |
| **Anti-Detection** | Load FIRST, filter stack traces, hide classes, minimize overhead |
| **Spoofing** | Consistent values, Luhn-valid IMEI, realistic fingerprints |
| **Architecture** | MVVM + Clean Architecture, UDF, State hoisting |
| **UI** | Material 3 Expressive, Dynamic colors, Spring animations |
| **Data** | DataStore for persistence, immutable state, coroutines |

---

## 🎣 YUKIHOOKAPI BEST PRACTICES

### ✅ DO's

```kotlin
// ✅ Use @InjectYukiHookWithXposed annotation
@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {
    
    // ✅ Configure in onInit()
    override fun onInit() = configs {
        debugLog { tag = "PrivacyShield"; isEnable = BuildConfig.DEBUG }
        isDebug = BuildConfig.DEBUG
    }
    
    // ✅ Use loadHooker() for modular hooks
    override fun onHook() = encase {
        loadHooker(AntiDetectHooker)  // ✅ Anti-detection FIRST
        loadHooker(DeviceHooker)
        loadHooker(NetworkHooker)
    }
}
```

### ❌ DON'Ts

```kotlin
// ❌ DON'T use raw XposedHelpers - use YukiHookAPI DSL
XposedHelpers.findAndHookMethod(...)  // ❌ Old way

// ✅ DO use YukiHookAPI fluent API
"android.telephony.TelephonyManager".toClass()
    .method { name = "getImei" }
    .hook { after { result = spoofedValue } }
```

---

## 📱 DEVICE HOOKER PATTERN

```kotlin
object DeviceHooker : YukiBaseHooker() {
    
    override fun onHook() {
        // ✅ Check app scope first
        if (!SpoofDataStore.isAppEnabled(packageName)) return
        
        // ✅ Hook TelephonyManager methods
        "android.telephony.TelephonyManager".toClass().apply {
            
            // ✅ Use method block for clear targeting
            method { name = "getDeviceId" }.hook {
                after { result = SpoofDataStore.getSpoofedIMEI() }
            }
            
            // ✅ Handle method overloads with paramCount
            method { name = "getImei"; paramCount = 0 }.hook {
                after { result = SpoofDataStore.getSpoofedIMEI() }
            }
            
            method { name = "getImei"; paramCount = 1 }.hook {
                after { result = SpoofDataStore.getSpoofedIMEI() }
            }
            
            // ✅ Use optional() for methods that might not exist
            method { name = "getMeid" }.optional().hook {
                after { result = SpoofDataStore.getSpoofedIMEI() }
            }
        }
        
        // ✅ Hook Build fields directly
        "android.os.Build".toClass().apply {
            field { name = "SERIAL" }.get().set(SpoofDataStore.getSpoofedSerial())
            field { name = "MODEL" }.get().set(SpoofDataStore.getSpoofedModel())
            field { name = "MANUFACTURER" }.get().set(SpoofDataStore.getSpoofedManufacturer())
        }
    }
}
```

---

## 🛡️ ANTI-DETECTION BEST PRACTICES

### Critical: Load Anti-Detection FIRST

```kotlin
override fun onHook() = encase {
    loadHooker(AntiDetectHooker)  // ⚠️ MUST BE FIRST
    loadHooker(DeviceHooker)      // Then spoofing hooks
}
```

### Stack Trace Filtering

```kotlin
object AntiDetectHooker : YukiBaseHooker() {
    
    // ✅ Comprehensive pattern list
    private val HIDDEN_PATTERNS = listOf(
        "de.robv.android.xposed",
        "io.github.lsposed",
        "com.highcapable.yukihookapi",
        "EdHooker", "LSPHooker", "XposedBridge",
        "XC_MethodHook", "XposedHelpers"
    )
    
    override fun onHook() {
        // ✅ Hook Thread.getStackTrace()
        "java.lang.Thread".toClass().apply {
            method { name = "getStackTrace" }.hook {
                after {
                    val stack = result as? Array<StackTraceElement> ?: return@after
                    result = stack.filterNot { element ->
                        HIDDEN_PATTERNS.any { element.className.contains(it) }
                    }.toTypedArray()
                }
            }
        }
        
        // ✅ Hook Throwable.getStackTrace()
        "java.lang.Throwable".toClass().apply {
            method { name = "getStackTrace" }.hook {
                after {
                    val stack = result as? Array<StackTraceElement> ?: return@after
                    result = stack.filterNot { element ->
                        HIDDEN_PATTERNS.any { element.className.contains(it) }
                    }.toTypedArray()
                }
            }
        }
        
        // ✅ Block Class.forName() for Xposed classes
        "java.lang.Class".toClass().apply {
            method { name = "forName"; paramCount = 1 }.hook {
                before {
                    val className = args[0] as? String ?: return@before
                    if (HIDDEN_PATTERNS.any { className.contains(it) }) {
                        throwable = ClassNotFoundException(className)
                    }
                }
            }
        }
    }
}
```

---

## 🔢 VALUE GENERATION BEST PRACTICES

### IMEI Generation (Luhn-Valid)

```kotlin
object IMEIGenerator {
    
    // ✅ Generate valid 15-digit IMEI with Luhn checksum
    fun generate(): String {
        // TAC (Type Allocation Code) - 8 digits, realistic prefix
        val tacPrefixes = listOf("35", "86", "01", "45")  // Common prefixes
        val tac = tacPrefixes.random() + (0..5).map { (0..9).random() }.joinToString("")
        
        // Serial - 6 digits
        val serial = (0..5).map { (0..9).random() }.joinToString("")
        
        // Calculate Luhn check digit
        val checkDigit = calculateLuhn(tac + serial)
        
        return tac + serial + checkDigit
    }
    
    // ✅ Luhn algorithm implementation
    private fun calculateLuhn(input: String): Int {
        var sum = 0
        var isDouble = true
        
        for (i in input.length - 1 downTo 0) {
            var digit = input[i].digitToInt()
            if (isDouble) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
            isDouble = !isDouble
        }
        
        return (10 - (sum % 10)) % 10
    }
    
    // ✅ Validate IMEI format
    fun isValid(imei: String): Boolean {
        if (imei.length != 15 || !imei.all { it.isDigit() }) return false
        return calculateLuhn(imei.dropLast(1)) == imei.last().digitToInt()
    }
}
```

### MAC Address Generation

```kotlin
object MACGenerator {
    
    // ✅ Generate valid unicast MAC (LSB of first octet = 0)
    fun generate(): String {
        val bytes = (0..5).map { Random.nextInt(256) }.toMutableList()
        bytes[0] = bytes[0] and 0xFE  // Clear multicast bit
        return bytes.joinToString(":") { "%02X".format(it) }
    }
}
```

### Fingerprint Generation

```kotlin
object FingerprintGenerator {
    
    // ✅ Generate realistic Build fingerprint
    fun generate(manufacturer: String, model: String): String {
        val brand = manufacturer.lowercase()
        val device = model.lowercase().replace(" ", "")
        val buildId = generateBuildId()
        
        return "$brand/$device/$device:16/$buildId:user/release-keys"
    }
    
    private fun generateBuildId(): String {
        val prefix = listOf("AP3A", "TP1A", "SP2A").random()
        val date = "${(1..12).random().toString().padStart(2, '0')}${(1..28).random().toString().padStart(2, '0')}"
        val patch = (1..100).random().toString().padStart(3, '0')
        return "$prefix.$date.$patch"
    }
}
```

---

## 💾 DATA LAYER BEST PRACTICES

### DataStore Configuration

```kotlin
object SpoofDataStore {
    
    private lateinit var dataStore: DataStore<Preferences>
    
    // ✅ Preference keys as constants
    private object Keys {
        val IMEI = stringPreferencesKey("spoofed_imei")
        val SERIAL = stringPreferencesKey("spoofed_serial")
        val MODEL = stringPreferencesKey("spoofed_model")
        val ENABLED_APPS = stringSetPreferencesKey("enabled_apps")
    }
    
    // ✅ Initialize in Application.onCreate()
    fun init(context: Context) {
        dataStore = context.dataStore
    }
    
    // ✅ Suspend functions for async access
    suspend fun getSpoofedIMEI(): String {
        return dataStore.data.first()[Keys.IMEI] ?: IMEIGenerator.generate().also {
            setSpoofedIMEI(it)
        }
    }
    
    // ✅ Blocking getter for hook context (hooks run in target process)
    fun getSpoofedIMEIBlocking(): String {
        return runBlocking { getSpoofedIMEI() }
    }
    
    // ✅ Use edit{} for writes
    suspend fun setSpoofedIMEI(imei: String) {
        dataStore.edit { it[Keys.IMEI] = imei }
    }
    
    // ✅ Check app scope
    suspend fun isAppEnabled(packageName: String): Boolean {
        val apps = dataStore.data.first()[Keys.ENABLED_APPS] ?: emptySet()
        return packageName in apps
    }
}
```

---

## 🎨 UI BEST PRACTICES (Material 3 Expressive)

### Theme Setup

```kotlin
@Composable
fun PrivacyShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // ✅ Dynamic colors on Android 12+
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context).copy(
                background = Color.Black,  // AMOLED
                surface = Color(0xFF0A0A0A)
            ) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF0A0A0A)
        )
        else -> lightColorScheme()
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
```

### State Hoisting Pattern

```kotlin
// ✅ Stateless composable (hoisted state)
@Composable
fun SpoofValueCard(
    label: String,
    value: String,
    onRegenerate: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.bodyLarge)
            Row {
                IconButton(onClick = onRegenerate) {
                    Icon(Icons.Default.Refresh, "Regenerate")
                }
            }
        }
    }
}

// ✅ Stateful wrapper in Screen
@Composable
fun SpoofSettingsScreen(viewModel: SpoofViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LazyColumn {
        item {
            SpoofValueCard(
                label = "IMEI",
                value = uiState.imei,
                onRegenerate = { viewModel.regenerateIMEI() },
                onEdit = { viewModel.setIMEI(it) }
            )
        }
    }
}
```

### Spring Animations (M3 Expressive)

```kotlin
// ✅ Use spring animations for natural feel
@Composable
fun AnimatedCard(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f)) +
                scaleIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
        exit = fadeOut() + scaleOut()
    ) {
        content()
    }
}
```

---

## 🏗️ ARCHITECTURE BEST PRACTICES

### ViewModel with UDF

```kotlin
class SpoofViewModel(
    private val spoofRepository: SpoofRepository
) : ViewModel() {
    
    // ✅ Single immutable UI state
    private val _uiState = MutableStateFlow(SpoofUiState())
    val uiState: StateFlow<SpoofUiState> = _uiState.asStateFlow()
    
    init {
        loadSpoofedValues()
    }
    
    // ✅ Event handlers update state
    fun regenerateIMEI() {
        viewModelScope.launch {
            val newImei = IMEIGenerator.generate()
            spoofRepository.setIMEI(newImei)
            _uiState.update { it.copy(imei = newImei) }
        }
    }
    
    private fun loadSpoofedValues() {
        viewModelScope.launch {
            val imei = spoofRepository.getIMEI()
            val serial = spoofRepository.getSerial()
            _uiState.update { it.copy(imei = imei, serial = serial) }
        }
    }
}

// ✅ Immutable data class for UI state
data class SpoofUiState(
    val imei: String = "",
    val serial: String = "",
    val isLoading: Boolean = false
)
```

---

## 📋 QUICK REFERENCE CHECKLIST

### Hook Development
- [ ] Use `@InjectYukiHookWithXposed` annotation
- [ ] Load anti-detection hooker FIRST
- [ ] Use `optional()` for uncertain methods
- [ ] Check `packageName` before hooking
- [ ] Use `after {}` block for result modification

### Anti-Detection
- [ ] Filter stack traces (Thread + Throwable)
- [ ] Block Class.forName() for Xposed classes
- [ ] Hide from /proc/maps reads
- [ ] Minimize hook overhead

### Value Generation
- [ ] IMEI: 15 digits, Luhn-valid
- [ ] MAC: Unicast bit cleared
- [ ] Fingerprint: Realistic format
- [ ] Serial: Alphanumeric, device-appropriate length

### UI/UX
- [ ] Use MaterialTheme.colorScheme (not hardcoded)
- [ ] Implement state hoisting
- [ ] Use spring animations
- [ ] Support dynamic colors

### Data
- [ ] Use DataStore (not SharedPreferences)
- [ ] Immutable state classes
- [ ] Coroutines for async operations

---

## 🔗 EXTERNAL MODULES INTEGRATION

```
┌─────────────────────────────────────────────────────────┐
│                    YOUR MODULE                           │
│               (PrivacyShield)                            │
│                                                          │
│  • Device Spoofing                                       │
│  • Anti-Hook Detection                                   │
└─────────────────────────────────────────────────────────┘
                          │
                          │ Works alongside (user installs separately)
                          ▼
┌───────────────┬───────────────┬───────────────────────────┐
│   Shamiko     │     PIF       │    Tricky Store           │
│  (Root Hide)  │ (Play Integ)  │  (HW Attestation)         │
└───────────────┴───────────────┴───────────────────────────┘
```

---

**Document Version:** 1.0  
**Last Updated:** December 14, 2025
