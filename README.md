# AccessibilityMappingApp

Android client and Kotlin/Ktor backend for Athar's accessibility mapping platform.

## Local setup

1. Install JDK 17 and point `JAVA_HOME` to it, or use Android Studio's embedded JDK.
2. Install the Android SDK.
3. Copy `local.properties.example` to `local.properties`.
4. Set `sdk.dir` in `local.properties`.
5. Fill `MAPS_API_KEY` and `BACKEND_BASE_URL` in `local.properties` when needed.

## Build

```powershell
.\gradlew :backend:test --console=plain
.\gradlew :app:assembleDebug --console=plain
```

The project redirects Gradle build output to `~/.gradle-local-build/AccessibilityMappingApp` to avoid OneDrive file-locking issues on Windows.
