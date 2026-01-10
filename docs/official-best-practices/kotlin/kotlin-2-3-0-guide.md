# Kotlin 2.3.0 Best Practices & Rules Guide for AI/LLM Assistance

**Version**: 1.0  
**Kotlin Version**: 2.3.0  
**Date**: December 2025  
**Purpose**: Comprehensive coding rules guide for AI-assisted development  

---

## Table of Contents

1. [Core Language Principles](#core-language-principles)
2. [Code Organization & Structure](#code-organization--structure)
3. [Naming Conventions](#naming-conventions)
4. [Formatting & Style](#formatting--style)
5. [Type Safety & Null Handling](#type-safety--null-handling)
6. [Immutability & Collections](#immutability--collections)
7. [Functions & Higher-Order Functions](#functions--higher-order-functions)
8. [Classes & Object-Oriented Design](#classes--object-oriented-design)
9. [Scope Functions & Context](#scope-functions--context)
10. [Coroutines & Asynchrony](#coroutines--asynchrony)
11. [Memory Management & Lifecycle](#memory-management--lifecycle)
12. [Performance Optimization](#performance-optimization)
13. [Testing & Verification](#testing--verification)
14. [Exception Handling & Error Management](#exception-handling--error-management)
15. [DSL & Extension Functions](#dsl--extension-functions)
16. [Architecture Patterns](#architecture-patterns)
17. [Kotlin 2.3.0 Breaking Changes](#kotlin-230-breaking-changes)
18. [Documentation & Comments](#documentation--comments)

---

## Core Language Principles

### Principle 1: Favor Immutability
**RULE**: Default to `val` over `var` for all variables and properties.

**WHY**: Immutable data is thread-safe, predictable, and easier to reason about. Mutable state is a source of bugs.

✅ **CORRECT**:
```kotlin
val userName: String = "John"
val userAge = 30
val config: Map<String, String> = mapOf("key" to "value")
```

❌ **WRONG**:
```kotlin
var userName: String = "John"  // Unnecessary mutability
var userAge = 30  // Can be reassigned unexpectedly
```

**Exception**: Use `var` only when state MUST change, and always document why.

---

### Principle 2: Prefer Expression-Based Code Over Statements
**RULE**: Use expression bodies and functional constructs instead of imperative statements.

**WHY**: Expression-based code is concise, declarative, and often more efficient.

✅ **CORRECT**:
```kotlin
fun isAdult(age: Int): Boolean = age >= 18

val doubled = numbers.map { it * 2 }.filter { it > 10 }

val status = when {
    count > 100 -> "Large"
    count > 10 -> "Medium"
    else -> "Small"
}
```

❌ **WRONG**:
```kotlin
fun isAdult(age: Int): Boolean {
    return age >= 18
}

val doubled = mutableListOf<Int>()
for (num in numbers) {
    val d = num * 2
    if (d > 10) {
        doubled.add(d)
    }
}
```

---

### Principle 3: Leverage Type Inference When Safe
**RULE**: Let the compiler infer types when context is clear. Explicitly declare types in public APIs and complex scenarios.

**WHY**: Reduces boilerplate without sacrificing clarity. Public APIs need explicit types for documentation.

✅ **CORRECT**:
```kotlin
// Local scope - inference is safe
val result = calculateTotal()  // Type clear from function return

// Public API - explicit type declaration
fun getUser(): User { ... }
val users: List<User> = loadUsers()  // Complex type, explicit declaration
```

❌ **WRONG**:
```kotlin
// Over-explicit in local scope
val result: Double = calculateTotal()
val name: String = "John"

// Public API missing types
fun getUser() { ... }  // What's returned?
```

---

### Principle 4: Use Kotlin Idioms, Not Java Patterns
**RULE**: Write Kotlin-idiomatic code. Avoid translating Java patterns directly.

**WHY**: Kotlin idioms are designed for the language's strengths and often produce better performance.

✅ **CORRECT**:
```kotlin
// String templates instead of concatenation
val message = "User: $name, Age: $age"

// Elvis operator for defaults
val displayName = user.nickname ?: "Anonymous"

// Smart casts in when expressions
when (obj) {
    is String -> println(obj.uppercase())
    is Int -> println(obj + 1)
}

// Range operators
for (i in 1..10) { ... }
for (i in 1 until 10) { ... }
```

❌ **WRONG**:
```kotlin
// Java-style string concatenation
val message = "User: " + name + ", Age: " + age

// Verbose null checks
val displayName = if (user.nickname != null) user.nickname else "Anonymous"

// No smart casting
when (obj) {
    is String -> println((obj as String).uppercase())
}
```

---

## Code Organization & Structure

### Rule 1: Directory Structure Mirrors Package Hierarchy
**RULE**: Organize files to match package structure. Package names are lowercase, no underscores.

```
src/main/kotlin/
├── com/
│   └── example/
│       ├── feature1/
│       │   ├── Feature1Activity.kt
│       │   ├── Feature1ViewModel.kt
│       │   └── Feature1Repository.kt
│       └── feature2/
│           ├── Feature2Screen.kt
│           └── Feature2Logic.kt
```

**RULE**: For multiplatform projects, use platform-specific suffixes:
- `src/jvmMain/kotlin/Platform.jvm.kt`
- `src/androidMain/kotlin/Platform.android.kt`
- `src/iosMain/kotlin/Platform.ios.kt`
- `src/commonMain/kotlin/Platform.kt` (no suffix for common)

---

### Rule 2: Single Responsibility Per File
**RULE**: A file should contain one public class/interface/object, or closely related top-level declarations.

**WHY**: Easier to locate code, improves maintainability.

✅ **CORRECT**:
```kotlin
// User.kt
data class User(val id: Int, val name: String)

fun User.isAdult(): Boolean = age >= 18
fun User.displayName(): String = name.uppercase()
```

❌ **WRONG**:
```kotlin
// Util.kt (meaningless name)
data class User(...)
class Product(...)
object Config { ... }
fun validateEmail(...) { ... }
```

---

### Rule 3: Organize Class Members in Specific Order
**RULE**: Follow this member ordering in classes:

1. Property declarations and initializer blocks
2. Secondary constructors
3. Method declarations
4. Companion object
5. Nested classes (if meant for external use)

```kotlin
class User(val id: Int, val name: String) {
    // Properties first
    private val internalList = mutableListOf<String>()
    
    init {
        // Initializer blocks after properties
        println("User created: $name")
    }
    
    // Secondary constructors
    constructor(name: String) : this(0, name)
    
    // Methods
    fun getName(): String = name
    
    // Companion object last
    companion object {
        const val MIN_AGE = 18
    }
}
```

---

### Rule 4: Extension Functions Organization
**RULE**: Define extension functions in the same file as the class they extend, or in a dedicated extensions file.

**WHY**: Keeps related functionality together, improves discoverability.

✅ **CORRECT**:
```kotlin
// User.kt
data class User(val id: Int, val name: String)

// Extension functions in same file
fun User.isAdult(): Boolean = age >= 18
fun User.greet(): String = "Hello, ${name.capitalize()}"
```

---

## Naming Conventions

### Rule 1: Packages - Lowercase, No Underscores
**RULE**: `com.example.project` or `com.example.myproject`

❌ **WRONG**: `com.example.My_Project`, `COM.EXAMPLE.PROJECT`

---

### Rule 2: Classes & Interfaces - Upper Camel Case
**RULE**: `class UserRepository`, `interface DataSource`, `object Singleton`

```kotlin
class UserAuthenticator { ... }
interface UserRepository { ... }
object AppConfig { ... }
enum class Priority { LOW, MEDIUM, HIGH }
```

---

### Rule 3: Functions & Variables - Lower Camel Case
**RULE**: `fun getUserName()`, `val userAge = 30`

```kotlin
fun calculateTotalPrice(items: List<Item>): Double { ... }
fun String.toUpperCaseFirst(): String = ...
val maximumRetries = 3
var currentUser: User? = null
```

❌ **WRONG**: `fun getUserName()` ❌ `fun get_user_name()` ❌ `fun GetUserName()`

---

### Rule 4: Constants - UPPER_SNAKE_CASE
**RULE**: Top-level and object `val` holding immutable data.

```kotlin
const val MAX_USERS = 100
const val API_BASE_URL = "https://api.example.com"
const val DEFAULT_TIMEOUT_MS = 5000L

object Config {
    const val APP_VERSION = "1.0.0"
}
```

**NOT constants** (use camelCase):
```kotlin
val userRepository = UserRepository()  // Object with behavior
val mutableConfig = mutableMapOf<String, String>()  // Mutable data
```

---

### Rule 5: Enum Constants - Follow Context
**RULE**: Use either UPPER_CASE or UpperCamelCase, consistently.

```kotlin
// Option 1: UPPER_CASE (typical for traditional enums)
enum class Priority { CRITICAL, HIGH, MEDIUM, LOW }

// Option 2: UpperCamelCase (acceptable for descriptive names)
enum class LogLevel { Debug, Info, Warning, Error }
```

---

### Rule 6: Backing Properties - Prefix with Underscore
**RULE**: Use underscore prefix to distinguish private backing properties.

```kotlin
private var _users: MutableList<User> = mutableListOf()
val users: List<User> = _users  // Exposed as immutable

// Or use Kotlin 2.3.0 explicit backing fields (experimental)
var items: List<String>
    field = mutableListOf()  // Internal mutable, public immutable
```

---

### Rule 7: Test Method Names - Use Backticks & Spaces (JUnit5)
**RULE**: Test method names can use spaces in backticks for clarity.

```kotlin
class UserServiceTest {
    @Test
    fun `should return user when valid id provided`() { ... }
    
    @Test
    fun `should throw exception when user not found`() { ... }
}
```

---

### Rule 8: Factory Functions - Distinct Names
**RULE**: Avoid naming factory functions same as the class.

✅ **CORRECT**:
```kotlin
data class Config(val key: String, val value: String)

fun defaultConfig(): Config = Config("default", "")
fun configFromJson(json: String): Config = ...
```

❌ **WRONG**:
```kotlin
fun Config(json: String): Config = ...  // Confusing, looks like constructor
```

---

### Rule 9: Acronyms - Specific Capitalization Rules
**RULE**:
- 2-letter acronyms: BOTH uppercase (`IOStream`, `URLPath`)
- 3+ letter acronyms: Only first uppercase (`XmlParser`, `HttpClient`)

```kotlin
class IOStream { ... }
class URLValidator { ... }
class XmlFormatter { ... }
class HttpInputStream { ... }
```

❌ **WRONG**: `class ioStream`, `class urlValidator`, `class XMLFormatter`, `class HTTPClient`

---

## Formatting & Style

### Rule 1: Indentation - 4 Spaces
**RULE**: Use 4 spaces for all indentation. Never use tabs.

```kotlin
class User {
    fun getName(): String {
        return name
    }
}
```

---

### Rule 2: Line Length - Maximum 100 Characters
**RULE**: Keep lines under 100 characters for readability.

**Action**: When line exceeds limit, break at logical points.

```kotlin
// Break function parameters
fun createUser(
    name: String,
    email: String,
    age: Int
): User = ...

// Break long expressions
val totalPrice = items
    .map { it.price * it.quantity }
    .sum()
```

---

### Rule 3: Spaces Around Operators
**RULE**: 
- Binary operators: spaces around (`a + b`)
- Unary operators: no spaces (`a++`, `-b`)
- Range operator: no spaces (`0..10`)

```kotlin
val sum = a + b  // Correct
val negated = -value  // Correct
val incremented = i++  // Correct
for (i in 0..10) { ... }  // Correct
for (i in 0 .. 10) { ... }  // Wrong
```

---

### Rule 4: Spaces in Keywords & Parentheses
**RULE**:
- Space after keywords: `if (condition)`, `for (item in list)`
- No space before method parentheses: `method()` not `method ()`
- No space inside parentheses: `func(a, b)` not `func( a, b )`

```kotlin
// Correct
if (age > 18) { ... }
when (status) { ... }
for (user in users) { ... }
method(a, b, c)

// Wrong
if(age > 18) { ... }
when(status) { ... }
method ( a, b, c )
```

---

### Rule 5: Spaces Around Dot Operator
**RULE**: No spaces around `.` or `?.`

```kotlin
user.getName().toUpperCase()  // Correct
user?.address?.city  // Correct
user . getName ( )  // Wrong
```

---

### Rule 6: Colons - Spaces After, Not Before
**RULE**:
- Space AFTER `:` in type declarations
- Space BEFORE `:` for inheritance/delegation
- No space in type annotations

```kotlin
val name: String = "John"  // Correct
fun getName(): String = ...  // Correct
class User : Person() { ... }  // Correct
class User(val name: String) : Person() { ... }  // Correct

val name:String = "John"  // Wrong - missing space
fun getName( ): String = ...  // Wrong - extra space
class User: Person() { ... }  // Wrong - missing space before
```

---

### Rule 7: Class Header Formatting
**RULE**: For short class headers, keep on one line. For long headers, format carefully.

```kotlin
// Short - one line
class User(val id: Int, val name: String)

// Long - format with each parameter on new line
class ComplexClass(
    val parameter1: Type1,
    val parameter2: Type2,
    val parameter3: Type3
) : SuperClass(parameter1), Interface1, Interface2 {
    // Body
}

// For super long inheritance list
class MyClass(val value: String) :
    Interface1,
    Interface2,
    Interface3 {
    // Body
}
```

---

### Rule 8: Function Formatting
**RULE**: Single-line if fits, otherwise break at parameters.

```kotlin
// Short function
fun calculateTotal(a: Int, b: Int): Int = a + b

// Long function
fun processLargeDataSet(
    data: List<String>,
    processor: (String) -> String,
    onComplete: () -> Unit
): List<String> {
    return data.map(processor).also { onComplete() }
}

// Expression body with long expression
fun buildComplexQuery() =
    database.query()
        .where { it.active }
        .orderBy { it.date }
        .limit(10)
```

---

### Rule 9: Lambda Formatting
**RULE**: 
- Single lambda: pass outside parentheses
- Multiline lambda: break parameters on first line, arrow after

```kotlin
// Single-line lambda outside parentheses
list.filter { it > 5 }

// Multiple parameters - explicit names
list.fold(0) { accumulator, item ->
    accumulator + item
}

// Multiline lambda
val result = users.map { user ->
    User(
        id = user.id,
        name = user.name.uppercase()
    )
}
```

---

### Rule 10: Trailing Commas
**RULE**: Use trailing commas in multiline declarations to reduce diff noise.

```kotlin
// Recommended
val list = listOf(
    "item1",
    "item2",
    "item3",  // Trailing comma
)

// Acceptable but less clean
val list = listOf(
    "item1",
    "item2",
    "item3"
)
```

---

### Rule 11: Chained Calls - Dot on New Line
**RULE**: When wrapping chained calls, put `.` or `?.` at the start of the new line.

```kotlin
// Correct
val result = users
    .filter { it.active }
    .map { it.name }
    .sorted()

// Wrong
val result = users.
    filter { it.active }.
    map { it.name }.
    sorted()
```

---

### Rule 12: Blank Lines
**RULE**:
- One blank line between methods
- One blank line between properties and methods
- One blank line between logical sections

```kotlin
class User {
    val id: Int
    val name: String
    
    fun getId(): Int = id
    
    fun getName(): String = name
    
    private fun validate(): Boolean = name.isNotBlank()
}
```

---

## Type Safety & Null Handling

### Rule 1: Avoid the `!!` Operator
**RULE**: Never use `!!` (not-null assertion). It defeats null safety.

**EXCEPTION**: Only in test code where failure is acceptable.

✅ **CORRECT**:
```kotlin
// Safe call operator
val length = user?.name?.length ?: 0

// Let block
user?.let { println(it.name) }

// Elvis operator
val name = user?.name ?: "Unknown"

// Smart cast after null check
if (obj is String) {
    println(obj)  // Smart cast, no cast needed
}
```

❌ **WRONG**:
```kotlin
val length = user!!.name!!.length  // Crashes if null
val name = user.name!!  // NPE waiting to happen
```

---

### Rule 2: Prefer Safe Calls Over Conditionals
**RULE**: Use `?.` operator chain instead of repeated null checks.

✅ **CORRECT**:
```kotlin
val city = user?.address?.city ?: "Unknown"
user?.let { it.sendNotification() }
user?.takeIf { it.isVerified }?.let { ... }
```

❌ **WRONG**:
```kotlin
if (user != null && user.address != null && user.address.city != null) {
    val city = user.address.city
}
```

---

### Rule 3: Elvis Operator for Defaults
**RULE**: Use `?:` to provide default values for nullable types.

```kotlin
val displayName = user.nickname ?: "Guest"
val port = config["port"]?.toInt() ?: 8080
val timeout = settings.timeout ?: Duration.ofSeconds(30)
```

---

### Rule 4: Let Block for Side Effects
**RULE**: Use `let` block to execute code only when value is non-null.

```kotlin
// Transform and pass forward
user?.let { 
    println("User: ${it.name}")
    it.sendWelcomeEmail()
}

// Chain operations
user?.let { u ->
    database.save(u)
}.also { 
    logSuccess()
}
```

---

### Rule 5: Smart Casts in When Expressions
**RULE**: Use `when` with type checking for automatic smart casts.

```kotlin
when (value) {
    is String -> println(value.length)  // No cast needed
    is Int -> println(value + 1)
    is List<*> -> println(value.size)
    else -> println("Unknown type")
}
```

---

### Rule 6: Nullable Return Types - Explicit
**RULE**: Make nullable return types explicit in signatures.

```kotlin
fun findUser(id: Int): User?  // Can return null
fun getUserName(user: User?): String?  // Parameter and return nullable

fun getOrNull(): T? = ...
fun getOrDefault(): T = ...
```

---

### Rule 7: Sealed Classes for Constrained Hierarchies
**RULE**: Use sealed classes to model fixed set of subtypes (alternatives).

**WHY**: Exhaustive `when` expressions without `else`, better type safety.

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Exhaustive when - no else needed
fun handleResult(result: Result<User>) {
    when (result) {
        is Result.Success -> println(result.data.name)
        is Result.Error -> println("Error: ${result.exception.message}")
        Result.Loading -> println("Loading...")
    }
}
```

---

### Rule 8: Use Type Aliases for Complex Types
**RULE**: Create type aliases for complex or frequently used types.

```kotlin
typealias UserRepository = Repository<User>
typealias Callback<T> = (T) -> Unit
typealias ValidationResult = Result<List<String>>

fun saveUser(user: User, callback: Callback<User>) { ... }
```

---

## Immutability & Collections

### Rule 1: Default to Immutable Collections
**RULE**: Use immutable collection types by default (`List`, `Set`, `Map`).

**WHY**: Thread-safe, predictable, can't be accidentally modified.

✅ **CORRECT**:
```kotlin
val users: List<User> = listOf(...)
val settings: Map<String, String> = mapOf("key" to "value")
val tags: Set<String> = setOf(...)

fun getUsers(): List<User> = ...  // Return immutable
```

❌ **WRONG**:
```kotlin
val users: MutableList<User> = mutableListOf(...)  // Unnecessary mutability
fun getUsers(): MutableList<User> = ...  // Dangerous, caller can modify
```

---

### Rule 2: Mutable Collections - Only When Required
**RULE**: Use mutable collections ONLY when state MUST change. Document why.

```kotlin
// Acceptable - building collection incrementally
val results = mutableListOf<String>()
for (item in items) {
    if (item.isValid()) {
        results.add(item.name)
    }
}

// Better - use functional operations
val results = items
    .filter { it.isValid() }
    .map { it.name }
    .toList()
```

---

### Rule 3: Convert Mutable to Immutable Before Returning
**RULE**: When returning collections, convert to immutable type.

```kotlin
private val _users = mutableListOf<User>()

fun getUsers(): List<User> = _users.toList()  // Immutable copy

// Or use backing properties pattern
private val _users = mutableListOf<User>()
val users: List<User> = _users
```

---

### Rule 4: Use Sequences for Large Collections
**RULE**: Use `Sequence` for complex chained operations on large datasets.

**WHY**: Sequences are lazy and avoid creating intermediate collections.

```kotlin
// Wrong - creates intermediate List<Int> and List<Int>
val result = list
    .filter { it > 10 }
    .map { it * 2 }

// Better for large lists - lazy evaluation
val result = list
    .asSequence()
    .filter { it > 10 }
    .map { it * 2 }
    .toList()
```

---

### Rule 5: Avoid Loops - Use Functional Operations
**RULE**: Prefer `map`, `filter`, `fold`, `forEach` over `for` loops.

**EXCEPTION**: `forEach` preferred over `for` when combined with other operations.

✅ **CORRECT**:
```kotlin
// Functional approach
val doubled = numbers.map { it * 2 }
val adults = users.filter { it.age >= 18 }
val total = items.fold(0) { sum, item -> sum + item.price }

// Good use of forEach
users.forEach { it.sendNotification() }
```

❌ **WRONG**:
```kotlin
// Imperative approach
val doubled = mutableListOf<Int>()
for (num in numbers) {
    doubled.add(num * 2)
}
```

---

### Rule 6: Defensive Copying for Exposed Collections
**RULE**: If exposing internal mutable collections, return a copy.

```kotlin
class UserGroup {
    private val members = mutableListOf<User>()
    
    // Expose as immutable
    fun getMembers(): List<User> = members.toList()
    
    // Or use defensive copy
    fun getMembersSnapshot(): List<User> = ArrayList(members)
    
    // Add method for safe modification
    fun addMember(user: User) {
        members.add(user)
    }
}
```

---

## Functions & Higher-Order Functions

### Rule 1: Single Responsibility Per Function
**RULE**: Each function should have one clear purpose.

```kotlin
// Good - single responsibility
fun validateEmail(email: String): Boolean = ...
fun sendEmail(to: String, body: String): Boolean = ...
fun persistUser(user: User): Boolean = ...

// Bad - multiple responsibilities
fun registerUser(user: User, validateEmail: Boolean): Boolean {
    // Validates, sends email, saves to database, logs action
}
```

---

### Rule 2: Default Parameters Over Overloads
**RULE**: Use default parameter values instead of creating multiple overloads.

✅ **CORRECT**:
```kotlin
fun connectDatabase(
    host: String = "localhost",
    port: Int = 5432,
    timeout: Duration = Duration.ofSeconds(30)
) { ... }

// Callers can use defaults
connectDatabase()
connectDatabase("production.example.com")
connectDatabase("prod.example.com", 5432)
```

❌ **WRONG**:
```kotlin
fun connectDatabase(host: String) { ... }
fun connectDatabase(host: String, port: Int) { ... }
fun connectDatabase(host: String, port: Int, timeout: Duration) { ... }
```

---

### Rule 3: Named Arguments for Multiple Parameters of Same Type
**RULE**: Use named arguments when function has multiple parameters of same type or boolean parameters.

```kotlin
// Clear with named arguments
createAccount(
    firstName = "John",
    lastName = "Doe",
    email = "john@example.com"
)

// Named arguments for booleans - essential for clarity
loadData(
    ignoreCache = true,
    validateSchema = false,
    retryOnFailure = true
)

// Without named arguments - unclear
loadData(true, false, true)
```

---

### Rule 4: Extension Functions - Liberal Use
**RULE**: Create extension functions to enhance existing types idiomatically.

```kotlin
// Extend String
fun String.isValidEmail(): Boolean = contains("@") && contains(".")

// Extend List
fun <T> List<T>.randomItem(): T = this[Random.nextInt(size)]

// Extend Any for type-safe checks
inline fun <reified T> Any.isInstanceOf(): Boolean = this is T

// Usage
if (email.isValidEmail()) { ... }
val randomUser = users.randomItem()
```

---

### Rule 5: Infix Functions - Only for Similar Objects
**RULE**: Use `infix` modifier only for functions operating on similar objects.

```kotlin
// Good infix functions
infix fun String.matches(pattern: Regex): Boolean = pattern.matches(this)
infix fun <T> List<T>.zip(other: List<T>): List<Pair<T, T>> = ...

// Usage
val isMatched = email matches emailRegex
val combined = list1 zip list2

// Bad infix - misleading
infix fun User.add(other: User): User  // Confusing, looks like mutation
```

---

### Rule 6: Higher-Order Functions - Correct Receiver
**RULE**: Use lambda receiver syntax for clarity in higher-order functions.

```kotlin
// Function that takes a block with receiver
fun buildString(builder: StringBuilder.() -> Unit): String {
    val sb = StringBuilder()
    sb.builder()  // Block executed with sb as receiver
    return sb.toString()
}

// Usage - clear context
val html = buildString {
    append("<html>")
    append("<body>Hello</body>")
    append("</html>")
}
```

---

### Rule 7: Return Types for Public APIs
**RULE**: Always explicitly declare return types for public functions.

```kotlin
// Public API - explicit return type required
fun calculateTotal(): Double = ...

// This prevents accidental return type changes
fun formatDate(date: LocalDate): String = ...

// Exception: Local functions can omit return type
fun main() {
    fun helper() = 42  // Return type inferred
}
```

---

### Rule 8: Lambdas - Short vs Named Parameters
**RULE**: Use `it` for single-parameter lambdas only. Always declare parameters explicitly for nested lambdas.

```kotlin
// Single parameter - it is clear
val doubled = numbers.map { it * 2 }

// Multiple parameters - explicit declaration
val sum = numbers.fold(0) { acc, num -> acc + num }

// Nested lambdas - ALWAYS explicit
val result = users
    .groupBy { user ->
        user.department
    }
    .mapValues { (department, depts) ->
        depts.filter { it.active }
    }

// Wrong - unclear with it in nested context
val result = users
    .groupBy { it.department }
    .mapValues { it.value.filter { it.active } }  // Which it?
```

---

### Rule 9: Avoid Multiple Labeled Returns
**RULE**: Single exit point per function. Use labeled returns sparingly.

```kotlin
// Acceptable
fun process(items: List<Item>): Boolean {
    items.forEach { item ->
        if (!item.isValid()) return false
    }
    return true
}

// Better - restructure to avoid labeled returns
fun process(items: List<Item>): Boolean = 
    items.all { it.isValid() }

// Avoid
fun complexLogic(): String {
    list.forEach { item ->
        if (condition1(item)) return@forEach
        if (condition2(item)) return "result1"
    }
    return "default"
}
```

---

### Rule 10: Tail Recursion - Mark Explicitly
**RULE**: Use `tailrec` modifier for recursive functions to optimize tail calls.

```kotlin
// Optimized - tail call eliminated
tailrec fun factorial(n: Int, acc: Int = 1): Int =
    if (n <= 1) acc else factorial(n - 1, n * acc)

// Not optimized - missing tailrec
fun fibonacci(n: Int): Int =
    if (n <= 1) n else fibonacci(n - 1) + fibonacci(n - 2)

// Better - use loop
fun fibonacci(n: Int): Int {
    var a = 0
    var b = 1
    repeat(n) {
        a = b.also { b += a }
    }
    return a
}
```

---

## Classes & Object-Oriented Design

### Rule 1: Data Classes for Data Containers
**RULE**: Use `data class` for classes whose primary purpose is holding data.

**WHY**: Generates `equals()`, `hashCode()`, `toString()`, `copy()`, `componentN()`.

```kotlin
// Correct use of data class
data class User(
    val id: Int,
    val name: String,
    val email: String
)

// Automatic benefits
val user1 = User(1, "John", "john@example.com")
val user2 = user1.copy(name = "Jane")  // copy() for immutability
println(user1 == user2)  // Proper equality
println(user1.toString())  // Nice string representation

// Destructuring
val (id, name, email) = user1
```

❌ **WRONG**:
```kotlin
class User {
    val id: Int
    val name: String
    val email: String
    
    // Manual equals, hashCode, toString - waste of time
}
```

---

### Rule 2: Use Copy for Immutable Updates
**RULE**: Use `copy()` method on data classes to create modified copies.

**WHY**: Maintains immutability while allowing state changes.

```kotlin
val user = User(id = 1, name = "John", email = "john@example.com")

// Change one property
val updatedUser = user.copy(name = "Jane")

// Change multiple properties
val newUser = user.copy(
    id = 2,
    email = "jane@example.com"
)

// Original unchanged
println(user.name)  // Still "John"
```

---

### Rule 3: Value Classes for Type Safety
**RULE**: Use inline `value class` to wrap primitives without runtime overhead.

**Kotlin 2.3.0+**: Explicit backing fields make this cleaner.

```kotlin
@JvmInline
value class UserId(val value: Int)

@JvmInline
value class EmailAddress(val value: String)

// Usage - type safe at compile time, no runtime cost
fun getUser(id: UserId): User = ...
fun sendEmail(to: EmailAddress) = ...

// Prevents accidental swaps
getUser(UserId(123))  // Clear intent
```

---

### Rule 4: Sealed Classes for Type Hierarchies
**RULE**: Use sealed classes to restrict inheritance to known subclasses.

```kotlin
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val code: Int, val message: String) : ApiResponse<Nothing>()
    object Loading : ApiResponse<Nothing>()
}

// Exhaustive when expression - no else needed
fun <T> handleResponse(response: ApiResponse<T>) {
    when (response) {
        is ApiResponse.Success -> handleSuccess(response.data)
        is ApiResponse.Error -> handleError(response.code, response.message)
        ApiResponse.Loading -> showLoadingIndicator()
    }
}
```

---

### Rule 5: Object Declarations for Singletons
**RULE**: Use `object` keyword for singletons instead of creating them manually.

```kotlin
// Kotlin handles singleton creation safely
object AppConfig {
    const val API_URL = "https://api.example.com"
    const val TIMEOUT_MS = 5000
    
    val logger: Logger = Logger.create()
}

object Database {
    fun query(sql: String): List<Row> = ...
}

// Usage
AppConfig.logger.log("Starting app")
Database.query("SELECT * FROM users")
```

---

### Rule 6: Delegation Over Inheritance
**RULE**: Use delegation pattern (by keyword) instead of inheritance when possible.

**WHY**: Composition is more flexible than inheritance, avoids fragile base class problem.

```kotlin
// Interface to implement
interface Database {
    fun save(data: String): Boolean
    fun load(id: Int): String?
}

// Delegation - implement interface by delegating to another object
class CachedDatabase(private val impl: Database) : Database by impl {
    override fun load(id: Int): String? {
        return cache.getOrNull(id) ?: impl.load(id)?.also { cache[id] = it }
    }
}

// Better than inheriting from BaseDatabase
```

---

### Rule 7: Primary Constructor with Properties
**RULE**: Declare properties directly in primary constructor for conciseness.

```kotlin
// Good - declares and initializes in one place
class User(
    val id: Int,
    val name: String,
    private val email: String
) {
    fun sendEmail(body: String) = sendTo(email, body)
}

// Verbose - unnecessary duplication
class User {
    val id: Int
    val name: String
    private val email: String
    
    constructor(id: Int, name: String, email: String) {
        this.id = id
        this.name = name
        this.email = email
    }
}
```

---

### Rule 8: Explicit Backing Fields (Kotlin 2.3.0+)
**RULE**: Use explicit backing fields for properties with different internal types.

**WHY**: Eliminates need for separate backing property.

```kotlin
// Before Kotlin 2.3.0
private val _items = mutableListOf<String>()
val items: List<String> = _items

// After Kotlin 2.3.0 (experimental)
var items: List<String>
    field = mutableListOf()  // Internal mutable implementation
    // Exposed as immutable

// Enable with compiler option:
// -Xexplicit-backing-fields
```

---

### Rule 9: Prefer Composition Over Inheritance
**RULE**: Inherit only when modeling "is-a" relationships. Otherwise, compose.

```kotlin
// Good - composition
class NotificationService(
    private val emailSender: EmailSender,
    private val smsSender: SmsSender,
    private val logger: Logger
) {
    fun notify(user: User) {
        emailSender.send(user.email, "Hello!")
        logger.log("Notification sent")
    }
}

// Avoid - unnecessary inheritance
class SmartNotificationService : NotificationService() {
    override fun notify(user: User) {
        super.notify(user)
        // Additional logic
    }
}
```

---

## Scope Functions & Context

### Rule 1: Use Appropriate Scope Function
**RULE**: Choose scope function based on context and return value needs.

```kotlin
// let - Transform object, use as parameter
val length = str?.let { it.length } ?: 0

// run - Configure object (implicit receiver), return result
val configured = User("John").run {
    age = 30
    email = "john@example.com"
    this  // Returns the configured object
}

// apply - Configure object (implicit receiver), return same object
val user = User().apply {
    age = 30
    email = "john@example.com"
}  // Returns the User instance

// also - Side effects (parameter), return same object
users.add(newUser).also {
    notifyListeners()  // Side effect
    println("User added: ${it.name}")
}

// with - Configure object (implicit receiver), return any value
val info = with(user) {
    "Name: $name, Email: $email"
}
```

**Decision Matrix**:
- **let**: Null handling, transformation
- **run**: Configuration with result
- **apply**: Configuration without result
- **also**: Side effects while returning same object
- **with**: Multiple operations on object (non-extension)

---

### Rule 2: Avoid Deep Nesting with Scope Functions
**RULE**: Don't nest scope functions deeply. Restructure code if needed.

```kotlin
// Bad - nested scope functions
user?.let { u ->
    database.save(u).also { savedUser ->
        notificationService.send(savedUser).apply {
            logger.log("Sent")
        }
    }
}

// Better - separate concerns
val savedUser = user?.let { database.save(it) } ?: return
notificationService.send(savedUser)
logger.log("Sent")
```

---

### Rule 3: Prefer Extension Functions for Multiple Operations
**RULE**: Extract multiple operations into extension function instead of chaining scope functions.

```kotlin
// Define extension
fun User.saveAndNotify(
    database: Database,
    notificationService: NotificationService
) {
    database.save(this)
    notificationService.send(this)
}

// Clean usage
user.saveAndNotify(database, notificationService)

// Instead of
user?.let {
    database.save(it)
    notificationService.send(it)
}
```

---

## Coroutines & Asynchrony

### Rule 1: Prefer Coroutines Over Threads
**RULE**: Use Kotlin coroutines for asynchronous code, not threads.

**WHY**: Coroutines are lightweight, non-blocking, and integrate with Kotlin's structured concurrency.

```kotlin
// Correct - coroutines
viewModelScope.launch {
    val user = fetchUserAsync()  // suspend function
    displayUser(user)
}

// Wrong - threads
thread {
    val user = fetchUser()  // Blocking call
    runOnUiThread { displayUser(user) }
}
```

---

### Rule 2: Structured Concurrency Always
**RULE**: Always launch coroutines within a scope. Never use `GlobalScope`.

**WHY**: `GlobalScope` creates uncontrolled coroutines that can leak and cause memory issues.

```kotlin
// Correct - scoped coroutine
class UserViewModel : ViewModel() {
    fun loadUser(id: Int) {
        viewModelScope.launch {  // Lifecycle-aware
            val user = repository.getUser(id)
            _user.value = user
        }
    }
}

// Wrong - GlobalScope
fun loadUser(id: Int) {
    GlobalScope.launch {  // Uncontrolled, memory leak risk
        val user = repository.getUser(id)
        updateUI(user)
    }
}
```

---

### Rule 3: CoroutineScope Selection
**RULE**: Choose appropriate scope based on context.

```kotlin
// ViewModel (Android)
viewModelScope.launch { ... }

// Fragment/Activity
lifecycleScope.launch { ... }

// Service/Repository - create own scope
val scope = CoroutineScope(Dispatchers.Main + Job())

// Custom scope with cancellation
val job = Job()
val scope = CoroutineScope(Dispatchers.IO + job)

// Clean up
job.cancel()
```

---

### Rule 4: Exception Handling in Coroutines
**RULE**: Use `try-catch` or `CoroutineExceptionHandler` for exception handling.

```kotlin
// Direct handling
viewModelScope.launch {
    try {
        val user = fetchUser()
        _user.value = user
    } catch (e: IOException) {
        _error.value = "Network error: ${e.message}"
    }
}

// Global handler
val exceptionHandler = CoroutineExceptionHandler { _, exception ->
    println("Caught exception: ${exception.message}")
}

viewModelScope.launch(exceptionHandler) {
    // Uncaught exceptions handled by exceptionHandler
}
```

---

### Rule 5: Use Suspend Functions
**RULE**: Convert blocking I/O operations to suspend functions.

```kotlin
// Good - suspend function
suspend fun fetchUser(id: Int): User =
    withContext(Dispatchers.IO) {
        // Network call on IO thread
        api.getUser(id)
    }

// Usage
viewModelScope.launch {
    val user = fetchUser(123)  // Non-blocking
}

// Wrong - blocking in coroutine
viewModelScope.launch {
    val user = Thread.sleep(1000)  // Blocks thread!
}

// Better - delay (non-blocking)
viewModelScope.launch {
    delay(1000)  // Non-blocking suspension
}
```

---

### Rule 6: Select Correct Dispatcher
**RULE**: Choose dispatcher based on operation type.

```kotlin
// Main thread - UI updates
launch(Dispatchers.Main) {
    updateUI()
}

// IO thread - network/disk
launch(Dispatchers.IO) {
    database.save(data)
}

// Default - CPU-intensive
launch(Dispatchers.Default) {
    val result = expensiveCalculation()
}

// Unconfined - avoid in production
launch(Dispatchers.Unconfined) { }

// Switch context
viewModelScope.launch(Dispatchers.Main) {
    val data = withContext(Dispatchers.IO) {
        fetchData()  // On IO thread
    }
    updateUI(data)  // Back to Main
}
```

---

### Rule 7: Flow for Reactive Streams
**RULE**: Use `Flow` for emitting multiple values over time.

```kotlin
// Produce stream
fun getUsers(): Flow<User> = flow {
    for (id in 1..10) {
        val user = fetchUser(id)
        emit(user)
    }
}

// Consume stream
viewModelScope.launch {
    getUsers()
        .filter { it.isActive }
        .map { it.name }
        .collect { name ->
            println(name)
        }
}
```

---

### Rule 8: StateFlow for State Management
**RULE**: Use `StateFlow` to represent mutable state in reactive way.

```kotlin
class UserViewModel : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()
    
    fun loadUser(id: Int) {
        viewModelScope.launch {
            _user.value = repository.getUser(id)
        }
    }
}

// Observe
viewModelScope.launch {
    viewModel.user.collect { user ->
        if (user != null) updateUI(user)
    }
}
```

---

### Rule 9: Cancellation Handling
**RULE**: Respect coroutine cancellation with `ensureActive()` or `isActive`.

```kotlin
// React to cancellation
viewModelScope.launch {
    for (i in 1..100) {
        ensureActive()  // Check if cancelled
        processItem(i)
        delay(100)
    }
}

// Cleanup on cancellation
try {
    viewModelScope.launch {
        // Code here
    }
} finally {
    cleanup()  // Called on cancellation
}
```

---

## Memory Management & Lifecycle

### Rule 1: Avoid Memory Leaks - Release Resources
**RULE**: Always release resources no longer needed: listeners, observers, connections.

```kotlin
// Anti-pattern - memory leak
class MyActivity : Activity() {
    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) { ... }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) { }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        // Listener never unregistered - memory leak!
    }
}

// Correct - cleanup in onDestroy
class MyActivity : Activity() {
    private val listener = object : SensorEventListener { ... }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(listener)  // Clean up
    }
}
```

---

### Rule 2: Avoid Static References to Context/Activity/View
**RULE**: Never keep static references to Context, Activity, or View objects.

**WHY**: Prevents garbage collection, causes memory leaks.

```kotlin
// Wrong - static reference
object AppConfig {
    var activity: Activity? = null  // Memory leak!
    var context: Context? = null
}

// Correct - use WeakReference if necessary
class MyRepository {
    private var context: WeakReference<Context>? = null
    
    fun setContext(ctx: Context) {
        context = WeakReference(ctx)
    }
    
    fun getContext(): Context? = context?.get()
}

// Better - use dependency injection
class MyRepository(
    private val context: Context  // Passed in, not stored globally
) { ... }
```

---

### Rule 3: Use Lifecycle-Aware Components
**RULE**: Use ViewModel, LiveData, and LifecycleObserver for automatic cleanup.

```kotlin
class UserViewModel : ViewModel() {
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user
    
    // ViewModel automatically cleared when activity destroyed
    
    override fun onCleared() {
        super.onCleared()
        // Additional cleanup if needed
    }
}

// Lifecycle-aware observer
activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        startUpdates()
    }
    
    override fun onStop(owner: LifecycleOwner) {
        stopUpdates()
    }
})
```

---

### Rule 4: Unregister Listeners and Observers
**RULE**: Always unregister listeners when no longer needed.

```kotlin
// Register in onCreate/onStart
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    eventBus.register(this)
    broadcastReceiver = MyReceiver()
    registerReceiver(broadcastReceiver, IntentFilter())
}

// Unregister in onDestroy/onStop
override fun onDestroy() {
    super.onDestroy()
    eventBus.unregister(this)
    unregisterReceiver(broadcastReceiver)
}
```

---

### Rule 5: Cancel Coroutines on Lifecycle Events
**RULE**: Use `viewModelScope` or `lifecycleScope` for automatic cancellation.

```kotlin
// Automatic cancellation with viewModelScope
class UserViewModel : ViewModel() {
    fun loadUser() {
        viewModelScope.launch {  // Cancelled when ViewModel cleared
            val user = repository.getUser()
            _user.value = user
        }
    }
}

// Manual cleanup if necessary
private val job = Job()
override fun onDestroy() {
    super.onDestroy()
    job.cancel()  // Cancel all coroutines
}
```

---

### Rule 6: Bitmap/Image Management
**RULE**: Recycle bitmaps and clear strong references to large images.

```kotlin
// Recycle bitmap
override fun onDestroy() {
    super.onDestroy()
    bitmap?.recycle()
    bitmap = null
}

// Clear imageView references
override fun onDestroy() {
    super.onDestroy()
    imageView.setImageDrawable(null)
    imageView.setImageBitmap(null)
}

// Use SoftReference for caches
private val bitmapCache = mutableMapOf<String, SoftReference<Bitmap>>()
```

---

## Performance Optimization

### Rule 1: Use Inline Functions Carefully
**RULE**: Mark functions with lambda parameters as `inline` to eliminate function call overhead.

**CAUTION**: Inline increases bytecode. Use only when beneficial.

```kotlin
// Good candidate for inline - used frequently with lambdas
inline fun <T> measure(block: () -> T): Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}

// Not good for inline - complex function
inline fun processLargeData(items: List<Item>, transformer: (Item) -> String) {
    // Complex logic here
}

// Suppress inline warning if needed
@Suppress("NOTHING_TO_INLINE")
inline fun getValue(): Int = 42
```

---

### Rule 2: Use Reified Type Parameters
**RULE**: Use `reified` with inline functions to access type information at runtime.

```kotlin
// Access type without reflection
inline fun <reified T> fromJson(json: String): T {
    return when (T::class) {
        String::class -> json as T
        Int::class -> json.toInt() as T
        else -> throw IllegalArgumentException()
    }
}

// Create instances without explicit class parameter
inline fun <reified T> newInstance(): T = T::class.createInstance()

// Filter by type safely
inline fun <reified T> List<Any>.filterType(): List<T> =
    filterIsInstance<T>()
```

---

### Rule 3: Avoid Unnecessary Object Creation
**RULE**: Don't create objects unnecessarily, especially in loops.

```kotlin
// Wrong - creates new regex each iteration
for (str in strings) {
    if (str.matches(Regex("\\d+"))) { ... }
}

// Correct - create once
val digitRegex = Regex("\\d+")
for (str in strings) {
    if (str.matches(digitRegex)) { ... }
}

// Use object pooling for expensive objects
object ConnectionPool {
    private val availableConnections = mutableListOf<Connection>()
    
    fun borrowConnection(): Connection = 
        availableConnections.removeLastOrNull() ?: createConnection()
    
    fun returnConnection(conn: Connection) {
        availableConnections.add(conn)
    }
}
```

---

### Rule 4: Lazy Initialization
**RULE**: Use `lazy` for expensive one-time initializations.

```kotlin
// Lazy property - initialized only when first accessed
val expensiveResource: Resource by lazy {
    println("Initializing resource")
    loadResource()  // Called once, on first access
}

// Usage
println("Starting")  // Prints first
val resource = expensiveResource  // Prints "Initializing resource" then uses it
val again = expensiveResource  // Uses cached value, doesn't reinitialize

// Thread-safe by default
// Can customize initialization with custom getter
val myProperty: String by lazy(LazyThreadSafetyMode.NONE) {
    "value"
}
```

---

### Rule 5: When Expression Over If-Else Chains
**RULE**: Use `when` instead of long `if-else` chains for better readability and potential optimization.

```kotlin
// Optimized by compiler
val result = when (value) {
    1 -> "One"
    2 -> "Two"
    3 -> "Three"
    else -> "Other"
}

// Less efficient
val result = if (value == 1) {
    "One"
} else if (value == 2) {
    "Two"
} else if (value == 3) {
    "Three"
} else {
    "Other"
}
```

---

### Rule 6: Smart Casts - Avoid Redundant Casting
**RULE**: Let Kotlin's smart cast eliminate redundant type casts.

```kotlin
// Smart cast works automatically
if (obj is String) {
    println(obj.length)  // No cast needed, obj is String
}

when (obj) {
    is String -> println(obj.length)  // Smart cast
    is Int -> println(obj + 1)
}

// Safe cast with smart cast
val str = obj as? String
if (str != null) {
    println(str.length)  // Smart cast, str is not null
}
```

---

### Rule 7: Collection Operations - Avoid Intermediate Collections
**RULE**: Use sequence or combine operations to avoid creating intermediate lists.

```kotlin
// Creates intermediate lists
val result = list
    .filter { it > 10 }  // Creates List<Int>
    .map { it * 2 }  // Creates List<Int>
    .take(5)  // Creates List<Int>

// Use sequence - lazy evaluation, no intermediates
val result = list
    .asSequence()
    .filter { it > 10 }
    .map { it * 2 }
    .take(5)
    .toList()  // Only one final list created

// Or use single operation
val result = list
    .filter { it > 10 }
    .take(5)  // Terminal operation, stops early
    .map { it * 2 }
```

---

### Rule 8: Tail Recursion Optimization
**RULE**: Use `tailrec` for recursive functions to prevent stack overflow.

```kotlin
// Optimized - converts to loop
tailrec fun factorial(n: Int, acc: Int = 1): Int =
    if (n <= 1) acc else factorial(n - 1, n * acc)

// Not optimized - deep recursion causes StackOverflowError
fun fibonacci(n: Int): Int =
    if (n <= 1) n else fibonacci(n - 1) + fibonacci(n - 2)

// Use loops for better readability
fun fibonacci(n: Int): Int {
    var a = 0
    var b = 1
    repeat(n) {
        a = b.also { b += a }
    }
    return a
}
```

---

### Rule 9: List vs Array
**RULE**: Use `List` for most cases. `Array` only when needed for JNI or specific APIs.

```kotlin
// Preferred - List with optimizations
val numbers: List<Int> = listOf(1, 2, 3)
val mutable = mutableListOf(1, 2, 3)

// Use Array only when necessary
val array = intArrayOf(1, 2, 3)  // Primitive array, no boxing

// Don't use Array<Int> - boxes every element
val boxedArray: Array<Int> = arrayOf(1, 2, 3)  // Inefficient
```

---

### Rule 10: Contract and Expect for Compiler Help
**RULE**: Use `contract` function to help compiler optimize (rare).

```kotlin
// Rarely used - tells compiler about control flow
fun <T> requireNotNull(value: T?, lazyMessage: () -> String = { "Required value was null" }): T {
    contract {
        returns() implies (value != null)
    }
    
    return value ?: throw IllegalArgumentException(lazyMessage())
}

// After calling requireNotNull, compiler knows it's not null
val user: User? = getUser()
requireNotNull(user)
println(user.name)  // Compiler allows - knows user is not null
```

---

## Testing & Verification

### Rule 1: Use JUnit 5 and Kotlin-Specific Features
**RULE**: Leverage Kotlin's features in tests for cleaner test code.

```kotlin
class UserServiceTest {
    @Test
    fun `should create user with valid data`() {
        // Test implementation
    }
    
    @Test
    fun `should throw exception when email is invalid`() {
        assertThrows<IllegalArgumentException> {
            userService.createUser("John", "invalid-email")
        }
    }
}

// Use @TestInstance for non-static members
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseTest {
    private lateinit var db: Database
    
    @BeforeAll
    fun setupDatabase() {
        db = Database()
    }
}
```

---

### Rule 2: Avoid Mutable State in Tests
**RULE**: Create test data with `val` and immutable collections.

```kotlin
// Good - immutable test data
val testUser = User(id = 1, name = "John", email = "john@example.com")

// Test different scenarios with copy
val inactiveUser = testUser.copy(isActive = false)
val anotherUser = testUser.copy(id = 2, name = "Jane")

// Don't mock excessively - test real behavior
@Test
fun `should calculate total price correctly`() {
    val items = listOf(
        Item(name = "Book", price = 10.0),
        Item(name = "Pen", price = 2.0)
    )
    
    val total = calculator.calculateTotal(items)
    
    assertEquals(12.0, total)
}
```

---

### Rule 3: Use Data Classes for Test Assertions
**RULE**: Compare objects directly using data class equality.

```kotlin
@Test
fun `should return user with correct properties`() {
    val expected = User(
        id = 1,
        name = "John",
        email = "john@example.com"
    )
    
    val actual = userRepository.getUser(1)
    
    // Simple, clear assertion
    assertEquals(expected, actual)
}

// With Kotest for powerful assertions
actual shouldBe expected
actual.name shouldContain "John"
```

---

### Rule 4: Helper Functions for Complex Test Data
**RULE**: Create builder functions for test data with complex structure.

```kotlin
// Helper function with defaults
fun createUser(
    id: Int = 1,
    name: String = "Test User",
    email: String = "test@example.com",
    isActive: Boolean = true
): User = User(id, name, email, isActive)

// Easy to customize per test
@Test
fun `inactive users should not receive emails`() {
    val inactiveUser = createUser(isActive = false)
    val email = Email(to = inactiveUser.email, subject = "Test")
    
    assertFalse(emailService.shouldSend(email, inactiveUser))
}
```

---

### Rule 5: Parameterized Tests for Multiple Scenarios
**RULE**: Use `@ParameterizedTest` to test multiple inputs.

```kotlin
@ParameterizedTest
@ValueSource(ints = [1, 2, 3, 4, 5])
fun `should be positive`(number: Int) {
    assertTrue(number > 0)
}

// CSV source for complex data
@ParameterizedTest
@CsvSource(
    "John, john@example.com, true",
    "Jane, jane@example.com, false",
    "'', invalid@example.com, false"
)
fun `should validate email`(name: String, email: String, expected: Boolean) {
    assertEquals(expected, validator.isValidEmail(email))
}
```

---

### Rule 6: Mock External Dependencies
**RULE**: Mock only external dependencies. Test real behavior of your code.

```kotlin
class UserServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val emailService = mockk<EmailService>()
    private val userService = UserService(userRepository, emailService)
    
    @Test
    fun `should notify user after successful registration`() {
        val newUser = User(id = 1, name = "John", email = "john@example.com")
        
        every { userRepository.save(any()) } returns newUser
        every { emailService.sendWelcomeEmail(any()) } returns true
        
        val result = userService.register("John", "john@example.com")
        
        assertEquals(newUser, result)
        verify { userRepository.save(any()) }
        verify { emailService.sendWelcomeEmail(newUser) }
    }
}
```

---

## Exception Handling & Error Management

### Rule 1: Prefer Specific Exceptions
**RULE**: Catch and throw specific exception types, not generic `Exception`.

```kotlin
// Good - specific exception handling
try {
    val user = getUserFromDatabase(id)
} catch (e: SQLException) {
    logger.error("Database error: ${e.message}")
    throw DataAccessException("Failed to fetch user", e)
} catch (e: IllegalArgumentException) {
    throw InvalidUserIdException("User ID must be positive", e)
}

// Bad - generic exception
try {
    val user = getUserFromDatabase(id)
} catch (e: Exception) {
    logger.error("An error occurred")
}
```

---

### Rule 2: Don't Suppress Exceptions Silently
**RULE**: Always handle exceptions meaningfully. Log, propagate, or recover.

```kotlin
// Bad - swallows exception silently
try {
    saveData()
} catch (e: IOException) {
    // Silence!
}

// Good - log and propagate
try {
    saveData()
} catch (e: IOException) {
    logger.error("Failed to save data: ${e.message}", e)
    throw DataPersistenceException("Could not save data", e)
}

// Good - recover gracefully
try {
    saveData()
} catch (e: IOException) {
    logger.warn("Save failed, will retry")
    retryWithBackoff { saveData() }
}
```

---

### Rule 3: Use Try as Expression
**RULE**: Use `try` expression to capture result or error.

```kotlin
// Expression form - cleaner
val result: Result<User> = try {
    val user = fetchUser()
    Result.Success(user)
} catch (e: IOException) {
    Result.Error(e)
} catch (e: ParseException) {
    Result.Error(e)
}

// Or use sealed classes
val user: User? = try {
    fetchUser()
} catch (e: Exception) {
    logger.error("Failed to fetch user", e)
    null
}
```

---

### Rule 4: Fail Fast and Explicitly
**RULE**: Validate early and throw exceptions rather than returning null.

```kotlin
// Good - fail fast with clear error
fun createUser(name: String, email: String): User {
    require(name.isNotBlank()) { "Name cannot be blank" }
    require(email.contains("@")) { "Email must be valid" }
    require(email.isNotBlank()) { "Email cannot be blank" }
    
    return User(generateId(), name, email)
}

// Bad - silently creates invalid user
fun createUser(name: String?, email: String?): User? {
    if (name == null || email == null) return null
    return User(generateId(), name, email)  // Could be invalid
}
```

---

### Rule 5: Use Sealed Classes for Result Types
**RULE**: Model success/failure with sealed classes for type-safe handling.

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

// Type-safe handling
fun <T> handleResult(result: Result<T>) {
    when (result) {
        is Result.Success -> println("Success: ${result.data}")
        is Result.Error -> println("Error: ${result.exception.message}")
    }
}

// In coroutines
viewModelScope.launch {
    val result = try {
        Result.Success(repository.getUser())
    } catch (e: Exception) {
        Result.Error(e)
    }
    _userResult.value = result
}
```

---

### Rule 6: Cleanup with Finally or Use
**RULE**: Ensure resources are cleaned up with `finally` or `use` blocks.

```kotlin
// Manual cleanup
try {
    val file = File("data.txt")
    file.readText()
} finally {
    file.close()
}

// Better - use extension function
File("data.txt").use { file ->
    file.readText()
}  // Automatically closed

// Try with resources (from Java)
val reader = BufferedReader(FileReader("file.txt"))
try {
    reader.readText()
} finally {
    reader.close()
}

// Kotlin idiom - use
BufferedReader(FileReader("file.txt")).use { reader ->
    reader.readText()
}
```

---

## DSL & Extension Functions

### Rule 1: Scope Receiver for DSL
**RULE**: Use scope receivers (context receivers) to build domain-specific languages.

```kotlin
// DSL for HTML building
fun html(init: HtmlBuilder.() -> Unit): String {
    return HtmlBuilder().apply(init).build()
}

class HtmlBuilder {
    private val elements = mutableListOf<String>()
    
    fun div(init: HtmlBuilder.() -> Unit) {
        elements.add("<div>")
        HtmlBuilder().apply(init).elements.forEach { elements.add(it) }
        elements.add("</div>")
    }
    
    fun p(text: String) {
        elements.add("<p>$text</p>")
    }
    
    fun build(): String = elements.joinToString("\n")
}

// Usage - looks like a language
val markup = html {
    div {
        p("Hello, World!")
    }
}
```

---

### Rule 2: Build Readable DSLs
**RULE**: Design DSLs that read naturally and match the problem domain.

```kotlin
// Task scheduling DSL
fun schedule(init: ScheduleBuilder.() -> Unit) {
    ScheduleBuilder().apply(init).execute()
}

class ScheduleBuilder {
    private val tasks = mutableListOf<Task>()
    
    infix fun String.every(duration: Duration) {
        tasks.add(Task(this, duration))
    }
    
    infix fun String.at(time: LocalTime) {
        tasks.add(Task(this, time))
    }
    
    fun execute() { /* ... */ }
}

// Natural reading
schedule {
    "Backup database" every Duration.ofHours(1)
    "Send email" at LocalTime.of(9, 0)
}
```

---

### Rule 3: Type-Safe Builders
**RULE**: Create type-safe builders that prevent invalid states at compile time.

```kotlin
// Database query builder
class QueryBuilder {
    private var tableName: String? = null
    private var columns: List<String>? = null
    private var whereClause: String? = null
    
    fun select(vararg cols: String) {
        columns = cols.toList()
    }
    
    fun from(table: String) {
        tableName = table
    }
    
    fun where(condition: String) {
        whereClause = condition
    }
    
    fun build(): String {
        require(tableName != null) { "FROM clause required" }
        require(columns != null) { "SELECT clause required" }
        
        var query = "SELECT ${columns!!.joinToString(", ")} FROM $tableName"
        if (whereClause != null) {
            query += " WHERE $whereClause"
        }
        return query
    }
}

// Type-safe usage
val query = QueryBuilder().apply {
    select("id", "name", "email")
    from("users")
    where("age > 18")
}.build()
```

---

## Architecture Patterns

### Rule 1: Unidirectional Data Flow
**RULE**: Implement unidirectional data flow for predictable state management.

```kotlin
// MVI Pattern - unidirectional flow
sealed class UserIntent {
    object LoadUser : UserIntent()
    data class UpdateName(val name: String) : UserIntent()
}

data class UserState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class UserViewModel : ViewModel() {
    private val _state = MutableStateFlow(UserState())
    val state: StateFlow<UserState> = _state.asStateFlow()
    
    fun sendIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUser -> loadUser()
            is UserIntent.UpdateName -> updateName(intent.name)
        }
    }
    
    private fun loadUser() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val user = repository.getUser()
                _state.update { it.copy(user = user, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
```

---

### Rule 2: Repository Pattern for Data Access
**RULE**: Abstract data sources behind repository interfaces.

```kotlin
// Repository interface
interface UserRepository {
    suspend fun getUser(id: Int): User?
    suspend fun saveUser(user: User): Boolean
    fun observeUsers(): Flow<List<User>>
}

// Implementation
class UserRepositoryImpl(
    private val api: UserApi,
    private val database: UserDatabase
) : UserRepository {
    override suspend fun getUser(id: Int): User? {
        return try {
            api.getUser(id)
        } catch (e: IOException) {
            database.getUser(id)
        }
    }
    
    override fun observeUsers(): Flow<List<User>> = 
        database.observeAllUsers()
}

// ViewModel uses repository
class UserViewModel(private val repository: UserRepository) : ViewModel() {
    fun loadUser(id: Int) {
        viewModelScope.launch {
            val user = repository.getUser(id)
            _user.value = user
        }
    }
}
```

---

### Rule 3: Dependency Injection
**RULE**: Inject dependencies rather than creating them internally.

```kotlin
// Manual DI
class Container {
    private val apiService = ApiService()
    private val database = Database()
    private val userRepository = UserRepositoryImpl(apiService, database)
    private val userViewModel = UserViewModel(userRepository)
    
    fun provideUserViewModel(): UserViewModel = userViewModel
}

// Or use Hilt
@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() { ... }

// In Activity
@AndroidEntryPoint
class UserActivity : AppCompatActivity() {
    private val viewModel: UserViewModel by viewModels()
}
```

---

### Rule 4: Separation of Concerns
**RULE**: Keep business logic, UI, and data access separate.

```kotlin
// Good separation
// 1. Data layer
interface UserRepository { ... }

// 2. Business logic layer
class UserUseCase(private val repository: UserRepository) {
    suspend fun getUserWithValidation(id: Int): Result<User> { ... }
}

// 3. Presentation layer
class UserViewModel(private val useCase: UserUseCase) : ViewModel() {
    fun loadUser(id: Int) {
        viewModelScope.launch {
            val result = useCase.getUserWithValidation(id)
            // Update UI state
        }
    }
}

// 4. UI layer
@Composable
fun UserScreen(viewModel: UserViewModel) {
    // Observe state and display
}
```

---

## Kotlin 2.3.0 Breaking Changes

### Breaking Change 1: Language Version Support
**CHANGE**: No support for `-language-version=1.8` or `-language-version=1.9` (non-JVM).

**ACTION**: Update code to Kotlin 2.0+ syntax.

```kotlin
// Old (1.8) - still works but outdated
val range = 1..10

// Modern (2.0+) - preferred
val range = 1 until 10
```

---

### Breaking Change 2: Apple Target Minimums
**CHANGE**: iOS/tvOS minimum raised from 12.0/5.0 to 14.0/7.0.

**ACTION**: If supporting older versions, explicitly configure:

```gradle
kotlin {
    ios {
        setCompatibilityMinVersions(iosMinVersion = "12.0")
    }
}
```

---

### Breaking Change 3: Intel Apple Targets Deprecation
**CHANGE**: `macosX64`, `iosX64`, `tvosX64`, `watchosX64` demoted to support-tier 3.

**ACTION**: Plan migration to ARM64 targets. Will be removed in Kotlin 2.4.0.

```gradle
// Avoid these targets
kotlin {
    macosX64()  // ❌ Use macosArm64() instead
    iosX64()    // ❌ Use iosArm64() instead
}
```

---

### Breaking Change 4: Android Target Migration
**CHANGE**: Must use Google's `com.android.kotlin.multiplatform.library` plugin.

**ACTION**: Update `build.gradle.kts`:

```gradle
// Old (deprecated)
plugins {
    kotlin("multiplatform")
}

kotlin {
    androidTarget()
}

// New (required)
plugins {
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    android {}
}
```

---

### Breaking Change 5: Ant Build System Removed
**CHANGE**: Ant build system no longer supported.

**ACTION**: Migrate to Gradle.

---

### Breaking Change 6: AGP 9.0.0 Built-in Kotlin
**CHANGE**: AGP 9.0.0+ includes built-in Kotlin, `kotlin-android` plugin not needed.

**ACTION**: Remove `kotlin-android` plugin from AGP 9.0.0+:

```gradle
// Remove this for AGP 9.0.0+
plugins {
    id("kotlin-android")  // ❌ Not needed, causes error
}
```

---

## Documentation & Comments

### Rule 1: KDoc for Public APIs
**RULE**: Document all public classes, functions, and properties with KDoc.

```kotlin
/**
 * Fetches a user by their ID from the database.
 *
 * This function makes a network call to the backend API and caches the result.
 * It will retry on network failures up to 3 times before throwing an exception.
 *
 * @param userId The unique identifier of the user to fetch
 * @return The [User] object if found, or null if the user doesn't exist
 * @throws IOException If network error occurs after all retries
 * @throws IllegalArgumentException If userId is negative
 *
 * @see User
 * @see cacheUser
 */
suspend fun getUser(userId: Int): User? {
    require(userId > 0) { "User ID must be positive" }
    return withContext(Dispatchers.IO) {
        retryWithBackoff { userApi.getUser(userId) }
    }
}
```

---

### Rule 2: Comment Intent, Not Implementation
**RULE**: Comments should explain WHY, not WHAT. Code already shows WHAT.

```kotlin
// Good - explains the why
// Reset counter to prevent memory leak from circular references
counter = 0

// Bad - states the obvious
// Set counter to zero
counter = 0

// Good - explains unusual decision
// Using TreeMap instead of HashMap for O(log n) ordering needed for rate limiting
val sortedByTime = TreeMap<Long, Request>()

// Bad - doesn't help
// Create a TreeMap
val sortedByTime = TreeMap<Long, Request>()
```

---

### Rule 3: Avoid Commented Code
**RULE**: Delete commented code. Use version control to retrieve old code.

```kotlin
// Good - no dead code
fun calculateTotal(items: List<Item>): Double {
    return items.sumOf { it.price * it.quantity }
}

// Bad - confusing to maintain
fun calculateTotal(items: List<Item>): Double {
    // var total = 0.0
    // for (item in items) {
    //     total += item.price * item.quantity
    // }
    // return total
    return items.sumOf { it.price * it.quantity }
}
```

---

### Rule 4: Mark Deprecated APIs
**RULE**: Use `@Deprecated` annotation with migration path.

```kotlin
@Deprecated(
    message = "Use getUser() instead, which returns User?",
    replaceWith = ReplaceWith("getUser(id)"),
    level = DeprecationLevel.WARNING
)
fun getUserOrNull(id: Int): User? = getUser(id)

// With migration helper
@Deprecated(
    "Use newFunction() instead",
    ReplaceWith("newFunction(param)"),
    DeprecationLevel.ERROR
)
fun oldFunction(param: String) = newFunction(param)
```

---

### Rule 5: Document Exceptions Thrown
**RULE**: Document exceptions in KDoc using `@throws`.

```kotlin
/**
 * Saves the user to the database.
 *
 * @param user The user to save
 * @return The saved user with generated ID
 * @throws IllegalArgumentException If user data is invalid
 * @throws IOException If database connection fails
 * @throws DuplicateKeyException If email already exists
 */
suspend fun saveUser(user: User): User {
    require(user.email.isNotBlank()) { "Email required" }
    require(user.name.isNotBlank()) { "Name required" }
    
    return withContext(Dispatchers.IO) {
        database.insert(user)
    }
}
```

---

### Rule 6: Keep Comments Updated
**RULE**: Update comments when code changes. Outdated comments are worse than no comments.

```kotlin
// WRONG - misleading comment
// Fetches user from cache, never calls API
suspend fun getUser(id: Int): User? {
    return api.getUser(id)  // Always calls API!
}

// CORRECT
// Fetches user from API. Caller should cache if needed.
suspend fun getUser(id: Int): User? {
    return api.getUser(id)
}
```

---

## Forbidden Practices - Never Do This

### 🚫 FORBIDDEN 1: Using `!!` Operator in Production
```kotlin
// ❌ NEVER in production code
val name = user!!.name  // Will crash if user is null

// ✅ USE instead
val name = user?.name ?: "Unknown"
```

### 🚫 FORBIDDEN 2: GlobalScope
```kotlin
// ❌ NEVER use GlobalScope
GlobalScope.launch {
    // Memory leak risk, uncontrolled lifetime
}

// ✅ USE scope-aware alternative
viewModelScope.launch {
    // Automatically cancelled when ViewModel is cleared
}
```

### 🚫 FORBIDDEN 3: Mutable Global State
```kotlin
// ❌ NEVER
object AppState {
    var currentUser: User? = null  // Mutable global - thread safety issues
    var isLoading = false
}

// ✅ USE dependency injection
class UserViewModel(val userRepository: UserRepository) {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()
}
```

### 🚫 FORBIDDEN 4: Swallowing Exceptions
```kotlin
// ❌ NEVER
try {
    saveData()
} catch (e: IOException) {
    // Silent failure
}

// ✅ ALWAYS handle
try {
    saveData()
} catch (e: IOException) {
    logger.error("Failed to save", e)
    throw DataPersistenceException(e)
}
```

### 🚫 FORBIDDEN 5: Static Context References
```kotlin
// ❌ NEVER
object Globals {
    var context: Context? = null  // Memory leak
}

// ✅ USE dependency injection
class MyRepository(private val context: Context) { ... }
```

### 🚫 FORBIDDEN 6: Var for Constants
```kotlin
// ❌ NEVER
var MAX_USERS = 100  // Can be changed

// ✅ USE
const val MAX_USERS = 100
```

### 🚫 FORBIDDEN 7: Deep Nesting
```kotlin
// ❌ NEVER - unreadable
user?.let { u ->
    u.orders?.forEach { order ->
        order.items?.filter { it.active }?.let { items ->
            // 3 levels deep
        }
    }
}

// ✅ USE - flat structure
val activeItems = user?.orders
    ?.flatMap { it.items.orEmpty() }
    ?.filter { it.active }
    ?: emptyList()
```

### 🚫 FORBIDDEN 8: Mutable Default Arguments
```kotlin
// ❌ NEVER - default shared across calls
fun processItems(items: MutableList<Item> = mutableListOf()) { ... }

// ✅ USE
fun processItems(items: List<Item> = emptyList()) { ... }
```

### 🚫 FORBIDDEN 9: Try-Catch for Control Flow
```kotlin
// ❌ NEVER - exceptions for normal flow
try {
    while (true) {
        process(queue.remove())
    }
} catch (e: NoSuchElementException) {
    return  // Using exception for flow control
}

// ✅ USE
while (queue.isNotEmpty()) {
    process(queue.remove())
}
```

### 🚫 FORBIDDEN 10: Platform Types from Java
```kotlin
// ❌ NEVER - platform type (could be null or not)
val str = javaMethod()  // Type: String!

// ✅ ALWAYS explicit
val str: String = javaMethod()  // Document expectation
val str: String? = javaMethod()  // Can be null
```

---

## Best Practices Summary Table

| Category | Practice | Reason |
|----------|----------|--------|
| Immutability | Default to `val` | Thread-safe, predictable |
| Collections | Use immutable types | Can't be accidentally modified |
| Functions | Expression bodies | Concise, declarative |
| Null Safety | Use `?.` and `?:` | Avoid NPE, safe navigation |
| Exceptions | Specific types | Clear error handling |
| Coroutines | Use scopes | Automatic cancellation |
| Memory | Lifecycle-aware | Prevent leaks |
| Performance | Use sequences | Lazy evaluation |
| Testing | Data classes | Clear assertions |
| Naming | Clear, idiomatic | Easy to understand |

---

## Quick Reference

### When to Use What

| Feature | When to Use | When Not to Use |
|---------|-----------|-----------------|
| `data class` | Holding data | Classes with behavior only |
| `sealed class` | Restricted hierarchy | Open inheritance |
| `object` | Singleton | Instance per creation |
| `inline` | Lambdas, small funcs | Large/complex functions |
| `lazy` | Expensive one-time init | Cheap initialization |
| `val` | Always | Only use `var` if must change |
| Flow | Multiple values over time | Single values |
| StateFlow | Mutable state | Immutable state |
| `!!` | Test code only | Production code |

---

## Version Information

- **Kotlin Version**: 2.3.0
- **Gradle Version**: 7.6.3 - 9.0.0
- **JVM Target**: Java 25 supported
- **Document Version**: 1.0
- **Last Updated**: December 2025

---

## References

- [Official Kotlin Documentation](https://kotlinlang.org/docs/)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [What's New in Kotlin 2.3.0](https://kotlinlang.org/docs/whatsnew23.html)
- [Kotlin AI Development](https://kotlinlang.org/docs/kotlin-ai-apps-development-overview.html)

---

**End of Document**
