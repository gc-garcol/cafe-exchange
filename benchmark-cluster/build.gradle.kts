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

var agronaVersion = "1.23.1"
var lombokVersion = "1.18.34"
var jedisVersion = "5.2.0"
var annotationsApiVersion = "6.0.53"
var grpcVersion = "1.68.2"
var protocVersion = "4.29.0"
var grpcUtilVersion = "4.28.3"
var swaggerUIVersion = "2.6.0"
var prometheusVersion = "1.14.1"
var springActuatorVersion = "3.3.4"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${swaggerUIVersion}")

    implementation("org.agrona:agrona:${agronaVersion}")

    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    implementation("com.google.protobuf:protobuf-java-util:${grpcUtilVersion}")

    runtimeOnly("io.grpc:grpc-netty-shaded:${grpcVersion}")
    implementation("io.grpc:grpc-services:${grpcVersion}")
    implementation("io.grpc:grpc-protobuf:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")
    compileOnly("org.apache.tomcat:annotations-api:${annotationsApiVersion}")
    implementation("com.google.protobuf:protobuf-java-util:${protocVersion}")

    // observability
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus:${prometheusVersion}")
    implementation("io.opentelemetry:opentelemetry-sdk-common:1.44.1")
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

tasks {
    task("run-benchmark", JavaExec::class) {
        group = "run-benchmark"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("gc.garcol.benchmarkcluster.BenchmarkClusterApplication")

        // Get the port from command-line arguments or use a default value
        val port = project.properties["port"]?.toString() ?: "8090"
        args = listOf("--server.port=$port")
    }
}
