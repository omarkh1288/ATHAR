# AGENTS.md - AccessibilityMappingApp Developer Guide

## Project Overview
**AccessibilityMappingApp** is a dual-platform (Android app + Kotlin/Ktor backend) accessibility mapping platform for reporting and tracking accessibility issues in physical locations. The Android client uses Jetpack Compose for UI, and the backend provides REST APIs for data management.

- **Client:** Native Android app (Kotlin + Jetpack Compose)
- **Backend:** Kotlin/Ktor server
- **Architecture:** Multi-module Gradle project with shared data models

## Build System & Environment

### Critical Build Configuration
- **Gradle:** 8.12.0 (Android Gradle Plugin)
- **Kotlin:** 2.2.20 (compiler version)
- **Java:** JDK 17.0.17 (Eclipse Adoptium)
- **Build Location:** Custom `~/.gradle-local-build/` directory to avoid OneDrive/Windows file locking issues

**Key build command:**
```bash
# Clean debug build
.\gradlew assembleDebug --console=plain

# Build and install to emulator
.\gradlew installDebug
```

### Known Windows Issues

**OneDrive file lock conflicts:**
- OneDrive sync conflicts with Gradle incremental build directories → **Solution**: Build outputs redirected to `~/.gradle-local-build/` via `settings.gradle.kts` (lines 11-18)
- Gradle incremental builds may hang due to Windows file locks → Always use `--console=plain` flag

**R.jar file locking (processDebugResources):**
- **Error**: `java.io.IOException: Couldn't delete R.jar: The process cannot access the file because it is being used by another process`
- **Root cause**: Gradle daemon or antivirus holds lock on `intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar`
- **Quick fix**: 
  ```bash
  # Option 1: Use the provided build script (recommended)
  .\build.bat

  # Option 2: Manual fix
  .\gradlew --stop
  taskkill /IM java.exe /F
  rmdir /s /q "%USERPROFILE%\.gradle-local-build\AccessibilityMappingApp"
  .\gradlew assembleDebug --console=plain
  ```
- **Permanent fix**: Build now includes auto-cleanup in `app/build.gradle.kts` - `processDebugResources` task automatically clears locked directories before execution

## Kotlin Compose Architecture

