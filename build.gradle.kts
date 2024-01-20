plugins {
    id("java")
    id("io.freefair.lombok") version "8.4"
}

group = "org.richardcarter"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.dropwizard.metrics:metrics-core:4.2.0")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}