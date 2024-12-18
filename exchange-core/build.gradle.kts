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

var agronaVersion = "1.23.1"
var annotationsApiVersion = "6.0.53"
var grpcVersion = "1.65.1"
var protocVersion = "4.27.2"
var dotenvVersion = "3.0.2"
var lombokVersion = "1.18.34"
var rockdbVersion = "9.6.1"
var jedisVersion = "5.2.0"

dependencies {
    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    implementation("io.github.cdimascio:dotenv-java:${dotenvVersion}")

    implementation("org.agrona:agrona:${agronaVersion}")
    implementation("org.rocksdb:rocksdbjni:${rockdbVersion}")

    runtimeOnly("io.grpc:grpc-netty-shaded:${grpcVersion}")
    implementation("io.grpc:grpc-services:${grpcVersion}")
    implementation("io.grpc:grpc-protobuf:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")
    compileOnly("org.apache.tomcat:annotations-api:${annotationsApiVersion}")
    implementation("com.google.protobuf:protobuf-java-util:${protocVersion}")

    implementation("org.rocksdb:rocksdbjni:${rockdbVersion}")
    implementation("redis.clients:jedis:${jedisVersion}")

    implementation("io.github.gc-garcol:cafe-ringbuffer:1.3.0")
    implementation("io.github.gc-garcol:cafe-wal:0.3.0")
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
