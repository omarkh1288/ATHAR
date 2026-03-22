@file:Suppress("CyclomaticComplexityInspection")

import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("org.jetbrains.kotlin.plugin.serialization")
}


android {
  namespace = "com.athar.accessibilitymapping"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.athar.accessibilitymapping"
    minSdk = 24
    //noinspection OldTargetApi
    targetSdk = 34
    versionCode = 1
    versionName = "1.0.0"

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
      localProperties.load(FileInputStream(localPropertiesFile))
    }
    val mapsApiKey = localProperties.getProperty("MAPS_API_KEY")
      ?: System.getenv("MAPS_API_KEY")
      ?: ""
    val backendBaseUrl = localProperties.getProperty("BACKEND_BASE_URL")
      ?: System.getenv("BACKEND_BASE_URL")
      ?: "https://chairman-ranger-laptops-senators.trycloudflare.com/"
    manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    isCoreLibraryDesugaringEnabled = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

  implementation("androidx.core:core-ktx:1.17.0")
  implementation("androidx.activity:activity-compose:1.12.4")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
  implementation("androidx.compose.ui:ui:1.10.3")
  implementation("androidx.compose.ui:ui-tooling-preview:1.10.3")
  implementation("androidx.compose.material3:material3:1.4.0")
  implementation("androidx.compose.material:material-icons-extended:1.7.8")
  implementation("com.composables:icons-lucide-cmp:2.2.1")
  implementation("androidx.navigation:navigation-compose:2.9.7")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  implementation("com.google.android.material:material:1.13.0")
  implementation("com.google.android.gms:play-services-maps:20.0.0")
  implementation("com.google.android.gms:play-services-location:21.3.0")
  implementation("com.google.maps.android:maps-compose:8.1.0")
  implementation("com.google.android.libraries.places:places:3.5.0")
  implementation("androidx.datastore:datastore-preferences:1.2.0")
  implementation("io.coil-kt:coil-compose:2.7.0")
  implementation("io.ktor:ktor-client-core:2.3.13")
  implementation("io.ktor:ktor-client-okhttp:2.3.13")
  implementation("io.ktor:ktor-client-content-negotiation:2.3.13")
  implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.13")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

  // CameraX for camera functionality
  implementation("androidx.camera:camera-camera2:1.3.4")
  implementation("androidx.camera:camera-lifecycle:1.3.4")
  implementation("androidx.camera:camera-view:1.3.4")

  // MediaPipe for hand landmark detection
  implementation("com.google.mediapipe:tasks-vision:0.10.14")

  debugImplementation("androidx.compose.ui:ui-tooling:1.10.3")
  debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.3")
}

val syncDebugApkMetadataForIde by tasks.registering(Copy::class) {
  dependsOn("packageDebug")
  from(layout.buildDirectory.dir("outputs/apk/debug")) {
    include("*.apk")
    include("output-metadata.json")
  }
  into(layout.buildDirectory.dir("intermediates/apk/debug"))
}

tasks.matching { it.name == "createDebugApkListingFileRedirect" }.configureEach {
  dependsOn(syncDebugApkMetadataForIde)
}

fun removeWindowsDirectory(target: java.io.File) {
  if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return
  if (!target.exists()) return

  providers.exec {
    isIgnoreExitValue = true
    commandLine("cmd", "/c", "attrib -R \"${target.absolutePath}\" /S /D")
  }.result.get()
  providers.exec {
    isIgnoreExitValue = true
    commandLine("cmd", "/c", "rmdir /s /q \"${target.absolutePath}\"")
  }.result.get()
}

fun clearWindowsReadOnlyAttributes(target: java.io.File) {
  if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return
  if (!target.exists()) return

  providers.exec {
    isIgnoreExitValue = true
    commandLine("cmd", "/c", "attrib -R \"${target.absolutePath}\" /S /D")
  }.result.get()
}

fun buildConfigVariantDirectory(taskName: String): java.io.File? {
  if (!taskName.startsWith("generate") || !taskName.endsWith("BuildConfig")) return null
  val variantName = taskName
    .removePrefix("generate")
    .removeSuffix("BuildConfig")
    .replaceFirstChar { char -> char.lowercase() }
    .takeIf { it.isNotBlank() }
    ?: return null
  return layout.buildDirectory.dir("generated/source/buildConfig/$variantName").get().asFile
}

fun artProfileTaskDirectory(taskName: String): java.io.File? {
  if (!taskName.startsWith("compile") || !taskName.endsWith("ArtProfile")) return null
  val variantName = taskName
    .removePrefix("compile")
    .removeSuffix("ArtProfile")
    .replaceFirstChar { char -> char.lowercase() }
    .takeIf { it.isNotBlank() }
    ?: return null
  return layout.buildDirectory.dir("intermediates/dex_metadata_directory/$variantName/$taskName").get().asFile
}

fun packageTaskTmpDirectory(taskName: String): java.io.File? {
  if (!taskName.startsWith("package") || taskName.contains("Resources")) return null
  return layout.buildDirectory.dir("intermediates/incremental/$taskName/tmp").get().asFile
}

val prepareWindowsBuildDirectory by tasks.registering {
  doLast {
    val buildDir = layout.buildDirectory.get().asFile
    clearWindowsReadOnlyAttributes(buildDir)

    listOf(
      buildDir.resolve("intermediates/incremental/packageDebug/tmp"),
      buildDir.resolve("intermediates/incremental/packageRelease/tmp"),
      buildDir.resolve("outputs/apk")
    ).forEach(::removeWindowsDirectory)
  }
}

tasks.matching { it.name == "preBuild" }.configureEach {
  dependsOn(prepareWindowsBuildDirectory)
}

val prepareDexBuilderWorkspace by tasks.registering {
  doLast {
    val debugTargets = listOf(
      layout.buildDirectory.dir("intermediates/desugar_graph/debug/dexBuilderDebug/out").get().asFile,
      layout.buildDirectory.dir("intermediates/project_dex_archive/debug/dexBuilderDebug/out").get().asFile
    )

    debugTargets.forEach(::removeWindowsDirectory)
  }
}

tasks.matching { it.name == "dexBuilderDebug" }.configureEach {
  dependsOn(prepareDexBuilderWorkspace)
}

tasks.matching { it.name.startsWith("generate") && it.name.endsWith("BuildConfig") }.configureEach {
  doFirst {
    buildConfigVariantDirectory(name)?.let(::removeWindowsDirectory)
  }
}

tasks.matching { it.name.startsWith("compile") && it.name.endsWith("ArtProfile") }.configureEach {
  doFirst {
    artProfileTaskDirectory(name)?.let(::removeWindowsDirectory)
  }
}

tasks.matching { it.name.startsWith("package") && !it.name.contains("Resources") }.configureEach {
  doFirst {
    packageTaskTmpDirectory(name)?.let(::removeWindowsDirectory)
  }
}

tasks.matching { it.name == "clean" }.configureEach {
  doFirst {
    val javacTmpRoot = layout.buildDirectory.dir("tmp").get().asFile
    javacTmpRoot.listFiles()
      ?.filter { it.isDirectory && it.name.startsWith("compile") && it.name.endsWith("JavaWithJavac") }
      ?.forEach(::removeWindowsDirectory)
  }
}
