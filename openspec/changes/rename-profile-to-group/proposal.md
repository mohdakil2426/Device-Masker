# Change: Rename Profile to Group Terminology

## Why

The term "Profile" is ambiguous in the app context - it can be confused with user profiles or device profiles. "Group" more accurately describes what it is: **a collection of apps that share the same spoofed device identity**.

User's rationale:
- "Group of apps" is more intuitive than "profile"
- Avoids confusion with user account profiles
- Better describes the relationship between apps and spoof settings

## What Changes

### 1. Data Model (BREAKING)
- **`SpoofProfile`** → **`SpoofGroup`** (in `:common` module)
- **`JsonConfig.profiles`** → **`JsonConfig.groups`**
- All references to profile ID → group ID

### 2. UI Layer
- **`ProfileScreen`** → **`GroupsScreen`** (list of groups)
- **`ProfileDetailScreen`** → **`GroupSpoofingScreen`** (edit group spoofing)
- **`ProfileCard`** → **`GroupCard`** (component)
- **`ProfileViewModel`** → **`GroupsViewModel`**
- **`profiledetail/`** → **`groupspoofing/`** (package)

### 3. Navigation
- **`NavRoutes.PROFILES`** → **`NavRoutes.GROUPS`**
- **`NavRoutes.PROFILE_DETAIL`** → **`NavRoutes.GROUP_SPOOFING`**
- **`profileDetailRoute()`** → **`groupSpoofingRoute()`**

### 4. String Resources
- All `profile_*` strings → `group_*` strings
- User-facing labels: "Profile" → "Group"

### 5. Repository & Service
- **`SpoofRepository`** methods: `getProfiles()` → `getGroups()`
- **`ConfigManager`**: profile references → group references

### 6. Memory Bank & Documentation
- All memory-bank `.md` files updated
- OpenSpec specs updated

## Impact

### Affected Specs
- `core-infrastructure` - Data model changes
- `data-management` - Storage key changes

### Affected Modules
| Module | Files Affected | Impact Level |
|--------|----------------|--------------|
| `:common` | 5 files | **HIGH** - Data model |
| `:app` | ~35 files | **HIGH** - UI + logic |
| `:xposed` | 6 files | **MEDIUM** - Read config |
| `memory-bank` | 6 files | LOW - Docs |

### Breaking Changes
- ⚠️ **JSON Config**: `profiles: []` → `groups: []`
- ⚠️ **Existing saved configs will need migration**

### Migration Strategy
- Add `@SerialName("profiles")` alias for backward compatibility
- OR perform one-time migration on app launch
