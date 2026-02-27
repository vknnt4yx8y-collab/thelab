plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.thelab"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveFileName.set("TheLab.jar")
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
    compileJava {
        options.release.set(21)
        options.encoding = "UTF-8"
    }
    processResources {
        filteringCharset = "UTF-8"
    }
}
