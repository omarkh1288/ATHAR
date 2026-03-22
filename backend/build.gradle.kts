plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.serialization")
  application
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
  }
}

application {
  mainClass.set("com.athar.backend.ServerKt")
}

dependencies {
  implementation("io.ktor:ktor-server-core-jvm:2.3.13")
  implementation("io.ktor:ktor-server-netty-jvm:2.3.13")
  implementation("io.ktor:ktor-server-auth-jvm:2.3.13")
  implementation("io.ktor:ktor-server-auth-jwt-jvm:2.3.13")
  implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.13")
  implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.13")
  implementation("io.ktor:ktor-server-call-logging-jvm:2.3.13")
  implementation("org.xerial:sqlite-jdbc:3.47.2.0")
  implementation("org.mindrot:jbcrypt:0.4")
  runtimeOnly("ch.qos.logback:logback-classic:1.5.8")

  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.2.20")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
}

tasks.test {
  useJUnitPlatform()
}
