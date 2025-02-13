plugins {
    java
}

// Always exclude checker-qual. This is the single most annoying thing that always reappears.
configurations.all {
    // We only use Jetbrains Annotations
    exclude("org.checkerframework", "checker-qual")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    withType<JavaCompile> {
        // We are fully aware, that we should be suppressing these instead of ignoring them here, but man keep my terminal clean.
        options.compilerArgs.addAll(listOf("-Xlint:none", "-Xlint:-deprecation", "-Xlint:-unchecked"))
    }
    withType<Test> {
        useJUnitPlatform()
        // Present until tests all succeed without
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        setForkEvery(1)
    }
}