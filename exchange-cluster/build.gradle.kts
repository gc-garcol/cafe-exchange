import com.google.protobuf.gradle.id

plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
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

var disruptorVersion = "4.0.0"
var annotationsApiVersion = "6.0.53"
var grpcVersion = "1.65.1"
var protocVersion = "4.27.2"
var dotenvVersion = "3.0.2"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.github.cdimascio:dotenv-java:${dotenvVersion}")

    runtimeOnly("io.grpc:grpc-netty-shaded:${grpcVersion}")
    implementation("io.grpc:grpc-services:${grpcVersion}")
    implementation("io.grpc:grpc-protobuf:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")
    compileOnly("org.apache.tomcat:annotations-api:${annotationsApiVersion}")
    implementation("com.google.protobuf:protobuf-java-util:${protocVersion}")
}


sourceSets {
    main {
        java.srcDirs(
            "src/main/java",
            "build/generated/source/proto/main/java",
            "build/generated/source/proto/main/grpc"
        )
    }
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:${protocVersion}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
            }
        }
    }
}
