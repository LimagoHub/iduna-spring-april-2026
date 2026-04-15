plugins {
    java
    application
}

application {
    mainClass = "de.flatmap.Main"
}

group = "de.flatmap"
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
}
