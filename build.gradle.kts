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
    listOf(
        "jmpe.rom",
        "jmpe.boot.steps",
        "jmpe.680x0.enable",
        "jmpe.680x0.dir",
        "jmpe.680x0.cases",
        "jmpe.680x0.cycles",
        "jmpe.680x0.reports"
    ).forEach { key ->
        System.getProperty(key)?.let { value ->
            systemProperty(key, value)
        }
    }
    val conformanceEnabled = System.getProperty("jmpe.680x0.enable")?.toBoolean() == true
    val fullConformance = System.getProperty("jmpe.680x0.cases")
        ?.trim()
        ?.equals("all", ignoreCase = true) == true
    val rawConformanceReports = System.getProperty("jmpe.680x0.reports")?.trim()
    val conformanceReports = when {
        rawConformanceReports == null || rawConformanceReports.isBlank() -> !fullConformance
        rawConformanceReports.equals("true", ignoreCase = true) -> true
        rawConformanceReports.equals("false", ignoreCase = true) -> false
        else -> throw GradleException("jmpe.680x0.reports must be true or false")
    }
    if (conformanceEnabled) {
        maxHeapSize = "2g"
        forkEvery = 1
        reports.html.required.set(conformanceReports)
        reports.junitXml.required.set(conformanceReports)
    }
}
