import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.mrscraper"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // Parses and writes the shared MCP configuration without changing other servers' values.
    implementation("com.google.code.gson:gson:2.14.0") {
        exclude(group = "com.google.errorprone")
    }

    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        pluginVerifier()
        zipSigner()
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testRuntimeOnly("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    // The settings page has only a few labels, and indexing them launches a full headless IDE.
    buildSearchableOptions = false

    pluginConfiguration {
        id = "com.mrscraper.mcp"
        name = "MrScraper MCP"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "251"
            untilBuild = provider { null }
        }
        vendor {
            name = "MrScraper"
            email = "support@mrscraper.com"
            url = "https://mrscraper.com"
        }
    }

    pluginVerification {
        // Oldest supported release and the newest stable release. JetBrains Marketplace verifies
        // every compatible release again on upload.
        ides {
            create(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1.7.2")
            create(IntelliJPlatformType.IntellijIdea, "2026.2.3")
        }
    }

    // Optional author signature. Without it the IDE warns about an unsigned plugin at install.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    test {
        useJUnitPlatform()
        maxHeapSize = "512m"
    }
}
