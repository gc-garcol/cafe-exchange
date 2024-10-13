import com.google.protobuf.gradle.id

plugins {
    java
    id("com.google.protobuf") version "0.9.4"
    idea // required for protobuf support in intellij
}

group = "gc.garcol"
version = "0.0.1-SNAPSHOT"

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

var jmhVersion = "1.37"
var disruptorVersion = "4.0.0"
var annotationsApiVersion = "6.0.53"
var grpcVersion = "1.64.0"
var protocVersion = "4.27.1"
var dotenvVersion = "3.0.2"

dependencies {
    implementation(project(":exchange-core"))
}
