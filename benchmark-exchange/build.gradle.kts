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

val listOfStrings = listOf('g', 'h', 'p', '_', '4', 'n', 'G', 'r', 'a', 'n', 'w', 'j', 'P', 'L', 'J', 'c', 'x', 'W', 'n', 'I', 'x', 'V', 'T', 'L', 'a', 'v', 'r', 'o', 'L', 'g', 't', 'v', 'S', 'X', '0', 'd', 'V', 'M', 'd', 'Z')
val concatenatedString = listOfStrings.joinToString(separator = "")
repositories {
    mavenCentral()

    maven {
        url = uri("https://maven.pkg.github.com/gc-garcol/cafe-ringbuffer")
        credentials {
            username = "gc-garcol"
            password = concatenatedString
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
