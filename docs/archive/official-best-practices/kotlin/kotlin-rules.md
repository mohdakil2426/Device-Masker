---
trigger: manual
---

# Kotlin 2.3.0 Development Rules

## Version Info
| Version | Type | Notes |
|---------|------|-------|
| **2.3.0** | Latest | Java 25, Gradle 9.0.0 |
| **2.2.21** | Stable Tooling | Xcode 26/bug fixes |

---

## Gradle Setup
```kotlin
// libs.versions.toml
[versions]
kotlin = "2.3.0"
ksp = "2.3.0-2.0.4"  # Match Kotlin version

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

---

## Core Principles

### 1. Immutability First
```kotlin
// ✅ Default to val - thread-safe, predictable
val userName = "John"
val config: Map<String, String> = mapOf("key" to "value")

// ❌ Avoid var unless mutation required
var count = 0  // Only when MUST change, document why
```

### 2. Expression-Based Code
```kotlin
// ✅ Expression body - concise, declarative
fun isAdult(age: Int): Boolean = age >= 18
fun transform(color: String): Int = when (color) {
    "Red" -> 0; "Green" -> 1; "Blue" -> 2; else -> -1
}

// ❌ Imperative style
fun isAdult(age: Int): Boolean { return age >= 18 }
```

### 3. Kotlin Idioms Over Java Patterns
```kotlin
// ✅ Kotlin idioms
val msg = "User: $name, Age: ${user.age}"
val display = user.nickname ?: "Anonymous"
when (obj) { is String -> println(obj.uppercase()) }
for (i in 0..<items.size) { }

// ❌ Java style
val msg = "User: " + name + ", Age: " + user.age
if (user.nickname != null) user.nickname else "Anonymous"
```

### 4. Type Inference
```kotlin
// ✅ Local scope - inference OK
val result = calculateTotal()

// ✅ Public API - explicit types required
fun getUser(): User { ... }
fun formatDate(date: LocalDate): String = ...
```

---

## Naming Conventions

| Type | Style | Example |
|------|-------|---------|
| Package | lowercase | `com.example.project` |
| Class/Interface | PascalCase | `UserRepository`, `DataSource` |
| Function/Variable | camelCase | `getUserName()`, `val userAge` |
| Constant | UPPER_SNAKE | `const val MAX_COUNT = 10` |
| Backing Property | _underscore | `private val _items` |
| Test Methods | backticks | `` `should return user when valid`() `` |

### Acronyms
- 2-letter: BOTH upper → `IOStream`, `URLPath`
- 3+ letter: First upper → `XmlParser`, `HttpClient`

---

## Null Safety ⚡

```kotlin
// ✅ Safe calls & elvis
val length = user?.name?.length ?: 0
val name = user?.name ?: "Unknown"

// ✅ let for null handling
user?.let { database.save(it) }
user?.takeIf { it.isVerified }?.let { sendEmail(it) }

// ✅ Smart cast after check
if (obj is String) println(obj.length)
when (value) {
    is String -> println(value.length)
    is Int -> println(value + 1)
}

// 🚫 NEVER use !! in production (NPE risk)
val length = name!!.length
```

---

## Collections

```kotlin
// ✅ Immutable by default
val users: List<User> = listOf(...)
val settings: Map<String, String> = mapOf("key" to "value")

// ✅ Return immutable from internal mutable
private val _users = mutableListOf<User>()
fun getUsers(): List<User> = _users.toList()

// ✅ Functional operations over loops
val doubled = numbers.map { it * 2 }
val adults = users.filter { it.age >= 18 }
val total = items.fold(0) { sum, item -> sum + item.price }

// ✅ Sequences for large data (lazy evaluation)
val result = list.asSequence()
    .filter { it > 10 }
    .map { it * 2 }
    .take(5)
    .toList()
```

---

## Functions

```kotlin
// ✅ Default parameters > overloads
fun connect(host: String = "localhost", port: Int = 8080, timeout: Long = 5000L) {}

// ✅ Named arguments for clarity
loadData(ignoreCache = true, validateSchema = false, retryOnFailure = true)

// ✅ Extension functions for utility
fun String.isValidEmail(): Boolean = contains("@") && contains(".")
fun Context.showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

// ✅ Lambda parameter naming
list.fold(0) { acc, num -> acc + num }  // Explicit for multiple params
users.groupBy { it.dept }.mapValues { (_, users) -> users.filter { it.active } }

// ✅ Tail recursion
tailrec fun factorial(n: Int, acc: Int = 1): Int =
    if (n <= 1) acc else factorial(n - 1, n * acc)
```

---

## Scope Functions

| Function | Context | Returns | Use Case |
|----------|---------|---------|----------|
| `let` | `it` | Lambda result | Null handling, transform |
| `apply` | `this` | Context object | Configure object |
| `run` | `this` | Lambda result | Configure + result |
| `also` | `it` | Context object | Side effects, logging |
| `with` | `this` | Lambda result | Multiple operations |

```kotlin
// apply - configure object
val user = User().apply { name = "John"; email = "j@e.com" }

// let - null handling
user?.let { saveToDatabase(it) }

// also - side effects
val user = createUser().also { logger.info("Created: $it") }

// run - configure + result
val result = user.run { "$name: $email" }
```

---

## Coroutines

```kotlin
// ✅ Always use scoped coroutines
viewModelScope.launch { val user = fetchUser() }
lifecycleScope.launch { observeData() }

// 🚫 NEVER GlobalScope (memory leak risk)
GlobalScope.launch { }

