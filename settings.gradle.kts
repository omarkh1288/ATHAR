pluginManagement {
  repositories {
    google()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "AccessibilityMappingApp"

val localBuildRoot = file("${System.getProperty("user.home")}/.gradle-local-build/${rootProject.name}")

gradle.beforeProject {
  val relativeProjectPath = path
    .removePrefix(":")
    .replace(":", "/")
    .ifBlank { "root" }
  layout.buildDirectory.set(localBuildRoot.resolve(relativeProjectPath))
}

include(":app")
include(":backend")
