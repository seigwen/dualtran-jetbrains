plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "dev.seigwen"
version = "0.1.0"

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
}
