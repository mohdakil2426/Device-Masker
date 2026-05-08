# Agent Coding Rules

These rules are mandatory for agents before writing or changing code in Device Masker.

## First Checks

- Read the relevant `AGENTS.md` file before editing a module.
- For Xposed work, load `.agents/skills/libxposed/SKILL.md` first.
- Keep the change scoped to the user request.
- Do not add baseline debt casually. Detekt baselines should stay empty.

## Code Shape

- Keep functions, composables, interfaces, and files small enough to explain quickly.
- Split by real responsibility, not by random helper extraction.
- Use data tables, maps, sets, or small helpers for pure mapping logic.
- Avoid large `when`/`if` piles when the data shape can remove branches.
- Put public or reusable helper types in files matching their declarations.
- Build long JSON, command text, manifests, and assertion text through small helpers.
- Keep compatibility facades narrow; new workflow code should depend on smaller interfaces.

## Constants

- Name every domain number.
- Raw numbers are acceptable only for obvious zero/one math, tiny local indexes, or API calls where the meaning is already clear.
- In hook code, name argument indexes, API constants, identifier lengths, byte sizes, and limits before use.

## Naming

Rules: `ParameterNaming`, `MatchingDeclarationName`, `ObjectPropertyNaming`

Write code like this:
- Callback parameters can be noun/action style: `timezoneSelected`, `groupSelected`.
- Put public data classes in files matching their declaration name.
- Use backing state names like `serviceConnectedState`, not `_isServiceConnected`.

Avoid:
- Public helper types hidden inside unrelated files.
- Leading underscore properties in Kotlin objects.
- Callback parameter names that Detekt flags repeatedly.

## Compose

- Reusable composables must accept `modifier: Modifier = Modifier`.
- State-backed collection parameters should use immutable collection types.
- Wrap callbacks captured by `LaunchedEffect` or restartable effects with `rememberUpdatedState`.
- Do not build ViewModels inside composable bodies. Use factories and default parameters at the screen boundary.
- Helpers that emit multiple independent children need a parent layout.
- Keep scope-specific modifiers, such as dropdown menu anchors, at the scope call site and pass them down as `modifier`.
- Split large screens by state, section, row, and action boundaries.
- Do not add wrapper composables that only forward a ViewModel.
- Create small stable UI-state models before rendering category-heavy screens.

## Android API Levels

- New platform APIs need both runtime SDK guards and local API annotations such as `@RequiresApi`.
- Keep API-gated helpers small so Lint can prove the guarded call path.

## Error Handling

- Prefer specific exceptions for file, JSON, parsing, and serialization failures.
- Fallbacks must be logged with useful context.
- Do not write silent recovery like `catch (Exception) { false }`.
- Broad catches are allowed only at real safety boundaries and must document why recovery is safe.

## Xposed Safety

- Runtime hooks must use `intercept(stableHooker { ... })` or explicit named `XposedInterface.Hooker` implementations.
- Never use direct Kotlin SAM callbacks like `.intercept { ... }` in runtime hookers.
- Separate method discovery, hook registration, and callback behavior.
- Use data tables for repeated framework getter hooks instead of copy-pasted hook blocks.
- Rethrow `XposedFrameworkError` before generic catches.
- Return original values for disabled, missing, blank, malformed, unsafe, or unsupported config.
- Do not generate random fallback identifiers inside target processes.
- Do not use Timber, Compose, app-private JSON reads, or hardcoded RemotePreferences keys in `:xposed`.
- Preserve hook isolation: one failed hook must not block unrelated hooks.

## App And Common Boundaries

- `SharedPrefsKeys` is the only source for RemotePreferences key names.
- `JsonConfig.appConfigs` is canonical for app scope.
- Generate identity values in app/common config flows, not in hooks.
- Prefer narrow workflow interfaces for new code. Compatibility facades should not keep growing.

## Logs And Evidence

- Put build logs in `logs/build/`.
- Put device/logcat evidence in `logs/device/`.
- Put scratch checks in `logs/tmp/`.
- Do not place temporary logs or evidence in the project root.

## Verification

- Run Spotless for formatting; do not hand-format unrelated code.
- Run Detekt after Kotlin or Compose changes.
- After `detektBaseline`, run `detekt` and count non-empty baseline IDs.
- Run tests for touched modules.
- Before release or R8 claims, run the full gate and the Xposed R8 ABI guard.
- Do not claim hook success without LSPosed/logcat evidence and target-app value checks where possible.

## Hard Stop

If a change requires adding Detekt baseline entries, broad Xposed behavior, a new config delivery path, or a custom Binder/AIDL hook-evidence path, stop and document the reason before implementing.
