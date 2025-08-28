plugins {
	java
	war
	id("org.springframework.boot") version "3.5.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "capstone"
version = "0.0.1-SNAPSHOT"
description = "capstoneProject250823"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	runtimeOnly("com.h2database:h2")
	implementation("org.springframework.security:spring-security-crypto") // BCrypt
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	implementation("org.springframework.boot:spring-boot-starter-webflux")   // WebClient
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation ("org.apache.commons:commons-csv:1.11.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
