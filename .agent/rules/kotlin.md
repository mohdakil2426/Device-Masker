---
trigger: always_on
---

# Kotlin Development Rules
# Official Best Practices & Conventions

## Kotlin Versions (Latest - Dec 2025)

| Version | Type | Release Date | Notes |
|---------|------|--------------|-------|
| **2.2.21** | Stable (Tooling) | Oct 23, 2025 | Xcode 26 support, bug fixes |
| **2.2.0** | Language Release | Jun 23, 2025 | Major language features |
| **2.3.0-RC3** | Preview | Dec 2025 | Upcoming language release |

### Release Types
- **Language (2.x.0)** - Major changes, every 6 months
- **Tooling (2.x.20)** - Performance/tooling, 3 months after language
- **Bug Fix (2.x.yz)** - Bug fixes, as needed

---

## Gradle Setup

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// build.gradle.kts (project)
plugins {
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("com.google.devtools.ksp") version "2.2.21-2.0.4" apply false
}

// libs.versions.toml
[versions]
kotlin = "2.2.21"
ksp = "2.2.21-2.0.4"  # Must match Kotlin version

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

---

## Naming Conventions

### Packages
```kotlin
// ✅ Lowercase, no underscores
package org.example.project
package com.akil.privacyshield

// ❌ Avoid
package org.example.my_project  // No underscores
package org.example.MyProject   // No uppercase
```

### Classes & Objects
```kotlin
// ✅ Upper camel case (PascalCase)
class DeviceHooker
object NetworkManager
interface DataRepository
data class UserProfile(val name: String)
```

### Functions & Properties
```kotlin
// ✅ Lower camel case
fun processUser() { }
val userName: String
var isEnabled: Boolean

// ❌ Avoid
fun ProcessUser() { }  // Not PascalCase for functions
val user_name: String  // No underscores
```

### Constants
```kotlin
// ✅ Upper snake case for compile-time constants
const val MAX_COUNT = 10
val DEFAULT_NAME = "Unknown"  // Runtime constant - camelCase OK

// ✅ Object properties
object Config {
    const val API_VERSION = "1.0"
}
```

### Backing Properties
```kotlin
// ✅ Underscore prefix for backing properties
private val _items = mutableListOf<String>()
val items: List<String> get() = _items
```

---

## Idiomatic Kotlin

### Data Classes (DTOs)
```kotlin
// ✅ Use data class for DTOs
data class User(
    val id: Long,
    val name: String,
    val email: String? = null
)
// Provides: equals(), hashCode(), toString(), copy(), componentN()
```

### Immutability
```kotlin
// ✅ Prefer val over var
val name = "John"           // Immutable
var count = 0               // Only when mutation needed

// ✅ Use immutable collections
val items: List<String> = listOf("a", "b")      // ✅
val items: MutableList<String> = mutableListOf() // Only when needed
```

### Null Safety
```kotlin
// ✅ Safe call operator
val length = name?.length

// ✅ Elvis operator for defaults
val length = name?.length ?: 0

// ✅ let for null checks
name?.let { println(it) }

// ❌ Avoid !! unless absolutely necessary
val length = name!!.length  // Can throw NPE
```

### String Templates
```kotlin
// ✅ Use string templates
val message = "Hello, $name!"
val info = "User: ${user.name}, Age: ${user.age}"

// ❌ Avoid concatenation
val message = "Hello, " + name + "!"
```

### Single-Expression Functions
```kotlin
// ✅ Use expression body when possible
fun double(x: Int): Int = x * 2
fun isValid(name: String): Boolean = name.isNotBlank()

// Transform() with when
fun transform(color: String): Int = when (color) {
    "Red" -> 0
    "Green" -> 1
    "Blue" -> 2
    else -> -1
}
```

### Default Parameter Values
```kotlin
// ✅ Default parameters instead of overloads
fun createUser(
    name: String,
    email: String = "",
    isActive: Boolean = true
) { }

// ❌ Avoid multiple overloads
fun createUser(name: String) { }
fun createUser(name: String, email: String) { }
```

### Named Arguments
```kotlin
// ✅ Use named arguments for clarity
createUser(
    name = "John",
    email = "john@example.com",
    isActive = true
)

// ✅ Especially for Boolean parameters
setEnabled(isEnabled = true)
```

---

## Control Flow

### if vs when
```kotlin
// ✅ Use if for binary conditions
val result = if (x > 0) "positive" else "non-positive"

// ✅ Use when for 3+ options
val result = when (x) {
    1 -> "one"
    2 -> "two"
    else -> "other"
}

// ✅ when as expression
val action = when {
    x.isOdd() -> "odd"
    x.isEven() -> "even"
    else -> "unknown"
}
```

