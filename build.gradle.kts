plugins {
    id("java")
    id("application")
}

group = "com.jmpe"
version = "0.1"

application {
    mainClass.set("com.JMPE.Main")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    failOnNoDiscoveredTests = false
}
