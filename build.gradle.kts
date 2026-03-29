plugins {
  id("com.android.application") version "8.12.0" apply false
  id("org.jetbrains.kotlin.android") version "2.2.20" apply false
  id("org.jetbrains.kotlin.jvm") version "2.2.20" apply false
  id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
  id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20" apply false
}

val kotlinVersion = "2.2.20"

// Keep build outputs outside OneDrive-managed project folders to avoid
// Windows access/cleanup failures on Gradle incremental directories.
val localBuildRoot = java.io.File(
  System.getProperty("user.home"),
  ".gradle-local-build/${rootProject.name}"
).absoluteFile

allprojects {
  val sanitizedPath = project.path.removePrefix(":").replace(':', '_').ifEmpty { "root" }
  layout.buildDirectory.set(localBuildRoot.resolve(sanitizedPath))

  configurations.configureEach {
    resolutionStrategy.force(
      "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
      "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion",
      "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion",
      "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"
    )
  }
}
