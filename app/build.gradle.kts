plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.lsp4j)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "logo.MainKt"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("logo-lsp")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes("Main-Class" to "logo.MainKt")
    }
}
