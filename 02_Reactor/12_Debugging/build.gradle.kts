plugins {
    java
    application
}

application {
    mainClass = "de.debugging.Main"
}

group = "de.debugging"
version = "1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.projectreactor:reactor-core:3.7.4")
    implementation("io.projectreactor:reactor-tools:3.7.4")
}