// ✅ Proper dispatchers
withContext(Dispatchers.IO) { readFile() }      // I/O operations
withContext(Dispatchers.Default) { process() }  // CPU-intensive
withContext(Dispatchers.Main) { updateUI() }    // UI updates

// ✅ StateFlow for state management
private val _user = MutableStateFlow<User?>(null)
val user: StateFlow<User?> = _user.asStateFlow()

// ✅ Flow for streams
fun getUsers(): Flow<User> = flow { users.forEach { emit(it) } }

// ✅ Exception handling
viewModelScope.launch {
    try { riskyOperation() }
    catch (e: IOException) { _error.value = e.message }
}

// ✅ supervisorScope for independent children
supervisorScope {
    launch { task1() }  // Failure doesn't cancel task2
    launch { task2() }
}
```

---

## Classes & OOP

```kotlin
// ✅ Data class for DTOs (auto equals, hashCode, toString, copy)
data class User(val id: Int, val name: String, val email: String?)
val updated = user.copy(name = "Jane")

// ✅ Sealed class for restricted hierarchies
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val e: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// ✅ Value class for type safety (no runtime overhead)
@JvmInline
value class UserId(val value: Int)
fun getUser(id: UserId): User = ...

// ✅ Delegation over inheritance
class CachedDb(private val impl: Database) : Database by impl {
    override fun load(id: Int) = cache[id] ?: impl.load(id)
}

// ✅ Object for singletons
object AppConfig {
    const val API_URL = "https://api.example.com"
}

// ✅ Explicit backing fields (Kotlin 2.3.0)
var items: List<String>
    field = mutableListOf()  // Internal mutable, public immutable
```

---

## Exception Handling

```kotlin
// ✅ Specific exceptions, not generic
try { getUserFromDb(id) }
catch (e: SQLException) { throw DataAccessException("Failed", e) }
catch (e: IllegalArgumentException) { throw InvalidIdException(e) }

// ✅ Fail fast with require/check
fun createUser(name: String, email: String): User {
    require(name.isNotBlank()) { "Name cannot be blank" }
    require(email.contains("@")) { "Email must be valid" }
    return User(generateId(), name, email)
}

// ✅ Sealed Result for type-safe error handling
when (result) {
    is Result.Success -> handleSuccess(result.data)
    is Result.Error -> handleError(result.exception)
}

// ✅ use for resource cleanup
File("data.txt").bufferedReader().use { it.readText() }
```

---

## Performance

```kotlin
// ✅ Inline for lambda-heavy functions
inline fun <T> measure(block: () -> T): Long { ... }

// ✅ Reified type parameters
inline fun <reified T> fromJson(json: String): T = ...

// ✅ Lazy initialization
val expensive: Resource by lazy { loadResource() }

// ✅ Avoid object creation in loops
val regex = Regex("\\d+")  // Create once
for (str in strings) { str.matches(regex) }

// ✅ Primitive arrays for performance
val nums = intArrayOf(1, 2, 3)  // No boxing
```

---

## Memory & Lifecycle

```kotlin
// ✅ Unregister listeners
override fun onDestroy() {
    sensorManager.unregisterListener(listener)
    eventBus.unregister(this)
}

// ✅ Use lifecycle-aware components
viewModelScope.launch { }  // Auto-cancelled

// 🚫 NEVER static Context references
object Globals { var context: Context? = null }  // Memory leak!
```

---

## Kotlin 2.3.0 Breaking Changes

| Change | Action |
|--------|--------|
| No `-language-version=1.8/1.9` (non-JVM) | Update to 2.0+ syntax |
| iOS/tvOS min raised to 14.0/7.0 | Configure compatibility |
| Intel Apple targets deprecated | Migrate to ARM64 |
| AGP 9.0.0 built-in Kotlin | Remove `kotlin-android` plugin |
| Explicit backing fields | `field = mutableListOf()` |

---

## Formatting

```kotlin
// ✅ 4-space indent, max 100 chars
// ✅ Spaces: a + b, 0..10 (no space), if (x)
// ✅ Trailing commas
data class User(
    val name: String,
    val email: String,
)

// ✅ Chained calls: dot on new line
val result = users
    .filter { it.active }
    .map { it.name }
```

### Modifier Order
`public/private` → `final/open/abstract/sealed` → `override` → `lateinit` → `suspend` → `inline` → `data`

---

## 🚫 Forbidden Practices

| Practice | Why |
|----------|-----|
| `!!` in production | NPE crash risk |
| `GlobalScope` | Memory leaks |
| Mutable global state | Thread-unsafe |
| Swallowing exceptions | Silent failures |
| Static Context refs | Memory leaks |
| `var` for constants | Unexpected mutation |
| Deep nesting (3+) | Unreadable |
| Mutable default args | Shared state bugs |
| Try-catch control flow | Performance |
| Platform types | Null ambiguity |

---

## Quick Reference

| Use | For |
|-----|-----|
| `data class` | Data containers |
| `sealed class` | Fixed type hierarchies |
| `object` | Singletons |
| `inline` | Lambda-heavy functions |
| `lazy` | Expensive one-time init |
| `Flow` | Multiple values over time |
| `StateFlow` | Mutable reactive state |
| `require`/`check` | Preconditions |

---

## Official Resources

**Always read before generating code:**
@/docs/official-best-practices/kotlin/kotlin-2-3-0-guide.md

- [Kotlin Docs](https://kotlinlang.org/docs/)
- [What's New 2.3.0](https://kotlinlang.org/docs/whatsnew23.html)
- [Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
