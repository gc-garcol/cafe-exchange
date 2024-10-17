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

dependencies {
    implementation(project(":exchange-core"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.agrona:agrona:${agronaVersion}")

    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    implementation("redis.clients:jedis:${jedisVersion}")
}

tasks {
    task("run-cluster", JavaExec::class) {
        group = "run"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("gc.garcol.exchangecluster.ExchangeClusterApplication")

        // Get the port from command-line arguments or use a default value
        val port = project.properties["port"]?.toString() ?: "8080"
        args = listOf("--server.port=$port")
    }
}
