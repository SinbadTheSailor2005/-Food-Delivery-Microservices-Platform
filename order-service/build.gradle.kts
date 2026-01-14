plugins {
	java
	id("org.springframework.boot")
	id("io.spring.dependency-management")
}

version = "1.0.0"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

dependencies {
	implementation(project(":shared-libs"))

	implementation("org.springframework.boot:spring-boot-starter-web")

	implementation("org.springframework.kafka:spring-kafka")

	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test")

	// lombok
	implementation("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")

	// additional libs
	implementation("org.mapstruct:mapstruct:1.6.3")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.0")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.kafka:spring-kafka-test")

	// Testcontainers
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("org.testcontainers:kafka")

	// Awaitility для асинхронных тестов
	testImplementation("org.awaitility:awaitility:4.2.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
