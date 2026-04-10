plugins {
    java
    application
}

application {
    mainClass = "de.bufferwindowgroup.Main"
}

group = "de.bufferwindowgroup"
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
