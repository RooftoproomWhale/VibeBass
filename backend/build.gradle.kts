plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot") version "3.2.2"
}

group = "com.woong.vibebass"
version = "0.0.1-SNAPSHOT"

// Java 및 Kotlin 컴파일러 버전을 17로 일관성 있게 정렬
kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot 공식 BOM 플랫폼을 활용한 버전 관리로 플러그인 호환성 오류 원천 해결
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.2"))

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin") // Kotlin 객체 JSON 직렬화 지원
    
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux") // WebClient용
    
    runtimeOnly("org.postgresql:postgresql")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1") // 코틀린 스타일 모킹 지원
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher") // Gradle 9.x 테스트 기동 호환용 추가
}

// KotlinCompile 태스크는 jvm 호환 17로 기본 매핑되므로 별도 옵션 제거

tasks.withType<Test> {
    useJUnitPlatform()
}
