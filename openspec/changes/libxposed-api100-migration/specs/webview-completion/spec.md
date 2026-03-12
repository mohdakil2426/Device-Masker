## ADDED Requirements

### Requirement: Hook WebView.getDefaultUserAgent(Context)

The module SHALL hook the static method `WebView.getDefaultUserAgent(Context)` to replace the device model in the returned User-Agent string with the spoofed model from the active device profile.

#### Scenario: Static UA returns spoofed model

- **WHEN** an app calls `WebView.getDefaultUserAgent(context)` and the device profile is "Pixel 9 Pro"
- **THEN** the returned UA string contains `Pixel 9 Pro` instead of the real device model

### Requirement: Hook System.getProperty("http.agent")

The module SHALL hook `System.getProperty(String)` and intercept calls where the key is `"http.agent"` to replace the device model with the spoofed model.

#### Scenario: OkHttp reads http.agent

- **WHEN** OkHttp internally calls `System.getProperty("http.agent")` for default User-Agent
- **THEN** the returned string contains the spoofed device model

#### Scenario: Non-http.agent property passthrough

- **WHEN** an app calls `System.getProperty("os.name")`
- **THEN** the hook does NOT intercept — the real value is returned

### Requirement: WebSettings.getUserAgentString() still hooked

The existing `WebSettings.getUserAgentString()` hook SHALL continue to function alongside the new hooks.

#### Scenario: All three UA vectors spoofed

- **WHEN** an app reads User-Agent via any of the three methods (WebSettings, WebView.getDefaultUserAgent, System.getProperty)
- **THEN** all three return a User-Agent with the spoofed device model

### Requirement: UA model replacement regex

The User-Agent model replacement SHALL use a regex pattern `\(Linux; Android (\d+(?:\.\d+)?); ([^)]*)\)` to replace only the device model portion, preserving the Android version.

#### Scenario: Regex replacement preserves version

- **WHEN** the original UA is `"Mozilla/5.0 (Linux; Android 15; SM-G998B) AppleWebKit/..."` and the spoofed model is "Pixel 9 Pro"
- **THEN** the result is `"Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/..."`
