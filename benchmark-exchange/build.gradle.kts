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

    maven {
        url = uri("https://maven.pkg.github.com/gc-garcol/cafe-ringbuffer")
        credentials {
            username = "gc-garcol"
            password = "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"
        }
    }
}

var jmhVersion = "1.37"
var disruptorVersion = "4.0.0"
var annotationsApiVersion = "6.0.53"
var grpcVersion = "1.64.0"
var protocVersion = "4.27.1"
var dotenvVersion = "3.0.2"

dependencies {
    implementation(project(":exchange-core"))

    // JMH
    implementation("org.openjdk.jmh:jmh-core:${jmhVersion}")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:${jmhVersion}")

    runtimeOnly("io.grpc:grpc-netty-shaded:${grpcVersion}")
    implementation("io.grpc:grpc-services:${grpcVersion}")
    implementation("io.grpc:grpc-protobuf:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")
    compileOnly("org.apache.tomcat:annotations-api:${annotationsApiVersion}")
    implementation("com.google.protobuf:protobuf-java-util:${protocVersion}")
}
