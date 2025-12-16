plugins {
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("jvm")
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
}

group = "dev.eventk.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions  {
        freeCompilerArgs.add("-Xjsr305=strict")
        freeCompilerArgs.add("-Xjvm-default=all")
    }
    jvmToolchain(17)
}

dependencies {
    // spring
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.flywaydb:flyway-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework:spring-jdbc")

    // project
    implementation(project(":domain"))
    implementation("dev.eventk:event-store-impl-postgresql:local-SNAPSHOT")
    implementation("dev.eventk:hex-arch-adapters-spring6:local-SNAPSHOT")

    // runtime
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // test
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading") // https://github.com/mockito/mockito/issues/3037
}