### Loops & Collections
```kotlin
// ✅ Prefer higher-order functions
val filtered = items.filter { it.isActive }
val names = users.map { it.name }
val sum = numbers.sum()

// ✅ Use for loop over forEach when simpler
for (item in items) { process(item) }

// ✅ Range iteration
for (i in 0..<items.size) { }  // Exclusive end
for (i in items.indices) { }   // Same as above
for ((index, value) in items.withIndex()) { }
```

---

## Scope Functions

### apply - Configure object
```kotlin
val user = User().apply {
    name = "John"
    email = "john@example.com"
}
```

### let - Transform nullable
```kotlin
val length = name?.let { it.length } ?: 0
user?.let { saveToDatabase(it) }
```

### run - Execute with receiver
```kotlin
val result = user.run {
    "$name: $email"
}
```

### with - Group calls on object
```kotlin
with(user) {
    println(name)
    println(email)
}
```

### also - Additional effects
```kotlin
val user = createUser().also {
    logger.info("Created: $it")
}
```

---

## Coroutines Best Practices

### Structured Concurrency
```kotlin
// ✅ Use structured concurrency
suspend fun loadData() = coroutineScope {
    val users = async { fetchUsers() }
    val posts = async { fetchPosts() }
    Pair(users.await(), posts.await())
}

// ❌ Avoid GlobalScope
GlobalScope.launch { }  // Unstructured, hard to cancel
```

### Dispatchers
```kotlin
// IO operations
withContext(Dispatchers.IO) {
    readFile()
}

// CPU-intensive work
withContext(Dispatchers.Default) {
    processData()
}

// UI updates (Android)
withContext(Dispatchers.Main) {
    updateUI()
}
```

### Exception Handling
```kotlin
// ✅ Use supervisorScope for independent children
supervisorScope {
    launch { task1() }  // Failure doesn't cancel task2
    launch { task2() }
}

// ✅ Try-catch in coroutines
launch {
    try {
        riskyOperation()
    } catch (e: Exception) {
        handleError(e)
    }
}
```

### Flow
```kotlin
// ✅ Cold stream with Flow
fun getUsers(): Flow<User> = flow {
    users.forEach { emit(it) }
}

// ✅ Collect in lifecycle-aware scope
viewModelScope.launch {
    getUsers().collect { user -> process(user) }
}

// ✅ StateFlow for state management
private val _state = MutableStateFlow(UiState())
val state: StateFlow<UiState> = _state.asStateFlow()
```

---

## Formatting Rules

### Modifiers Order
```kotlin
public / private / protected / internal
expect / actual
final / open / abstract / sealed / const
external
override
lateinit
tailrec
vararg
suspend
inner
enum / annotation / fun (as modifier)
companion
inline / value
infix
operator
data
```

### Annotations
```kotlin
// ✅ Separate line for annotations
@Inject
lateinit var repository: Repository

// ✅ Single annotation can be on same line
@Volatile var running = false
```

### Trailing Commas
```kotlin
// ✅ Use trailing commas in multiline
data class User(
    val name: String,
    val email: String,  // Trailing comma
)

enum class Status {
    ACTIVE,
    INACTIVE,  // Trailing comma
}
```

---

## Android Specific

### ViewModel
```kotlin
class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }
}
```

### Compose
```kotlin
@Composable
fun UserCard(
    user: User,
    onClickz() -> Unit,
    modifier: Modifier = Modifier  // Modifier last with default
) {
    Card(modifier = modifier) {
        Text(text = user.name)
    }
}
```

### Extension Functions
```kotlin
// ✅ Use for utility functions
fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
```

---

## What to Avoid

```kotlin
// ❌ Mutable public state
var items = mutableListOf<String>()  // Expose immutable instead

// ❌ Platform types leaking
fun getItems(): List<String>? = javaMethod()  // Handle nullability

// ❌ Unnecessary non-null assertions
val length = name!!.length  // Use safe calls instead

// ❌ Deep nesting
if (a) { if (b) { if (c) { } } }  // Use when or early returns

// ❌ Magic numbers
delay(5000)  // Use named constants

// ❌ Redundant Unit return type
fun doSomething(): Unit { }  // Omit Unit
```

---

## Official Resources

### Kotlin 2.3.0 Guide always read this first for targeted Quary

@/docs/developer-android-docs/kotlin-2-3-0-guide.md

- [Kotlin Documentation](https://kotlinlang.org/docs/)