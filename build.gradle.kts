plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "dev.seigwen"
version = "0.2.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

intellij {
    version.set("2023.3.8")
    type.set("IC")
    plugins.set(listOf())
}

tasks {
    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("243.*")
    }
    buildSearchableOptions {
        enabled = false
    }
    test {
        useJUnit()
    }
    runIde {
        jvmArgs("-Xmx1g", "-XX:ReservedCodeCacheSize=256m")
    }
    register<JavaExec>("conformancePayload") {
        group = "verification"
        description = "Write the SPEC 2.1 payload from the real Kotlin catalogue and message bundle"
        dependsOn("testClasses")
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("dev.seigwen.dualtran.ConformancePayload")
    }
}
