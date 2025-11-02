plugins {
    kotlin("jvm") version "1.9.20"
    `java-library`
}

group = "com.lvlup"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}