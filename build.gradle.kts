plugins {
    id("java")
}

group = "com.jmpe"
version = "0.1"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    failOnNoDiscoveredTests = false
    listOf("jmpe.rom", "jmpe.boot.steps", "jmpe.680x0.enable", "jmpe.680x0.dir", "jmpe.680x0.cases", "jmpe.680x0.cycles").forEach { key ->
        System.getProperty(key)?.let { value ->
            systemProperty(key, value)
        }
    }
}