### UI Structure (`app/src/main/java/com/athar/accessibilitymapping/ui/`)
- **MainActivity.kt**: Entry point, tab-based navigation (Map, Requests, Profile)
- **screens/**: Composable screen definitions (LoginScreen, MapScreen, ProfileScreen, etc.)
- **components/**: Reusable UI components
- **theme/**: Material Design 3 theming, custom dimensions (sdp/ssp - responsive units)
- **localization/**: Multi-language support

### Composition Pattern
Uses `rememberCoroutineScope()` + coroutines for async operations. State management via `mutableStateOf()` + `rememberSaveable()` for lifecycle-aware state preservation.

**Example (MainActivity.kt lines 84-100):** Tab-based navigation using enum-driven state transitions.

### Critical Custom Extensions
- **sdp/ssp units**: Custom responsive dimension extensions (see `theme/Dimension.kt`)
- Enables scalable UI across different screen sizes
- Always use `.sdp` for layouts instead of `.dp`

## Data & Networking

### Backend API Client (Ktor)
Located in: `app/src/main/java/com/athar/accessibilitymapping/data/BackendApiClient.kt`

**Key details:**
- HTTP Client: `HttpClient(OkHttp)` with content negotiation (JSON)
- Base URL: Configured from `local.properties` → `BuildConfig.BACKEND_BASE_URL` (default Cloudflare tunnel URL)
- Authentication: Bearer token via `bearerAuth(token)` 
- File uploads: Multipart form data support for accessibility request photos

**Configuration precedence:**
1. `local.properties` (local dev override)
2. Environment variables (CI/CD)
3. Defaults in `build.gradle.kts` (lines 38-40)

### Data Models
Located in: `app/src/main/java/com/athar/accessibilitymapping/data/`

Key entities:
- **UserRole**: Enum (USER, REVIEWER, ADMIN) - determines UI visibility
- **AuthRepository**: Manages login state and JWT tokens
- **AppPreferencesStore**: DataStore-based preference persistence

## Build & Compilation Tips

### Common Gradle Errors

**"None of the following candidates is applicable" on `.border()`:**
- **Root cause**: Border function signature changed between Compose versions
- **Solution**: Use positional arguments instead of named parameters
```kotlin
// ❌ Wrong (named params)
.border(width = 1.5.dp, color = color, shape = shape)

// ✅ Correct (positional)
.border(1.5.dp, color, shape)
```
- Example fix: `app/src/main/java/com/athar/accessibilitymapping/ui/components/ToggleSwitch.kt` (lines 54-57, 67-70)

### Dependency Versions (app/build.gradle.kts)
Key dependencies that define compatibility:
- Jetpack Compose UI: `1.10.3`
- Material3: `1.4.0` 
- Ktor Client: `2.3.13`
- MediaPipe (vision tasks): `0.10.14` (hand landmark detection)
- CameraX: `1.3.4`

### Multi-DEX Configuration
App requires multi-DEX due to method count. Configuration in `build.gradle.kts`:
- `multiDexEnabled = true`
- ProGuard rules via `multidex-config.pro` and `multidex-keep.txt`
- Ensures critical classes like MediaPipe remain unobfuscated

## API Integration Pattern

Ktor client setup pattern (lines 45+ in BackendApiClient.kt):
```kotlin
val httpClient = HttpClient(OkHttp) {
  install(ContentNegotiation) { json(Json { /* config */ }) }
  defaultRequest { 
    bearerAuth(token)
    header(HttpHeaders.ContentType, ContentType.Application.Json)
  }
}
```

Request/Response cycle:
1. Build request with `.post()`, `.get()`, etc.
2. Handle multipart uploads via `MultiPartFormDataContent`
3. Parse response via `bodyAsText()` then manual JSON parsing OR direct `body<Model>()`

## Project-Specific Patterns

### Maps API Integration
- API Key stored in `local.properties` as `MAPS_API_KEY`
- Loaded into `BuildConfig` at build time (see gradle lines 32-40)
- Used in `util/resolveMapsApiKey()` → Initialize Google Places library
- Maps Compose library: `com.google.maps.android:maps-compose:8.1.0`

### Custom Dimension System
**Pattern**: Screen-aware scaling via extension properties
- `sdp` = Scaled DP (based on screen width)
- `ssp` = Scaled SP (based on screen width, for text)
- Defined in `ui/theme/Dimension.kt`
- **Always prefer `sdp`/`ssp` over hardcoded `dp`/`sp`**

### Localization
- Found in `ui/localization/`
- Implementation uses `ProvideAppLocalization` composable
- AppLanguage enum controls language state
- Centralizes string resources for multi-language support

## Testing & Debugging

### Emulator Tips
- Use `.\gradlew installDebug` to install apk on running emulator
- Debug via Android Studio's Logcat filtered on package `com.athar.accessibilitymapping`
- Screenshots: Emulator toolbar or `adb shell screencap`

### Build Cache
- First build after `clean`: ~60-90 seconds
- Incremental builds: ~30 seconds (due to Kotlin compilation)
- Cache stored in `~/.gradle-local-build/` (Windows-safe location)

## External Dependencies of Note

### Vision/ML
- **MediaPipe Tasks Vision** (`0.10.14`): Hand/pose landmark detection for accessibility scanning
- **CameraX**: Camera lifecycle management and frame capture

### Networking & Serialization
- **Ktor Client** (`2.3.13`): HTTP client with multipart form upload support
- **Kotlinx Serialization** (`1.7.3`): JSON serialization (kotlinx-serialization-json)

### Android Core
- **Jetpack Compose**: UI framework (1.10.3)
- **Material3**: Design system (1.4.0)
- **DataStore**: Preference persistence (1.2.0)
- **Navigation Compose** (`2.9.7`): Tab/screen navigation

## Git & Version Control Notes
- Project uses Git (`.git/` folder present)
- Multiple error log files (`build_err.txt`, `errors.txt`) suggest build troubleshooting in progress
- Local overrides in `.idea/` and `.vscode/` should not be committed (already in `.gitignore`)

---

**Last Updated:** March 2026 | For questions about build issues, check build_err.txt or run `./gradlew --status` to diagnose daemon problems.
